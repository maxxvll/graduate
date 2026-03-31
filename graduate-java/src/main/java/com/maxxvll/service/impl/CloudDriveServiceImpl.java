package com.maxxvll.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.maxxvll.common.exception.BusinessException;
import com.maxxvll.common.vo.CloudDownloadVO;
import com.maxxvll.common.vo.CloudFilePageVO;
import com.maxxvll.common.vo.CloudFileVO;
import com.maxxvll.common.vo.CloudPreviewContentVO;
import com.maxxvll.common.vo.CloudShareVO;
import com.maxxvll.common.vo.GroupFileVO;
import com.maxxvll.domain.ChatGroupMember;
import com.maxxvll.domain.ChatUser;
import com.maxxvll.domain.CloudShare;
import com.maxxvll.domain.CloudStorageUsage;
import com.maxxvll.domain.GroupFile;
import com.maxxvll.mapper.ChatGroupMemberMapper;
import com.maxxvll.mapper.ChatUserMapper;
import com.maxxvll.mapper.CloudShareMapper;
import com.maxxvll.mapper.CloudStorageUsageMapper;
import com.maxxvll.mapper.GroupFileMapper;
import com.maxxvll.service.ChatGroupMemberService;
import com.maxxvll.service.CloudDriveService;
import com.maxxvll.utils.CloudDocumentPreviewRenderer;
import com.maxxvll.utils.CloudFileAccessSigner;
import com.maxxvll.utils.CloudFileSupport;
import com.maxxvll.utils.MinioUtil;
import com.maxxvll.utils.UserContextUtil;
import io.minio.StatObjectResponse;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Slf4j
@Service
public class CloudDriveServiceImpl implements CloudDriveService {

    private static final int DEFAULT_LIST_LIMIT = 100;
    private static final int MAX_LIST_LIMIT = 200;
    private static final long MAX_PREVIEW_SOURCE_BYTES = 12L * 1024 * 1024;
    private static final long DEFAULT_QUOTA_BYTES = 10L * 1024 * 1024 * 1024; // 10GB

    @Resource
    private MinioUtil minioUtil;

    @Resource
    private CloudStorageUsageMapper cloudStorageUsageMapper;

    @Resource
    private CloudFileAccessSigner cloudFileAccessSigner;

    @Resource
    private CloudShareMapper cloudShareMapper;

    @Resource
    private GroupFileMapper groupFileMapper;

    @Resource
    private ChatUserMapper chatUserMapper;

    @Resource
    private PasswordEncoder passwordEncoder;

    @Resource
    private ChatGroupMemberMapper chatGroupMemberMapper;

    @Override
    public CloudFilePageVO listFiles(String userId, int limit, String cursor, String folderId) throws Exception {
        int normalizedLimit = normalizeLimit(limit);
        // 使用 folderId 构建前缀来实现文件夹过滤
        String prefix = buildFolderPrefix(userId, folderId);
        MinioUtil.CloudListResult listResult = minioUtil.listCloudFilesWithPrefix(userId, normalizedLimit, cursor, prefix);
        long usedBytes = resolveUsedBytes(userId);
        long quotaBytes = resolveQuotaBytes(userId);

        List<CloudFileVO> files = listResult.items().stream()
                .map(item -> toFileVO(userId, item))
                .toList();

        // 统计文件和文件夹数量
        int folderCount = 0;
        int fileCount = 0;
        for (CloudFileVO file : files) {
            if (Boolean.TRUE.equals(file.getIsFolder())) {
                folderCount++;
            } else {
                fileCount++;
            }
        }

        Double usagePercent = quotaBytes > 0 ? (double) usedBytes / quotaBytes * 100 : null;

        return CloudFilePageVO.builder()
                .files(files)
                .used(usedBytes)
                .quota(quotaBytes)
                .usagePercent(usagePercent)
                .nextCursor(listResult.nextCursor())
                .hasMore(listResult.hasMore())
                .currentFolderId(folderId)
                .currentFolderPath(prefix)
                .totalCount(files.size())
                .folderCount(folderCount)
                .fileCount(fileCount)
                .build();
    }

    @Override
    public String upload(MultipartFile file, String userId, String folderId) throws Exception {
        // 检查存储配额
        checkStorageQuota(userId, file.getSize());

        MinioUtil.CloudObjectInfo cloudObjectInfo = minioUtil.uploadToCloudInfo(file, userId, folderId);
        cloudStorageUsageMapper.addUsageDelta(userId, cloudObjectInfo.size());
        return cloudObjectInfo.objectName();
    }

    @Override
    public String getDownloadUrl(String objectName) {
        String userId = UserContextUtil.getCurrentUserId();
        ensureCloudOwnership(objectName, userId);
        return cloudFileAccessSigner.buildAccessPath("download", objectName, userId);
    }

    @Override
    public CloudDownloadVO downloadFile(String objectName) throws Exception {
        String userId = UserContextUtil.getCurrentUserId();
        return downloadFile(objectName, userId, null, null);
    }

    @Override
    public CloudDownloadVO downloadFile(String objectName, String userId, Long offset, Long length) throws Exception {
        ensureCloudOwnership(objectName, userId);
        StatObjectResponse stat = requireStat(objectName);
        String fileName = extractFileName(objectName);
        String contentType = CloudFileSupport.normalizeContentType(fileName, stat.contentType());
        InputStream inputStream = minioUtil.getCloudFileStream(objectName, offset, length);
        return CloudDownloadVO.builder()
                .inputStream(inputStream)
                .fileName(fileName)
                .contentType(contentType)
                .size(stat.size())
                .build();
    }

    @Override
    public CloudPreviewContentVO getPreviewContent(String objectName) throws Exception {
        String userId = UserContextUtil.getCurrentUserId();
        ensureCloudOwnership(objectName, userId);
        StatObjectResponse stat = requireStat(objectName);
        String fileName = extractFileName(objectName);
        CloudFileSupport.Descriptor descriptor = CloudFileSupport.describe(fileName, stat.contentType());

        if (!descriptor.previewable()) {
            return CloudPreviewContentVO.builder()
                    .mode("unsupported")
                    .title(fileName)
                    .contentType(descriptor.contentType())
                    .message("当前文件类型暂不支持在线预览，请先下载后查看。")
                    .build();
        }

        if (descriptor.binaryPreview() || descriptor.streamable()) {
            return CloudPreviewContentVO.builder()
                    .mode(descriptor.previewMode())
                    .title(fileName)
                    .contentType(descriptor.contentType())
                    .message("当前文件将通过预览地址直接打开。")
                    .build();
        }

        if (stat.size() > MAX_PREVIEW_SOURCE_BYTES) {
            return CloudPreviewContentVO.builder()
                    .mode("unsupported")
                    .title(fileName)
                    .contentType(descriptor.contentType())
                    .message("该文件过大，当前仅支持下载后查看。")
                    .build();
        }

        byte[] bytes;
        try (InputStream inputStream = minioUtil.getCloudFileStream(objectName)) {
            bytes = inputStream.readAllBytes();
        }

        if (descriptor.textPreview()) {
            return CloudDocumentPreviewRenderer.renderText(fileName, descriptor.contentType(), bytes);
        }

        if (descriptor.documentPreview()) {
            return CloudDocumentPreviewRenderer.renderDocument(
                    fileName,
                    descriptor.contentType(),
                    descriptor.extension(),
                    bytes
            );
        }

        return CloudPreviewContentVO.builder()
                .mode("unsupported")
                .title(fileName)
                .contentType(descriptor.contentType())
                .message("当前文件类型暂不支持在线预览，请先下载后查看。")
                .build();
    }

    @Override
    public void delete(String objectName, String userId) throws Exception {
        ensureCloudOwnership(objectName, userId);
        long removedSize = minioUtil.removeCloudObject(objectName);
        cloudStorageUsageMapper.addUsageDelta(userId, -removedSize);
    }

    @Override
    public String importByUrl(String url, String userId) throws Exception {
        MinioUtil.CloudObjectInfo cloudObjectInfo = minioUtil.importToCloudByUrlInfo(url, userId);
        cloudStorageUsageMapper.addUsageDelta(userId, cloudObjectInfo.size());
        return cloudObjectInfo.objectName();
    }

    // ==================== 文件夹管理实现 ====================

    @Override
    public String createFolder(String folderName, String parentId, String userId) throws Exception {
        if (folderName == null || folderName.trim().isEmpty()) {
            throw new BusinessException("文件夹名称不能为空");
        }
        // 清理名称
        folderName = folderName.trim().replace("/", "_").replace("\\", "_");
        // 构建文件夹路径（以 / 结尾表示是文件夹）
        String prefix = buildFolderPrefix(userId, parentId);
        String folderPath = prefix + folderName + "/";
        // 创建空对象作为文件夹标记
        String objectName = folderPath + ".folder";
        minioUtil.createFolderMarker(objectName);
        log.info("用户[{}]创建了文件夹[{}], 路径: {}", userId, folderName, folderPath);
        return folderPath;
    }

    @Override
    public void rename(String objectName, String newName, String userId) throws Exception {
        ensureCloudOwnership(objectName, userId);
        if (newName == null || newName.trim().isEmpty()) {
            throw new BusinessException("新名称不能为空");
        }
        newName = newName.trim().replace("/", "_").replace("\\", "_");
        // 获取原对象的父路径和扩展名
        String userPrefix = "cloud/" + userId + "/";
        if (!objectName.startsWith(userPrefix)) {
            throw new BusinessException("无效的文件路径");
        }
        String relativePath = objectName.substring(userPrefix.length());
        int lastSlash = relativePath.lastIndexOf('/');
        String parentPath = lastSlash > 0 ? relativePath.substring(0, lastSlash + 1) : "";
        boolean isFolder = objectName.endsWith("/") || objectName.contains("/.folder");
        String newObjectName = userPrefix + parentPath + newName + (isFolder ? "/" : "");
        // MinIO 不支持直接重命名，需要复制后删除
        minioUtil.copyCloudObject(objectName, newObjectName);
        minioUtil.removeCloudObject(objectName);
        log.info("用户[{}]重命名文件[{}]为[{}]", userId, objectName, newObjectName);
    }

    @Override
    public void moveToFolder(String objectName, String targetFolderId, String userId) throws Exception {
        ensureCloudOwnership(objectName, userId);
        String userPrefix = "cloud/" + userId + "/";
        if (!objectName.startsWith(userPrefix)) {
            throw new BusinessException("无效的文件路径");
        }
        // 构建目标路径
        String targetPrefix = buildFolderPrefix(userId, targetFolderId);
        String relativePath = objectName.substring(userPrefix.length());
        // 获取文件名（保留原始扩展名）
        String fileName = relativePath.contains("/")
                ? relativePath.substring(relativePath.lastIndexOf('/') + 1)
                : relativePath;
        String newObjectName = targetPrefix + fileName;
        // 移动（复制后删除）
        minioUtil.copyCloudObject(objectName, newObjectName);
        minioUtil.removeCloudObject(objectName);
        log.info("用户[{}]移动文件[{}]到[{}]", userId, objectName, newObjectName);
    }

    private String buildFolderPrefix(String userId, String folderId) {
        if (folderId == null || folderId.trim().isEmpty() || folderId.equals("/") || folderId.equals("root")) {
            return "cloud/" + userId + "/";
        }
        // folderId 应该是以 cloud/userId/ 开头的路径
        if (!folderId.startsWith("cloud/" + userId + "/")) {
            return "cloud/" + userId + "/" + folderId + "/";
        }
        return folderId.endsWith("/") ? folderId : folderId + "/";
    }

    @Override
    public CloudShareVO createShare(String fileId, String password, Integer expireDays, Long userId) throws Exception {
        // Ensure user owns the file
        ensureCloudOwnership(fileId, userId.toString());

        // Generate share code
        String shareCode = generateShareCode();

        // Calculate expire time
        LocalDateTime expireTime = null;
        if (expireDays != null && expireDays > 0) {
            expireTime = LocalDateTime.now().plusDays(expireDays);
        }

        // Encrypt password if provided using BCrypt
        String encryptedPassword = null;
        if (password != null && !password.isBlank()) {
            encryptedPassword = passwordEncoder.encode(password);
        }

        // Create share record
        CloudShare share = new CloudShare();
        share.setFileId(fileId);
        share.setUserId(userId);
        share.setShareCode(shareCode);
        share.setPassword(encryptedPassword);
        share.setExpireTime(expireTime);
        share.setDownloadCount(0);
        cloudShareMapper.insert(share);

        // Build response
        return buildShareVO(share, fileId);
    }

    @Override
    public List<CloudShareVO> listShares(Long userId) throws Exception {
        List<CloudShare> shares = cloudShareMapper.selectByUserId(userId);
        List<CloudShareVO> result = new ArrayList<>(shares.size());
        for (CloudShare share : shares) {
            result.add(buildShareVO(share, share.getFileId()));
        }
        return result;
    }

    @Override
    public CloudShareVO getShareByCode(String shareCode, String password) throws Exception {
        CloudShare share = cloudShareMapper.selectByShareCode(shareCode);
        if (share == null) {
            throw new BusinessException("分享不存在或已过期");
        }

        // Check password using BCrypt
        if (share.getPassword() != null && !share.getPassword().isBlank()) {
            if (password == null || !passwordEncoder.matches(password, share.getPassword())) {
                throw new BusinessException("提取码错误");
            }
        }

        return buildShareVO(share, share.getFileId());
    }

    @Override
    public void incrementDownloadCount(Long shareId) {
        CloudShare share = cloudShareMapper.selectById(shareId);
        if (share != null) {
            share.setDownloadCount(share.getDownloadCount() + 1);
            cloudShareMapper.updateById(share);
        }
    }

    @Override
    public void cancelShare(Long shareId, Long userId) {
        CloudShare share = cloudShareMapper.selectById(shareId);
        if (share == null) {
            throw new BusinessException("分享不存在");
        }
        if (!share.getUserId().equals(userId)) {
            throw new BusinessException("无权取消此分享");
        }
        cloudShareMapper.deleteById(shareId);
    }

    @Override
    public List<GroupFileVO> listGroupFiles(String groupId, Long userId) throws Exception {
        List<GroupFile> files = groupFileMapper.selectByGroupId(groupId);
        return files.stream()
                .map(this::buildGroupFileVO)
                .toList();
    }

    @Override
    public GroupFileVO saveFileFromChat(String fileId, String fileName, Long fileSize, String fileType, String groupId, Long userId) throws Exception {
        // Copy file to group file storage
        String newObjectName = "group/" + groupId + "/" + System.currentTimeMillis() + "_" + fileName;
        minioUtil.copyCloudObject(fileId, newObjectName);

        // Create group file record
        GroupFile groupFile = new GroupFile();
        groupFile.setGroupId(groupId);
        groupFile.setFileId(Long.parseLong(fileId));
        groupFile.setFileName(fileName);
        groupFile.setFileSize(fileSize);
        groupFile.setFileType(fileType);
        groupFile.setUploaderId(userId);
        groupFileMapper.insert(groupFile);

        return buildGroupFileVO(groupFile);
    }

    private CloudShareVO buildShareVO(CloudShare share, String fileId) throws Exception {
        CloudShareVO vo = new CloudShareVO();
        vo.setId(share.getId());
        vo.setFileId(fileId);
        vo.setShareCode(share.getShareCode());
        vo.setShareUrl("/pages/share/extract?code=" + share.getShareCode());
        vo.setDownloadCount(share.getDownloadCount());
        vo.setCreateTime(share.getCreateTime() != null ? share.getCreateTime().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime() : null);
        vo.setExpireTime(share.getExpireTime());
        vo.setHasPassword(share.getPassword() != null && !share.getPassword().isBlank());
        vo.setUserId(share.getUserId());

        // 检查是否过期
        if (share.getExpireTime() != null) {
            vo.setExpireDays((int) java.time.Duration.between(LocalDateTime.now(), share.getExpireTime()).toDays());
            vo.setExpired(LocalDateTime.now().isAfter(share.getExpireTime()));
        } else {
            vo.setExpireDays(null);
            vo.setExpired(false);
        }

        // Get file info from MinIO - let GlobalExceptionHandler handle any exceptions
        try {
            StatObjectResponse stat = minioUtil.getCloudFileStat(fileId);
            vo.setFileName(extractFileName(fileId));
            vo.setFileSize(stat.size());
            vo.setFileType(stat.contentType());
        } catch (Exception e) {
            log.warn("Failed to get file stat for share {}, using basic info", fileId, e);
            vo.setFileName(extractFileName(fileId));
            vo.setFileType(null);
        }

        // 获取分享者信息
        ChatUser user = chatUserMapper.selectById(share.getUserId());
        if (user != null) {
            vo.setUserName(user.getUsername());
            vo.setUserAvatar(user.getAvatar());
        }

        return vo;
    }

    private GroupFileVO buildGroupFileVO(GroupFile groupFile) {
        GroupFileVO vo = new GroupFileVO();
        vo.setId(groupFile.getId());
        vo.setGroupId(groupFile.getGroupId());
        vo.setFileId(groupFile.getFileId().toString());
        vo.setFileName(groupFile.getFileName());
        vo.setFileSize(groupFile.getFileSize());
        vo.setFileType(groupFile.getFileType());
        vo.setUploaderId(groupFile.getUploaderId());
        vo.setUploadTime(groupFile.getCreateTime() != null ? groupFile.getCreateTime().toString() : null);
        vo.setUpdateTime(groupFile.getUpdateTime() != null ? groupFile.getUpdateTime().toString() : null);

        // 获取文件扩展名和分类
        String extension = CloudFileSupport.resolveExtension(groupFile.getFileName());
        vo.setExtension(extension);
        CloudFileSupport.Descriptor descriptor = CloudFileSupport.describe(groupFile.getFileName(), groupFile.getFileType());
        vo.setCategory(descriptor.category());
        vo.setPreviewable(descriptor.previewable());
        vo.setPreviewMode(descriptor.previewMode());

        // Get uploader name
        ChatUser user = chatUserMapper.selectById(groupFile.getUploaderId());
        if (user != null) {
            vo.setUploaderName(user.getUsername());
        }

        // Build download URL
        vo.setDownloadUrl(cloudFileAccessSigner.buildAccessPath("download", groupFile.getFileId().toString(), groupFile.getUploaderId().toString()));

        // Build preview URL
        if (descriptor.previewable()) {
            if (descriptor.binaryPreview() || descriptor.streamable()) {
                vo.setPreviewUrl(cloudFileAccessSigner.buildAccessPath("inline", groupFile.getFileId().toString(), groupFile.getUploaderId().toString()));
            }
        }

        return vo;
    }

    @Override
    public void deleteGroupFile(Long fileId, Long userId) throws Exception {
        // 1. 查询群组文件记录
        GroupFile groupFile = groupFileMapper.selectById(fileId);
        if (groupFile == null) {
            throw new BusinessException("文件不存在");
        }

        // 2. 权限检查：只有文件上传者或群管理员/群主可以删除
        if (!groupFile.getUploaderId().equals(userId)) {
            // 不是上传者，检查是否有群管理权限
            Long groupId;
            try {
                groupId = Long.parseLong(groupFile.getGroupId());
            } catch (NumberFormatException e) {
                throw new BusinessException("无效的群ID");
            }
            String userIdStr = userId.toString();

            // 需要查询用户在群中的角色
            ChatGroupMember groupMember = chatGroupMemberMapper.selectOne(
                    new LambdaQueryWrapper<ChatGroupMember>()
                            .eq(ChatGroupMember::getGroupId, groupId)
                            .eq(ChatGroupMember::getUserId, userIdStr)
                            .eq(ChatGroupMember::getIsQuit, 0)
            );

            if (groupMember == null) {
                throw new BusinessException("您不在该群中，无权删除此文件");
            }

            Integer role = groupMember.getRole();
            // 角色值：1-群主，2-管理员，3-普通成员
            if (role == null || role > 2) {
                throw new BusinessException("只有文件上传者或群管理员可以删除文件");
            }
        }

        // 3. 删除 MinIO 中的文件
        String objectName = groupFile.getFileId().toString();
        minioUtil.removeCloudObject(objectName);

        // 4. 删除数据库记录
        groupFileMapper.deleteById(fileId);

        log.info("用户[{}]删除了群组文件[{}], 文件名: {}", userId, fileId, groupFile.getFileName());
    }

    private String generateShareCode() {
        String chars = "0123456789";
        Random random = new Random();
        StringBuilder code = new StringBuilder();
        for (int i = 0; i < 6; i++) {
            code.append(chars.charAt(random.nextInt(chars.length())));
        }
        return code.toString();
    }

    private CloudFileVO toFileVO(String userId, MinioUtil.CloudObjectItem item) {
        StatObjectResponse stat;
        boolean isFolder = item.objectName().endsWith("/") || item.objectName().contains("/.folder");
        String fileName = item.fileName();

        try {
            stat = requireStat(item.objectName());
        } catch (Exception e) {
            log.warn("failed to get file stat for {}, returning basic info", item.objectName(), e);
            CloudFileSupport.Descriptor descriptor = CloudFileSupport.describe(fileName, null);
            return CloudFileVO.builder()
                    .name(fileName)
                    .object(item.objectName())
                    .size(item.size())
                    .modifyTime(item.modifyTime())
                    .contentType(descriptor.contentType())
                    .extension(descriptor.extension())
                    .category(descriptor.category())
                    .previewMode(descriptor.previewMode())
                    .previewable(descriptor.previewable())
                    .streamable(descriptor.streamable())
                    .downloadUrl(cloudFileAccessSigner.buildAccessPath("download", item.objectName(), userId))
                    .isFolder(isFolder)
                    .build();
        }

        CloudFileSupport.Descriptor descriptor = CloudFileSupport.describe(fileName, stat.contentType());
        return CloudFileVO.builder()
                .name(fileName)
                .object(item.objectName())
                .size(stat.size())
                .modifyTime(item.modifyTime())
                .createTime(item.modifyTime())
                .contentType(descriptor.contentType())
                .extension(descriptor.extension())
                .category(descriptor.category())
                .previewMode(descriptor.previewMode())
                .previewable(descriptor.previewable())
                .streamable(descriptor.streamable())
                .downloadUrl(cloudFileAccessSigner.buildAccessPath("download", item.objectName(), userId))
                .previewUrl(descriptor.binaryPreview()
                        ? cloudFileAccessSigner.buildAccessPath("inline", item.objectName(), userId)
                        : null)
                .streamUrl(descriptor.streamable()
                        ? cloudFileAccessSigner.buildAccessPath("stream", item.objectName(), userId)
                        : null)
                .isFolder(isFolder)
                .version(1L)
                .downloadCount(0L)
                .starred(false)
                .build();
    }

    private StatObjectResponse requireStat(String objectName) throws Exception {
        StatObjectResponse stat = minioUtil.getCloudFileStat(objectName);
        if (stat == null) {
            throw new BusinessException("文件不存在");
        }
        return stat;
    }

    private long resolveUsedBytes(String userId) throws Exception {
        Long usedBytes = cloudStorageUsageMapper.selectUsedBytesByUserId(userId);
        if (usedBytes != null) {
            return usedBytes;
        }

        long calculated = minioUtil.calculateCloudUsedBytes(userId);
        cloudStorageUsageMapper.upsertUsedBytes(userId, calculated);
        return calculated;
    }

    private long resolveQuotaBytes(String userId) {
        Long quotaBytes = cloudStorageUsageMapper.selectQuotaBytesByUserId(userId);
        if (quotaBytes != null && quotaBytes > 0) {
            return quotaBytes;
        }
        // 初始化默认配额
        cloudStorageUsageMapper.initStorageQuota(userId, DEFAULT_QUOTA_BYTES);
        return DEFAULT_QUOTA_BYTES;
    }

    /**
     * 检查存储配额
     * @param userId 用户ID
     * @param fileSize 文件大小
     * @throws BusinessException 如果超出配额
     */
    private void checkStorageQuota(String userId, long fileSize) throws Exception {
        long usedBytes = resolveUsedBytes(userId);
        long quotaBytes = resolveQuotaBytes(userId);

        if (quotaBytes > 0 && usedBytes + fileSize > quotaBytes) {
            long remaining = quotaBytes - usedBytes;
            String message = String.format("存储空间不足，剩余 %s，无法上传 %s 的文件",
                    formatBytes(remaining), formatBytes(fileSize));
            throw new BusinessException(message);
        }
    }

    /**
     * 格式化字节大小为可读字符串
     */
    private String formatBytes(long bytes) {
        if (bytes < 0) {
            return "0 B";
        }
        String[] units = {"B", "KB", "MB", "GB", "TB"};
        int unitIndex = 0;
        double size = bytes;
        while (size >= 1024 && unitIndex < units.length - 1) {
            size /= 1024;
            unitIndex++;
        }
        return String.format("%.2f %s", size, units[unitIndex]);
    }

    private void ensureCloudOwnership(String objectName, String userId) {
        String prefix = "cloud/" + userId + "/";
        if (objectName == null || !objectName.startsWith(prefix)) {
            throw new BusinessException("无权操作该文件");
        }
    }

    private int normalizeLimit(int limit) {
        if (limit <= 0) {
            return DEFAULT_LIST_LIMIT;
        }
        return Math.min(limit, MAX_LIST_LIMIT);
    }

    private String extractFileName(String objectName) {
        int index = objectName == null ? -1 : objectName.lastIndexOf('/');
        if (index < 0 || index == objectName.length() - 1) {
            return objectName == null ? "未命名文件" : objectName;
        }
        return objectName.substring(index + 1);
    }
}
