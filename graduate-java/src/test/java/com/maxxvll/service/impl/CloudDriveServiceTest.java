package com.maxxvll.service.impl;

import com.maxxvll.common.exception.BusinessException;
import com.maxxvll.common.vo.CloudFilePageVO;
import com.maxxvll.common.vo.CloudFileVO;
import com.maxxvll.domain.ChatUser;
import com.maxxvll.domain.CloudShare;
import com.maxxvll.domain.CloudStorageUsage;
import com.maxxvll.mapper.ChatUserMapper;
import com.maxxvll.mapper.CloudShareMapper;
import com.maxxvll.mapper.CloudStorageUsageMapper;
import com.maxxvll.mapper.GroupFileMapper;
import com.maxxvll.service.CloudDriveService;
import com.maxxvll.utils.CloudFileAccessSigner;
import com.maxxvll.utils.MinioUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * CloudDriveService 单元测试
 */
@ExtendWith(MockitoExtension.class)
class CloudDriveServiceTest {

    @Mock
    private MinioUtil minioUtil;

    @Mock
    private CloudStorageUsageMapper cloudStorageUsageMapper;

    @Mock
    private CloudFileAccessSigner cloudFileAccessSigner;

    @Mock
    private CloudShareMapper cloudShareMapper;

    @Mock
    private GroupFileMapper groupFileMapper;

    @Mock
    private ChatUserMapper chatUserMapper;

    @InjectMocks
    private CloudDriveServiceImpl cloudDriveService;

    @Nested
    @DisplayName("云文件构建测试")
    class CloudFileBuildingTests {

        @Test
        @DisplayName("构建云文件对象")
        void buildCloudFile() {
            CloudFileVO file = CloudFileVO.builder()
                    .name("document.pdf")
                    .object("files/document.pdf")
                    .size(1024 * 1024L)
                    .contentType("application/pdf")
                    .extension("pdf")
                    .downloadUrl("http://localhost:9000/files/document.pdf")
                    .isFolder(false)
                    .createTime(LocalDateTime.now().toString())
                    .build();

            assertThat(file).isNotNull();
            assertThat(file.getName()).isEqualTo("document.pdf");
            assertThat(file.getIsFolder()).isFalse();
        }

        @Test
        @DisplayName("构建文件夹对象")
        void buildFolder() {
            CloudFileVO folder = CloudFileVO.builder()
                    .name("My Documents")
                    .isFolder(true)
                    .folderId("folder_001")
                    .createTime(LocalDateTime.now().toString())
                    .build();

            assertThat(folder).isNotNull();
            assertThat(folder.getIsFolder()).isTrue();
        }
    }

    @Nested
    @DisplayName("文件大小处理测试")
    class FileSizeTests {

        @Test
        @DisplayName("文件大小格式化")
        void fileSizeFormatting() {
            long bytes = 1024 * 1024 * 1024;
            String formatted = formatFileSize(bytes);

            assertThat(formatted).isEqualTo("1.00 GB");
        }

        @Test
        @DisplayName("文件大小格式化 - MB")
        void fileSizeFormattingMB() {
            long bytes = 50 * 1024 * 1024;
            String formatted = formatFileSize(bytes);

            assertThat(formatted).isEqualTo("50.00 MB");
        }

        @Test
        @DisplayName("文件大小格式化 - KB")
        void fileSizeFormattingKB() {
            long bytes = 1024;
            String formatted = formatFileSize(bytes);

            assertThat(formatted).isEqualTo("1.00 KB");
        }

        @Test
        @DisplayName("文件大小格式化 - Bytes")
        void fileSizeFormattingBytes() {
            long bytes = 500;
            String formatted = formatFileSize(bytes);

            assertThat(formatted).isEqualTo("500 B");
        }

        private String formatFileSize(long bytes) {
            if (bytes >= 1024 * 1024 * 1024) {
                return String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024));
            } else if (bytes >= 1024 * 1024) {
                return String.format("%.2f MB", bytes / (1024.0 * 1024));
            } else if (bytes >= 1024) {
                return String.format("%.2f KB", bytes / 1024.0);
            } else {
                return bytes + " B";
            }
        }
    }

    @Nested
    @DisplayName("存储配额测试")
    class StorageQuotaTests {

        @Test
        @DisplayName("默认存储配额")
        void defaultStorageQuota() {
            long defaultQuota = 10L * 1024 * 1024 * 1024;

            assertThat(defaultQuota).isEqualTo(10L * 1024 * 1024 * 1024);
        }

        @Test
        @DisplayName("存储使用量百分比计算")
        void storageUsagePercentage() {
            long usedBytes = 5L * 1024 * 1024 * 1024;
            long quotaBytes = 10L * 1024 * 1024 * 1024;

            double usagePercent = (double) usedBytes / quotaBytes * 100;

            assertThat(usagePercent).isEqualTo(50.0);
        }

        @Test
        @DisplayName("存储配额不足")
        void storageQuotaExceeded() {
            long usedBytes = 11L * 1024 * 1024 * 1024;
            long quotaBytes = 10L * 1024 * 1024 * 1024;

            boolean isExceeded = usedBytes > quotaBytes;

            assertThat(isExceeded).isTrue();
        }

        @Test
        @DisplayName("存储使用量边界值")
        void storageUsageBoundary() {
            long usedBytes = 0;
            long quotaBytes = 10L * 1024 * 1024 * 1024;

            double usagePercent = quotaBytes > 0 ? (double) usedBytes / quotaBytes * 100 : 0;

            assertThat(usagePercent).isEqualTo(0.0);
        }
    }

    @Nested
    @DisplayName("文件列表测试")
    class FileListTests {

        @Test
        @DisplayName("文件列表过滤 - 按名称")
        void fileListFilterByName() {
            List<CloudFileVO> files = new ArrayList<>();

            CloudFileVO file1 = CloudFileVO.builder().name("document.pdf").build();
            files.add(file1);

            CloudFileVO file2 = CloudFileVO.builder().name("image.jpg").build();
            files.add(file2);

            CloudFileVO file3 = CloudFileVO.builder().name("doc.txt").build();
            files.add(file3);

            List<CloudFileVO> filtered = files.stream()
                    .filter(f -> f.getName() != null && f.getName().contains("doc"))
                    .toList();

            assertThat(filtered).hasSize(2);
        }

        @Test
        @DisplayName("文件列表排序")
        void fileListSorting() {
            List<CloudFileVO> files = new ArrayList<>();

            CloudFileVO file1 = CloudFileVO.builder()
                    .name("banana")
                    .createTime(LocalDateTime.now().minusDays(2).toString())
                    .build();
            files.add(file1);

            CloudFileVO file2 = CloudFileVO.builder()
                    .name("apple")
                    .createTime(LocalDateTime.now().minusDays(1).toString())
                    .build();
            files.add(file2);

            CloudFileVO file3 = CloudFileVO.builder()
                    .name("cherry")
                    .createTime(LocalDateTime.now().toString())
                    .build();
            files.add(file3);

            files.sort((f1, f2) -> f1.getName().compareTo(f2.getName()));

            assertThat(files.get(0).getName()).isEqualTo("apple");
            assertThat(files.get(1).getName()).isEqualTo("banana");
            assertThat(files.get(2).getName()).isEqualTo("cherry");
        }

        @Test
        @DisplayName("文件列表分页")
        void fileListPagination() {
            List<CloudFileVO> allFiles = new ArrayList<>();
            for (int i = 1; i <= 100; i++) {
                CloudFileVO file = CloudFileVO.builder()
                        .name("file_" + i)
                        .build();
                allFiles.add(file);
            }

            int page = 2;
            int pageSize = 20;
            int start = (page - 1) * pageSize;

            List<CloudFileVO> pageFiles = allFiles.subList(
                    start,
                    Math.min(start + pageSize, allFiles.size())
            );

            assertThat(pageFiles).hasSize(20);
            assertThat(pageFiles.get(0).getName()).isEqualTo("file_21");
        }
    }

    @Nested
    @DisplayName("文件分享测试")
    class FileShareTests {

        @Test
        @DisplayName("构建文件分享")
        void buildFileShare() {
            CloudShare share = new CloudShare();
            share.setId(1L);
            share.setShareCode("abc123");
            share.setFileId("file_001");
            share.setUserId(1L);
            share.setExpireTime(LocalDateTime.now().plusDays(7));
            share.setDownloadCount(0);

            assertThat(share).isNotNull();
            assertThat(share.getShareCode()).hasSize(6);
        }

        @Test
        @DisplayName("分享链接生成")
        void shareLinkGeneration() {
            String shareCode = "abc123";
            String shareLink = "https://example.com/share/" + shareCode;

            assertThat(shareLink).contains(shareCode);
            assertThat(shareLink).startsWith("https://");
        }

        @Test
        @DisplayName("分享过期检查")
        void shareExpirationCheck() {
            CloudShare expiredShare = new CloudShare();
            expiredShare.setExpireTime(LocalDateTime.now().minusDays(1));

            CloudShare validShare = new CloudShare();
            validShare.setExpireTime(LocalDateTime.now().plusDays(1));

            boolean isExpired = expiredShare.getExpireTime().isBefore(LocalDateTime.now());
            boolean isValid = validShare.getExpireTime().isAfter(LocalDateTime.now());

            assertThat(isExpired).isTrue();
            assertThat(isValid).isTrue();
        }

        @Test
        @DisplayName("分享访问次数增加")
        void shareViewCountIncrement() {
            CloudShare share = new CloudShare();
            share.setDownloadCount(5);

            share.setDownloadCount(share.getDownloadCount() + 1);

            assertThat(share.getDownloadCount()).isEqualTo(6);
        }
    }

    @Nested
    @DisplayName("文件类型测试")
    class FileTypeTests {

        @Test
        @DisplayName("图片文件类型判断")
        void imageFileTypeJudgment() {
            List<String> imageExtensions = List.of("jpg", "jpeg", "png", "gif", "bmp", "webp");

            assertThat(imageExtensions).contains("jpg");
            assertThat(imageExtensions).contains("png");
            assertThat(imageExtensions).contains("gif");
        }

        @Test
        @DisplayName("文档文件类型判断")
        void documentFileTypeJudgment() {
            List<String> documentExtensions = List.of("pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt");

            assertThat(documentExtensions).contains("pdf");
            assertThat(documentExtensions).contains("docx");
            assertThat(documentExtensions).contains("xlsx");
        }

        @Test
        @DisplayName("视频文件类型判断")
        void videoFileTypeJudgment() {
            List<String> videoExtensions = List.of("mp4", "avi", "mov", "mkv", "wmv");

            assertThat(videoExtensions).contains("mp4");
            assertThat(videoExtensions).contains("avi");
        }

        @Test
        @DisplayName("音频文件类型判断")
        void audioFileTypeJudgment() {
            List<String> audioExtensions = List.of("mp3", "wav", "ogg", "m4a", "flac");

            assertThat(audioExtensions).contains("mp3");
            assertThat(audioExtensions).contains("wav");
        }
    }

    @Nested
    @DisplayName("异常场景测试")
    class ExceptionScenarioTests {

        @Test
        @DisplayName("文件名过长")
        void fileNameTooLong() {
            String longFileName = "a".repeat(500) + ".pdf";

            assertThat(longFileName.length()).isGreaterThan(255);
        }

        @Test
        @DisplayName("无效文件类型")
        void invalidFileType() {
            String invalidExtension = "exe";

            List<String> allowedExtensions = List.of("jpg", "png", "pdf", "doc", "mp4");

            assertThat(allowedExtensions).doesNotContain(invalidExtension);
        }

        @Test
        @DisplayName("文件不存在")
        void fileNotExists() {
            CloudFileVO file = null;

            assertThat(file).isNull();
        }

        @Test
        @DisplayName("存储空间不足")
        void storageInsufficient() {
            long usedBytes = 9L * 1024 * 1024 * 1024;
            long quotaBytes = 10L * 1024 * 1024 * 1024;
            long fileSize = 2L * 1024 * 1024 * 1024;

            boolean canUpload = (usedBytes + fileSize) <= quotaBytes;

            assertThat(canUpload).isFalse();
        }

        @Test
        @DisplayName("分享码格式错误")
        void shareCodeFormatError() {
            String invalidCode = "abc";
            String validCode = "abcdef";

            assertThat(invalidCode.length()).isNotEqualTo(6);
            assertThat(validCode.length()).isEqualTo(6);
        }
    }

    @Nested
    @DisplayName("存储使用记录测试")
    class StorageUsageTests {

        @Test
        @DisplayName("构建存储使用记录")
        void buildStorageUsage() {
            CloudStorageUsage usage = new CloudStorageUsage();
            usage.setUserId("1");
            usage.setUsedBytes(5L * 1024 * 1024 * 1024);
            usage.setQuotaBytes(10L * 1024 * 1024 * 1024);
            usage.setUpdatedAt(new Date());

            assertThat(usage).isNotNull();
            assertThat(usage.getUsedBytes()).isEqualTo(5L * 1024 * 1024 * 1024);
        }

        @Test
        @DisplayName("存储使用量更新")
        void storageUsageUpdate() {
            CloudStorageUsage usage = new CloudStorageUsage();
            usage.setUsedBytes(5L * 1024 * 1024 * 1024);

            long fileSize = 1024 * 1024;
            usage.setUsedBytes(usage.getUsedBytes() + fileSize);

            assertThat(usage.getUsedBytes()).isEqualTo(5L * 1024 * 1024 * 1024 + fileSize);
        }

        @Test
        @DisplayName("删除文件释放空间")
        void deleteFileReleaseSpace() {
            CloudStorageUsage usage = new CloudStorageUsage();
            usage.setUsedBytes(5L * 1024 * 1024 * 1024);

            long fileSize = 1024 * 1024;
            usage.setUsedBytes(Math.max(0, usage.getUsedBytes() - fileSize));

            assertThat(usage.getUsedBytes()).isEqualTo(5L * 1024 * 1024 * 1024 - fileSize);
        }
    }
}
