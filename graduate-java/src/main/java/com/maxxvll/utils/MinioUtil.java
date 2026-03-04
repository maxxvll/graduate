package com.maxxvll.utils;

import cn.hutool.core.util.IdUtil;
import io.minio.*;
import io.minio.http.Method;
import io.minio.messages.DeleteObject;
import io.minio.messages.Item;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class MinioUtil {

    @Value("${minio.endpoint}")
    private String endpoint;
    @Value("${minio.access-key}")
    private String accessKey;
    @Value("${minio.secret-key}")
    private String secretKey;
    @Value("${minio.default-bucket}")
    private String bucketName;
    /** 公共头像桶（匿名可读，头像直链永不过期） */
    @Value("${minio.avatar-bucket:chat-avatars-public}")
    private String avatarBucketName;

    private static final String CHUNK_TEMP_BUCKET = "chat-chunks-temp";
    private MinioClient minioClient;

    @PostConstruct
    public void init() {
        try {
            minioClient = MinioClient.builder()
                    .endpoint(endpoint)
                    .credentials(accessKey, secretKey)
                    .build();

            // 主文件桶（私有）
            boolean bucketExists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
            if (!bucketExists) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
                log.info("创建主Bucket成功: {}", bucketName);
            }

            // 切片临时桶
            boolean chunkBucketExists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(CHUNK_TEMP_BUCKET).build());
            if (!chunkBucketExists) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(CHUNK_TEMP_BUCKET).build());
                log.info("创建切片Bucket成功: {}", CHUNK_TEMP_BUCKET);
            }

            // 公共头像桶（匿名可读，无需签名 URL，彻底解决 Firefox CORS + 头像不更新问题）
            boolean avatarBucketExists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(avatarBucketName).build());
            if (!avatarBucketExists) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(avatarBucketName).build());
                log.info("创建头像公共Bucket成功: {}", avatarBucketName);
            }
            // 设置公共读策略：允许所有人匿名 GET（Head + Get）
            String avatarBucketPolicy = String.format(
                    "{\"Version\":\"2012-10-17\",\"Statement\":[{\"Effect\":\"Allow\",\"Principal\":{\"AWS\":[\"*\"]},"
                  + "\"Action\":[\"s3:GetObject\",\"s3:HeadObject\"],\"Resource\":[\"arn:aws:s3:::%s/*\"]}]}",
                    avatarBucketName);
            minioClient.setBucketPolicy(SetBucketPolicyArgs.builder()
                    .bucket(avatarBucketName)
                    .config(avatarBucketPolicy)
                    .build());
            log.info("头像Bucket公共读策略已设置: {}", avatarBucketName);
        } catch (Exception e) {
            log.error("Minio初始化失败", e);
        }
    }

    // ==================== 头像上传（公共桶，永久直链）====================
    /**
     * 上传用户头像到公共头像桶。
     * 使用固定路径 avatar/user/{userId}（无 UUID 后缀），每次上传都覆盖同一对象，
     * 使得 URL 永不变化，所有持有此 URL 的地方自动看到最新头像，彻底解决头像不同步问题。
     *
     * @param file   头像文件
     * @param userId 用户ID
     * @return 存储的相对路径，如 avatar/user/123
     */
    public String uploadAvatar(MultipartFile file, String userId) throws Exception {
        // 使用固定路径，覆盖旧头像，URL 永远不变
        String fileName = "avatar/user/" + userId;

        minioClient.putObject(
                PutObjectArgs.builder()
                        .bucket(avatarBucketName)
                        .object(fileName)
                        .stream(file.getInputStream(), file.getSize(), -1)
                        .contentType(file.getContentType())
                        .build()
        );
        log.info("用户头像上传成功（公共桶，固定路径）: {}/{}", avatarBucketName, fileName);
        return fileName;
    }

    // ==================== 群头像上传（公共桶）====================
    /**
     * 上传群头像到公共头像桶（UUID 路径，每次创建新对象）。
     *
     * @param file 头像文件
     * @return 存储的相对路径，如 avatar/group/{uuid}.jpg
     */
    public String uploadGroupAvatar(MultipartFile file) throws Exception {
        String originalFilename = file.getOriginalFilename();
        String suffix = ".jpg";
        if (originalFilename != null && originalFilename.contains(".")) {
            suffix = originalFilename.substring(originalFilename.lastIndexOf(".")).toLowerCase();
        }
        String fileName = String.format("avatar/group/%s%s", IdUtil.simpleUUID(), suffix);
        minioClient.putObject(
                PutObjectArgs.builder()
                        .bucket(avatarBucketName)
                        .object(fileName)
                        .stream(file.getInputStream(), file.getSize(), -1)
                        .contentType(file.getContentType())
                        .build()
        );
        log.info("群头像上传成功（公共桶）: {}/{}", avatarBucketName, fileName);
        return fileName;
    }

    public void uploadChatFile(MultipartFile file, String fileName, boolean isPublic) throws Exception {
        minioClient.putObject(
                PutObjectArgs.builder()
                        .bucket(bucketName)
                        .object(fileName)
                        .stream(file.getInputStream(), file.getSize(), -1)
                        .contentType(file.getContentType())
                        .build()
        );
    }

    public String getChatFileUrl(String fileName, boolean isPublic) {
        if (fileName == null || fileName.isEmpty()) return "";
        // 如果已经是 http 开头，直接返回
        if (fileName.startsWith("http")) return fileName;

        try {
            return minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .bucket(bucketName)
                            .object(fileName)
                            .method(Method.GET)
                            .expiry(7, TimeUnit.DAYS)
                            .build()
            );
        } catch (Exception e) {
            log.error("获取文件URL失败: {}", fileName, e);
            return "";
        }
    }

    // ==================== 获取头像永久直链 ====================
    /**
     * 获取头像 URL。
     * <p>
     * 路径格式说明：
     * <ul>
     *   <li>{@code avatar/user/{id}}：新格式用户头像（公共桶）→ 永久直链</li>
     *   <li>{@code avatar/group/{uuid}.ext}：新格式群头像（公共桶）→ 永久直链</li>
     *   <li>{@code avatar/{numericId}/{uuid}_name.ext}：旧格式用户头像（私有桶）→ 预签名 URL</li>
     *   <li>{@code http...}：已是完整 URL，直接返回</li>
     * </ul>
     *
     * @param fileName 数据库存储的相对路径或完整 URL
     * @return 可直接在浏览器/img标签中使用的 URL
     */
    public String getAvatarUrl(String fileName) {
        if (fileName == null || fileName.isBlank()) return "";
        // 已经是完整 URL，直接返回（历史旧数据或外部 URL）
        if (fileName.startsWith("http")) return fileName;

        String base = endpoint.endsWith("/") ? endpoint.substring(0, endpoint.length() - 1) : endpoint;

        // 新格式路径：第二节为 "user" 或 "group"，存放在公共桶，返回永久直链
        if (fileName.startsWith("avatar/user/") || fileName.startsWith("avatar/group/")) {
            return base + "/" + avatarBucketName + "/" + fileName;
        }

        // 旧格式路径（如 avatar/{numericId}/{uuid}_name.ext）：存在于私有桶，回退预签名 URL
        return getChatFileUrl(fileName, true);
    }

    public String checkFileExistByMd5(String md5) {
        log.warn("checkFileExistByMd5 未实现数据库查询，秒传功能暂不可用: md5={}", md5);
        return null;
    }

    // ================ 云盘相关辅助 ===================
    /**
     * 列出指定用户云盘下的所有对象及大小
     */
    public List<Map<String, Object>> listCloudFiles(String userId) throws Exception {
        List<Map<String, Object>> list = new ArrayList<>();
        String prefix = "cloud/" + userId + "/";
        Iterable<Result<Item>> results = minioClient.listObjects(
                ListObjectsArgs.builder().bucket(bucketName).prefix(prefix).recursive(true).build()
        );
        for (Result<Item> r : results) {
            Item item = r.get();
            Map<String, Object> m = new HashMap<>();
            m.put("name", item.objectName().substring(prefix.length()));
            m.put("size", item.size());
            // lastModified may be null but typically present
            if (item.lastModified() != null) {
                m.put("modifyTime", item.lastModified().toString());
            }
            list.add(m);
        }
        return list;
    }

    /**
     * 上传任意文件到用户的云盘目录
     * @return 存储的对象名
     */
    public String uploadToCloud(MultipartFile file, String userId) throws Exception {
        String suffix = "";
        String original = file.getOriginalFilename();
        if (original != null && original.contains(".")) {
            suffix = original.substring(original.lastIndexOf("."));
        }
        String objectName = String.format("cloud/%s/%s%s", userId, IdUtil.simpleUUID(), suffix);
        minioClient.putObject(
                PutObjectArgs.builder()
                        .bucket(bucketName)
                        .object(objectName)
                        .stream(file.getInputStream(), file.getSize(), -1)
                        .contentType(file.getContentType())
                        .build()
        );
        return objectName;
    }

    /**
     * 从远程链接拉取内容并存到用户云盘
     */
    public String importToCloudByUrl(String url, String userId) throws Exception {
        // 简化实现：使用 Java URL 读取流
        try (InputStream in = new java.net.URL(url).openStream()) {
            String suffix = "";
            String path = new java.net.URL(url).getPath();
            if (path.contains(".")) suffix = path.substring(path.lastIndexOf("."));
            String objectName = String.format("cloud/%s/%s%s", userId, IdUtil.simpleUUID(), suffix);
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .stream(in, -1, 10485760) // unknown size
                            .build()
            );
            return objectName;
        }
    }

    public String getCloudFileUrl(String objectName) {
        if (objectName == null || objectName.isEmpty()) return "";
        try {
            return minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .method(Method.GET)
                            .expiry(7, TimeUnit.DAYS)
                            .build()
            );
        } catch (Exception e) {
            log.error("获取云盘文件 URL 失败: {}", objectName, e);
            return "";
        }
    }

    public List<Integer> listUploadedChunks(String md5) {
        List<Integer> chunkIndexList = new ArrayList<>();
        try {
            String prefix = md5 + "/";
            Iterable<Result<Item>> results = minioClient.listObjects(
                    ListObjectsArgs.builder().bucket(CHUNK_TEMP_BUCKET).prefix(prefix).build()
            );

            for (Result<Item> result : results) {
                Item item = result.get();
                String objectName = item.objectName();
                String indexStr = objectName.substring(prefix.length());
                try {
                    chunkIndexList.add(Integer.parseInt(indexStr));
                } catch (NumberFormatException e) {
                    // ignore
                }
            }
        } catch (Exception e) {
            log.error("查询已上传切片列表失败", e);
        }
        return chunkIndexList;
    }

    public void uploadChunk(String md5, Integer chunkIndex, MultipartFile file) throws Exception {
        String objectName = md5 + "/" + chunkIndex;
        minioClient.putObject(
                PutObjectArgs.builder()
                        .bucket(CHUNK_TEMP_BUCKET)
                        .object(objectName)
                        .stream(file.getInputStream(), file.getSize(), -1)
                        .contentType(file.getContentType())
                        .build()
        );
    }

    public String mergeChunks(String md5, String originalFileName, Integer totalChunks) throws Exception {
        log.info("开始合并文件: md5={}, 原始文件名={}", md5, originalFileName);
        String finalObjectName = generateFriendlyFileName(originalFileName);
        boolean mergeSuccess = false;

        try {
            try {
                List<ComposeSource> sourceList = new ArrayList<>();
                for (int i = 0; i < totalChunks; i++) {
                    sourceList.add(ComposeSource.builder().bucket(CHUNK_TEMP_BUCKET).object(md5 + "/" + i).build());
                }
                minioClient.composeObject(ComposeObjectArgs.builder().bucket(bucketName).object(finalObjectName).sources(sourceList).build());
                mergeSuccess = true;
                log.info("Minio服务端合并成功: {}", finalObjectName);
            } catch (Exception e) {
                log.warn("Minio服务端合并失败，启用降级方案", e);
            }

            if (!mergeSuccess) {
                mergeChunksLocally(md5, finalObjectName, totalChunks);
            }
        } finally {
            cleanTempChunks(md5, totalChunks);
        }
        return finalObjectName;
    }

    private void mergeChunksLocally(String md5, String finalObjectName, Integer totalChunks) throws Exception {
        log.info("执行本地流式合并: {}", finalObjectName);
        List<byte[]> chunkDataList = new ArrayList<>();
        long totalSize = 0;
        for (int i = 0; i < totalChunks; i++) {
            try (InputStream stream = minioClient.getObject(GetObjectArgs.builder().bucket(CHUNK_TEMP_BUCKET).object(md5 + "/" + i).build())) {
                byte[] bytes = stream.readAllBytes();
                chunkDataList.add(bytes);
                totalSize += bytes.length;
            }
        }
        byte[] finalFileBytes = new byte[(int) totalSize];
        int offset = 0;
        for (byte[] chunk : chunkDataList) {
            System.arraycopy(chunk, 0, finalFileBytes, offset, chunk.length);
            offset += chunk.length;
        }
        try (ByteArrayInputStream bais = new ByteArrayInputStream(finalFileBytes)) {
            minioClient.putObject(PutObjectArgs.builder().bucket(bucketName).object(finalObjectName).stream(bais, finalFileBytes.length, -1).build());
        }
    }

    private String generateFriendlyFileName(String originalFileName) {
        String uuid = IdUtil.simpleUUID();
        String suffix = "";
        String pureName = originalFileName;
        if (originalFileName.contains(".")) {
            int lastDotIndex = originalFileName.lastIndexOf(".");
            suffix = originalFileName.substring(lastDotIndex);
            pureName = originalFileName.substring(0, lastDotIndex);
        }
        if (pureName.length() > 40) pureName = pureName.substring(0, 40);
        StringBuilder finalPath = new StringBuilder("chat/files/");
        finalPath.append(uuid);
        if (pureName != null && !pureName.trim().isEmpty()) {
            finalPath.append("_").append(pureName);
        }
        finalPath.append(suffix);
        return finalPath.toString();
    }

    private void cleanTempChunks(String md5, Integer totalChunks) {
        try {
            List<DeleteObject> deleteObjectList = new ArrayList<>();
            for (int i = 0; i < totalChunks; i++) {
                deleteObjectList.add(new DeleteObject(md5 + "/" + i));
            }
            minioClient.removeObjects(RemoveObjectsArgs.builder().bucket(CHUNK_TEMP_BUCKET).objects(deleteObjectList).build());
        } catch (Exception e) {
            log.error("清理切片失败", e);
        }
    }

    public void uploadFile(MultipartFile file, String fileName) throws Exception {
        minioClient.putObject(
                PutObjectArgs.builder()
                        .bucket(bucketName)
                        .object(fileName)
                        .stream(file.getInputStream(), file.getSize(), -1)
                        .contentType(file.getContentType())
                        .build()
        );
        log.info("通用文件上传成功: {}", fileName);
    }

    /**
     * 删除指定对象（用于网盘删除文件）
     */
    public void removeObject(String objectName) throws Exception {
        minioClient.removeObject(
                RemoveObjectArgs.builder()
                        .bucket(bucketName)
                        .object(objectName)
                        .build()
        );
        log.info("删除对象成功: {}", objectName);
    }

    public String getPublicUrl(String fileName) {
        return getChatFileUrl(fileName, true);
    }
}