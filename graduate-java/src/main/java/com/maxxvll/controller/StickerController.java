package com.maxxvll.controller;

import cn.hutool.core.util.IdUtil;
import com.maxxvll.common.BaseController;
import com.maxxvll.common.Result;
import com.maxxvll.domain.ChatSticker;
import com.maxxvll.service.ChatStickerService;
import com.maxxvll.utils.MinioUtil;
import com.maxxvll.utils.UserContextUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 表情包控制器
 */
@Slf4j
@RestController
@RequestMapping("/sticker")
@Tag(name = "表情包", description = "表情包管理相关接口")
public class StickerController extends BaseController {

    private static final String STICKER_BUCKET = "chat-stickers";

    @Resource
    private ChatStickerService chatStickerService;

    @Resource
    private MinioUtil minioUtil;

    /**
     * 获取用户收藏的表情列表
     */
    @GetMapping("/list")
    public Result<List<ChatSticker>> getStickerList() {
        Long userId = Long.valueOf(UserContextUtil.getCurrentUserId());
        List<ChatSticker> stickers = chatStickerService.getUserStickers(userId);
        return Result.success("获取表情列表成功", stickers);
    }

    /**
     * 添加收藏表情（通过文件上传）
     */
    @PostMapping("/upload")
    public Result<ChatSticker> uploadSticker(@RequestParam MultipartFile file,
                                               @RequestParam(required = false) String name,
                                               @RequestParam(required = false, defaultValue = "custom") String category) {
        Long userId = Long.valueOf(UserContextUtil.getCurrentUserId());

        if (file.isEmpty()) {
            return Result.fail("上传文件不能为空");
        }

        // 校验文件类型（只允许图片）
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !isImageFile(originalFilename)) {
            return Result.fail("只支持上传图片格式（jpg、png、gif、webp）");
        }

        // 校验文件大小（限制5MB）
        if (file.getSize() > 5 * 1024 * 1024) {
            return Result.fail("表情图片大小不能超过5MB");
        }

        try {
            // 上传到 MinIO
            String url = uploadStickerToMinio(file, userId);
            ChatSticker sticker = chatStickerService.addSticker(userId, url, name, category);
            return Result.success("表情添加成功", sticker);
        } catch (Exception e) {
            log.error("表情上传失败", e);
            return Result.fail("表情上传失败：" + e.getMessage());
        }
    }

    /**
     * 上传表情图片到 MinIO
     */
    private String uploadStickerToMinio(MultipartFile file, Long userId) throws Exception {
        String originalFilename = file.getOriginalFilename();
        String suffix = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            suffix = originalFilename.substring(originalFilename.lastIndexOf(".")).toLowerCase();
        }
        // 使用用户ID和UUID构建对象名，避免文件名冲突
        String objectName = String.format("sticker/user/%d/%s%s", userId, IdUtil.simpleUUID(), suffix);

        minioUtil.uploadFile(file, objectName);
        log.info("表情图片上传成功: userId={}, objectName={}", userId, objectName);

        // 返回相对路径，前端可通过 getStickerUrl 获取完整 URL
        return objectName;
    }

    /**
     * 添加收藏表情（通过URL）
     */
    @PostMapping("/add")
    public Result<ChatSticker> addSticker(@RequestParam String url,
                                           @RequestParam(required = false) String name,
                                           @RequestParam(required = false, defaultValue = "custom") String category) {
        Long userId = Long.valueOf(UserContextUtil.getCurrentUserId());
        ChatSticker sticker = chatStickerService.addSticker(userId, url, name, category);
        return Result.success("表情添加成功", sticker);
    }

    /**
     * 删除收藏的表情
     */
    @DeleteMapping("/{stickerId}")
    public Result<Void> deleteSticker(@PathVariable Long stickerId) {
        Long userId = Long.valueOf(UserContextUtil.getCurrentUserId());
        chatStickerService.deleteSticker(stickerId, userId);
        return Result.success("删除成功");
    }

    /**
     * 重命名表情
     */
    @PutMapping("/{stickerId}/rename")
    public Result<Void> renameSticker(@PathVariable Long stickerId,
                                       @RequestParam String name) {
        Long userId = Long.valueOf(UserContextUtil.getCurrentUserId());
        chatStickerService.renameSticker(stickerId, userId, name);
        return Result.success("重命名成功");
    }

    private boolean isImageFile(String filename) {
        if (filename == null) return false;
        String lower = filename.toLowerCase();
        return lower.endsWith(".jpg") || lower.endsWith(".jpeg") ||
               lower.endsWith(".png") || lower.endsWith(".gif") ||
               lower.endsWith(".webp");
    }
}
