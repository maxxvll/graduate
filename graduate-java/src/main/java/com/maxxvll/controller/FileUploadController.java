package com.maxxvll.controller;

import com.maxxvll.common.BaseController;
import com.maxxvll.common.Result;
import com.maxxvll.common.annotation.RateLimit;
import com.maxxvll.common.dto.FileUploadChunkDTO;
import com.maxxvll.common.exception.BusinessException;
import com.maxxvll.common.vo.FileUploadCheckVO;
import com.maxxvll.utils.FileSecurityUtil;
import com.maxxvll.utils.MinioUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/chat/file")
@Tag(name = "文件上传", description = "聊天文件上传相关接口")
public class FileUploadController extends BaseController {

    @Resource
    private MinioUtil minioUtil;

    @Resource
    private FileSecurityUtil fileSecurityUtil;

    @PostMapping("/check")
    @RateLimit(limit = 60, period = 60, limitType = RateLimit.LimitType.USER, message = "请求过于频繁，请稍后再试")
    public Result<FileUploadCheckVO> checkFile(@RequestParam String md5, @RequestParam String fileName) {
        // 文件名校验
        if (fileName == null || fileName.isBlank()) {
            throw new BusinessException("文件名不能为空");
        }

        // 校验文件扩展名
        fileSecurityUtil.validateExtension(fileName);

        String existUrl = minioUtil.checkFileExistByMd5(md5);
        if (existUrl != null) {
            log.info("秒传成功: md5={}", md5);
            return Result.success(FileUploadCheckVO.builder()
                    .shouldUpload(false)
                    .fileUrl(existUrl)
                    .build());
        }

        List<Integer> uploadedChunks = minioUtil.listUploadedChunks(md5);
        log.info("断点续传检查: md5={}, 已上传切片数={}", md5, uploadedChunks.size());

        return Result.success(FileUploadCheckVO.builder()
                .shouldUpload(true)
                .uploadedChunks(uploadedChunks)
                .build());
    }

    @PostMapping("/upload-chunk")
    @RateLimit(limit = 120, period = 60, limitType = RateLimit.LimitType.USER, message = "上传过于频繁，请稍后再试")
    public Result<Boolean> uploadChunk(@Valid @ModelAttribute FileUploadChunkDTO dto) {
        // 校验文件大小
        if (dto.getFile() != null) {
            fileSecurityUtil.validateFileSize(dto.getFile().getSize(), FileSecurityUtil.MAX_CHUNK_SIZE);
        }

        // 校验总切片数（防止恶意构造）
        if (dto.getTotalChunks() == null || dto.getTotalChunks() <= 0 || dto.getTotalChunks() > 1000) {
            throw new BusinessException("非法的切片数量");
        }

        try {
            minioUtil.uploadChunk(dto.getMd5(), dto.getChunkIndex(), dto.getFile());
            return Result.success(true);
        } catch (Exception e) {
            log.error("切片上传失败", e);
            return Result.fail("切片上传失败");
        }
    }

    /**
     * 合并切片 (最终版)
     * 1. 合并文件
     * 2. 生成签名URL
     * 3. 自动清理切片
     */
    @PostMapping("/merge")
    @RateLimit(limit = 60, period = 60, limitType = RateLimit.LimitType.USER, message = "合并操作过于频繁，请稍后再试")
    public Result<String> mergeChunks(@RequestParam String md5,
                                      @RequestParam String fileName,
                                      @RequestParam Integer totalChunks,
                                      @RequestParam(defaultValue = "false") Boolean isPublic) {
        // 文件名校验
        if (fileName == null || fileName.isBlank()) {
            throw new BusinessException("文件名不能为空");
        }

        // 校验文件扩展名
        fileSecurityUtil.validateExtension(fileName);

        // 校验总切片数（防止恶意构造）
        if (totalChunks == null || totalChunks <= 0 || totalChunks > 1000) {
            throw new BusinessException("非法的切片数量");
        }

        try {
            log.info(">>> 开始处理合并请求 <<<");
            String finalPath = minioUtil.mergeChunks(md5, fileName, totalChunks);
            String url = minioUtil.getChatFileUrl(finalPath, isPublic);
            log.info("合并全部完成: Path={}", finalPath);
            // 3. 返回 URL 给前端，前端直接存入数据库
            return Result.success("合并全部完成",url);

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("合并流程失败", e);
            return Result.fail("合并失败: " + e.getMessage());
        }
    }
}
