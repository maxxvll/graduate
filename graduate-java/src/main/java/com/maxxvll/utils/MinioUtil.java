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
import java.util.List;
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

    private static final String CHUNK_TEMP_BUCKET = "chat-chunks-temp";
    private MinioClient minioClient;

    @PostConstruct
    public void init() {
        try {
            minioClient = MinioClient.builder()
                    .endpoint(endpoint)
                    .credentials(accessKey, secretKey)
                    .build();

            boolean bucketExists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
            if (!bucketExists) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
                log.info("创建主Bucket成功: {}", bucketName);
            }

            boolean chunkBucketExists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(CHUNK_TEMP_BUCKET).build());
            if (!chunkBucketExists) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(CHUNK_TEMP_BUCKET).build());
                log.info("创建切片Bucket成功: {}", CHUNK_TEMP_BUCKET);
            }
        } catch (Exception e) {
            log.error("Minio初始化失败", e);
        }
    }

    // ==================== 【新增】专门用于头像上传 ====================
    /**
     * 上传头像
     * @param file 文件
     * @param userId 用户ID
     * @return 存储的相对路径 (如 avatar/123/uuid_filename.jpg)
     */
    public String uploadAvatar(MultipartFile file, String userId) throws Exception {
        String originalFilename = file.getOriginalFilename();
        String suffix = "";
        String pureName = "avatar";

        if (originalFilename != null && originalFilename.contains(".")) {
            int lastDotIndex = originalFilename.lastIndexOf(".");
            suffix = originalFilename.substring(lastDotIndex);
            pureName = originalFilename.substring(0, lastDotIndex);
            // 防止原始文件名过长
            if (pureName.length() > 30) pureName = pureName.substring(0, 30);
        }

        // 生成路径: avatar/{userId}/{uuid}_{pureName}{suffix}
        String fileName = String.format("avatar/%s/%s_%s%s", userId, IdUtil.simpleUUID(), pureName, suffix);

        minioClient.putObject(
                PutObjectArgs.builder()
                        .bucket(bucketName)
                        .object(fileName)
                        .stream(file.getInputStream(), file.getSize(), -1)
                        .contentType(file.getContentType())
                        .build()
        );
        log.info("头像上传成功: {}", fileName);
        return fileName; // 【关键】只返回路径，不返回URL
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

    // ==================== 【修改】获取头像URL ====================
    public String getAvatarUrl(String fileName) {
        return getChatFileUrl(fileName, true); // 头像通常是公开的
    }

    public String checkFileExistByMd5(String md5) {
        log.warn("checkFileExistByMd5 未实现数据库查询，秒传功能暂不可用: md5={}", md5);
        return null;
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

    public String getPublicUrl(String fileName) {
        return getChatFileUrl(fileName, true);
    }
}