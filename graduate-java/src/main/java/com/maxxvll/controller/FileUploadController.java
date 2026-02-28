package com.maxxvll.controller;

import com.maxxvll.common.Result;
import com.maxxvll.common.dto.FileUploadChunkDTO;
import com.maxxvll.common.vo.FileUploadCheckVO;
import com.maxxvll.utils.MinioUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/chat/file")
public class FileUploadController {

    @Resource
    private MinioUtil minioUtil;

    @PostMapping("/check")
    public Result<FileUploadCheckVO> checkFile(@RequestParam String md5, @RequestParam String fileName) {
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
    public Result<Boolean> uploadChunk(@ModelAttribute FileUploadChunkDTO dto) {
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
    public Result<String> mergeChunks(@RequestParam String md5,
                                      @RequestParam String fileName,
                                      @RequestParam Integer totalChunks,
                                      @RequestParam(defaultValue = "false") Boolean isPublic) {
        try {
            log.info(">>> 开始处理合并请求 <<<");
            String finalPath = minioUtil.mergeChunks(md5, fileName, totalChunks);
            String url = minioUtil.getChatFileUrl(finalPath, isPublic);
            log.info("合并全部完成: Path={}", finalPath);
            // 3. 返回 URL 给前端，前端直接存入数据库
            return Result.success("合并全部完成",url);

        } catch (Exception e) {
            log.error("合并流程失败", e);
            return Result.fail("合并失败: " + e.getMessage());
        }
    }
}