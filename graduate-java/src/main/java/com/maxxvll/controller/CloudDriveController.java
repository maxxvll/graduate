package com.maxxvll.controller;

import com.maxxvll.common.BaseController;
import com.maxxvll.common.Result;
import com.maxxvll.common.dto.CloudShareCreateDTO;
import com.maxxvll.common.dto.CloudFileSaveDTO;
import com.maxxvll.common.vo.CloudDownloadVO;
import com.maxxvll.common.vo.CloudFilePageVO;
import com.maxxvll.common.vo.CloudPreviewContentVO;
import com.maxxvll.common.vo.CloudShareVO;
import com.maxxvll.common.vo.GroupFileVO;
import com.maxxvll.mapper.CloudStorageUsageMapper;
import com.maxxvll.service.CloudDriveService;
import com.maxxvll.utils.CloudFileAccessSigner;
import com.maxxvll.utils.MinioUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 云盘控制器
 * 提供文件上传、下载、分享等功能的REST接口
 */
@Slf4j
@RestController
@RequestMapping("/cloud")
@Tag(name = "云盘", description = "云盘文件管理相关接口")
public class CloudDriveController extends BaseController {

    private static final long DEFAULT_QUOTA_BYTES = 10L * 1024 * 1024 * 1024; // 10GB

    @Resource
    private CloudDriveService cloudDriveService;

    @Resource
    private CloudFileAccessSigner cloudFileAccessSigner;

    @Resource
    private CloudStorageUsageMapper cloudStorageUsageMapper;

    @Resource
    private MinioUtil minioUtil;

    // ==================== 文件管理 API ====================

    /**
     * 获取文件列表
     */
    @GetMapping("/list")
    public Result<CloudFilePageVO> list(
            @RequestParam(defaultValue = "100") @Positive int limit,
            @RequestParam(required = false) String cursor,
            @RequestParam(required = false) String folderId) throws Exception {
        limit = normalizeLimit(limit);
        return Result.success(cloudDriveService.listFiles(getCurrentUserId(), limit, cursor, folderId));
    }

    /**
     * 上传文件
     */
    @SneakyThrows
    @PostMapping("/upload")
    public Result<Void> upload(
            @RequestParam("file") @NotNull MultipartFile file,
            @RequestParam(required = false) String folderId) {
        cloudDriveService.upload(file, getCurrentUserId(), folderId);
        return success();
    }

    /**
     * 下载文件
     */
    @SneakyThrows
    @GetMapping("/download")
    public ResponseEntity<InputStreamResource> download(@RequestParam @NotBlank String object) {
        CloudDownloadVO cloudFile = cloudDriveService.downloadFile(object);
        return buildFileResponse(cloudFile, false);
    }

    /**
     * 预览文件内容
     */
    @SneakyThrows
    @GetMapping("/preview-content")
    public Result<CloudPreviewContentVO> previewContent(@RequestParam @NotBlank String object) {
        return Result.success(cloudDriveService.getPreviewContent(object));
    }

    /**
     * 通过访问令牌下载文件
     */
    @SneakyThrows
    @GetMapping("/access/download")
    public ResponseEntity<InputStreamResource> downloadByAccess(
            @RequestParam @NotBlank String object,
            @RequestParam("uid") @NotBlank String userId,
            @RequestParam("exp") @NotNull Long expiresAt,
            @RequestParam("sig") @NotBlank String signature) {
        if (!isAccessValid("download", object, userId, expiresAt, signature)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        CloudDownloadVO cloudFile = cloudDriveService.downloadFile(object, userId, null, null);
        return buildFileResponse(cloudFile, false);
    }

    /**
     * 通过访问令牌内联预览文件
     */
    @SneakyThrows
    @GetMapping("/access/inline")
    public ResponseEntity<InputStreamResource> inlinePreview(
            @RequestParam @NotBlank String object,
            @RequestParam("uid") @NotBlank String userId,
            @RequestParam("exp") @NotNull Long expiresAt,
            @RequestParam("sig") @NotBlank String signature) {
        if (!isAccessValid("inline", object, userId, expiresAt, signature)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        CloudDownloadVO cloudFile = cloudDriveService.downloadFile(object, userId, null, null);
        return buildFileResponse(cloudFile, true);
    }

    /**
     * 通过访问令牌流式传输文件（支持断点续传）
     */
    @SneakyThrows
    @GetMapping("/access/stream")
    public ResponseEntity<InputStreamResource> streamFile(
            @RequestParam @NotBlank String object,
            @RequestParam("uid") @NotBlank String userId,
            @RequestParam("exp") @NotNull Long expiresAt,
            @RequestParam("sig") @NotBlank String signature,
            @RequestHeader(value = HttpHeaders.RANGE, required = false) String rangeHeader) {
        if (!isAccessValid("stream", object, userId, expiresAt, signature)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        CloudDownloadVO metadata = cloudDriveService.downloadFile(object, userId, 0L, 1L);
        try {
            ByteRange range = parseRange(rangeHeader, metadata.getSize());
            if (range == null) {
                return ResponseEntity.status(HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE)
                        .header(HttpHeaders.CONTENT_RANGE, "bytes */" + metadata.getSize())
                        .build();
            }

            CloudDownloadVO payload = range.partial()
                    ? cloudDriveService.downloadFile(object, userId, range.start(), range.length())
                    : cloudDriveService.downloadFile(object, userId, null, null);

            ResponseEntity.BodyBuilder builder = ResponseEntity.status(range.partial() ? HttpStatus.PARTIAL_CONTENT : HttpStatus.OK)
                    .contentType(resolveMediaType(metadata.getContentType()))
                    .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                    .header(HttpHeaders.CONTENT_LENGTH, String.valueOf(range.length()))
                    .header(HttpHeaders.CACHE_CONTROL, "private, max-age=300");

            if (range.partial()) {
                builder.header(HttpHeaders.CONTENT_RANGE,
                        "bytes " + range.start() + "-" + range.end() + "/" + metadata.getSize());
            }

            return builder.body(new InputStreamResource(payload.getInputStream()));
        } finally {
            IOUtils.closeQuietly(metadata.getInputStream());
        }
    }

    /**
     * 删除文件
     */
    @SneakyThrows
    @PostMapping("/delete")
    public Result<Void> delete(@RequestParam @NotBlank String object) {
        cloudDriveService.delete(object, getCurrentUserId());
        return success();
    }

    /**
     * 从URL导入文件
     */
    @SneakyThrows
    @PostMapping("/import")
    public Result<Void> importUrl(@Valid @RequestBody Map<String, String> body) {
        String fileUrl = body.get("fileUrl");
        if (fileUrl == null || fileUrl.trim().isEmpty()) {
            return fail("缺少 fileUrl 参数");
        }
        cloudDriveService.importByUrl(fileUrl, getCurrentUserId());
        return success();
    }

    // ==================== 文件夹管理 API ====================

    /**
     * 创建文件夹
     */
    @SneakyThrows
    @PostMapping("/folder/create")
    public Result<Void> createFolder(
            @RequestParam @NotBlank String folderName,
            @RequestParam(required = false) String parentId) {
        cloudDriveService.createFolder(folderName, parentId, getCurrentUserId());
        return success();
    }

    /**
     * 重命名文件/文件夹
     */
    @SneakyThrows
    @PostMapping("/rename")
    public Result<Void> rename(
            @RequestParam @NotBlank String object,
            @RequestParam @NotBlank String newName) {
        cloudDriveService.rename(object, newName, getCurrentUserId());
        return success("重命名成功");
    }

    /**
     * 移动文件到文件夹
     */
    @SneakyThrows
    @PostMapping("/move")
    public Result<Void> moveToFolder(
            @RequestParam @NotBlank String object,
            @RequestParam(required = false) String targetFolderId) {
        cloudDriveService.moveToFolder(object, targetFolderId, getCurrentUserId());
        return success("移动成功");
    }

    // ==================== 分享管理 API ====================

    /**
     * 创建分享链接
     */
    @SneakyThrows
    @PostMapping("/share/create")
    public Result<CloudShareVO> createShare(@Valid @RequestBody CloudShareCreateDTO dto) {
        Long userId = getCurrentUserIdAsLong();
        return Result.success(cloudDriveService.createShare(dto.getFileId(), dto.getPassword(), dto.getExpireDays(), userId));
    }

    /**
     * 获取用户的分享列表
     */
    @SneakyThrows
    @GetMapping("/share/list")
    public Result<List<CloudShareVO>> listShares() {
        Long userId = getCurrentUserIdAsLong();
        return Result.success(cloudDriveService.listShares(userId));
    }

    /**
     * 通过分享码获取分享信息
     */
    @SneakyThrows
    @GetMapping("/share/download")
    public Result<CloudShareVO> getShare(
            @RequestParam @NotBlank String code,
            @RequestParam(required = false) String password) {
        return Result.success(cloudDriveService.getShareByCode(code, password));
    }

    /**
     * 取消分享
     */
    @PostMapping("/share/cancel")
    public Result<Void> cancelShare(@RequestParam @NotNull Long shareId) {
        Long userId = getCurrentUserIdAsLong();
        cloudDriveService.cancelShare(shareId, userId);
        return success();
    }

    // ==================== 群组文件 API ====================

    /**
     * 获取群组文件列表
     */
    @SneakyThrows
    @GetMapping("/group/file/list")
    public Result<List<GroupFileVO>> listGroupFiles(@RequestParam @NotBlank String groupId) {
        Long userId = getCurrentUserIdAsLong();
        return Result.success(cloudDriveService.listGroupFiles(groupId, userId));
    }

    /**
     * 从聊天保存文件到云盘
     */
    @SneakyThrows
    @PostMapping("/file/saveFromChat")
    public Result<GroupFileVO> saveFileFromChat(@Valid @RequestBody CloudFileSaveDTO dto) {
        Long userId = getCurrentUserIdAsLong();
        return Result.success(cloudDriveService.saveFileFromChat(
                dto.getMessageId().toString(), "", 0L, "", dto.getSessionId(), userId));
    }

    /**
     * 删除群组文件
     */
    @SneakyThrows
    @PostMapping("/group/file/delete")
    public Result<Void> deleteGroupFile(@RequestParam @NotNull Long fileId) {
        Long userId = getCurrentUserIdAsLong();
        cloudDriveService.deleteGroupFile(fileId, userId);
        log.info("用户[{}]删除群组文件[{}]", userId, fileId);
        return success();
    }

    // ==================== 存储使用量 API ====================

    /**
     * 获取存储使用情况
     */
    @GetMapping("/storage/usage")
    public Result<Map<String, Object>> getStorageUsage() {
        String userId = getCurrentUserId();
        Long usedBytes = cloudStorageUsageMapper.selectUsedBytesByUserId(userId);
        Long quotaBytes = cloudStorageUsageMapper.selectQuotaBytesByUserId(userId);

        if (quotaBytes == null || quotaBytes <= 0) {
            quotaBytes = DEFAULT_QUOTA_BYTES;
        }
        if (usedBytes == null) {
            usedBytes = 0L;
        }

        Double usagePercent = (double) usedBytes / quotaBytes * 100;
        Long remainingBytes = Math.max(0, quotaBytes - usedBytes);

        Map<String, Object> usage = new HashMap<>();
        usage.put("usedBytes", usedBytes);
        usage.put("quotaBytes", quotaBytes);
        usage.put("remainingBytes", remainingBytes);
        usage.put("usagePercent", usagePercent);

        return success(usage);
    }

    /**
     * 刷新存储使用量
     */
    @SneakyThrows
    @PostMapping("/storage/refresh")
    public Result<Void> refreshStorageUsage() {
        String userId = getCurrentUserId();
        long calculated = minioUtil.calculateCloudUsedBytes(userId);
        cloudStorageUsageMapper.upsertUsedBytes(userId, calculated);
        log.info("用户[{}]刷新存储使用量: {} bytes", userId, calculated);
        return success();
    }

    // ==================== 私有辅助方法 ====================

    private ResponseEntity<InputStreamResource> buildFileResponse(CloudDownloadVO cloudFile, boolean inline) {
        ContentDisposition disposition = (inline ? ContentDisposition.inline() : ContentDisposition.attachment())
                .filename(cloudFile.getFileName(), StandardCharsets.UTF_8)
                .build();
        return ResponseEntity.ok()
                .contentType(resolveMediaType(cloudFile.getContentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .header(HttpHeaders.CONTENT_LENGTH, String.valueOf(cloudFile.getSize()))
                .header(HttpHeaders.CACHE_CONTROL, "private, max-age=300")
                .body(new InputStreamResource(cloudFile.getInputStream()));
    }

    private MediaType resolveMediaType(String contentType) {
        if (contentType == null || contentType.isBlank()) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
        try {
            return MediaType.parseMediaType(contentType);
        } catch (Exception e) {
            log.warn("Failed to parse media type: {}, using default", contentType, e);
            return MediaType.APPLICATION_OCTET_STREAM;
        }
    }

    private boolean isAccessValid(String accessMode, String object, String userId, Long expiresAt, String signature) {
        return cloudFileAccessSigner.isValid(accessMode, object, userId, expiresAt, signature);
    }

    private ByteRange parseRange(String rangeHeader, long totalSize) {
        if (rangeHeader == null || rangeHeader.isBlank()) {
            return new ByteRange(0, totalSize - 1, totalSize, false);
        }
        if (!rangeHeader.startsWith("bytes=") || rangeHeader.contains(",")) {
            return null;
        }

        String[] parts = rangeHeader.substring("bytes=".length()).split("-", 2);
        if (parts.length != 2) {
            return null;
        }

        try {
            if (parts[0].isBlank()) {
                long suffixLength = Long.parseLong(parts[1]);
                if (suffixLength <= 0) {
                    return null;
                }
                long start = Math.max(0, totalSize - suffixLength);
                return new ByteRange(start, totalSize - 1, totalSize - start, true);
            }

            long start = Long.parseLong(parts[0]);
            long end = parts[1].isBlank() ? totalSize - 1 : Long.parseLong(parts[1]);
            if (start < 0 || start >= totalSize || end < start) {
                return null;
            }
            long boundedEnd = Math.min(end, totalSize - 1);
            return new ByteRange(start, boundedEnd, boundedEnd - start + 1, true);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private record ByteRange(long start, long end, long length, boolean partial) {
    }
}
