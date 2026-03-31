package com.maxxvll.service;

import com.maxxvll.common.vo.CloudDownloadVO;
import com.maxxvll.common.vo.CloudFilePageVO;
import com.maxxvll.common.vo.CloudPreviewContentVO;
import com.maxxvll.common.vo.CloudShareVO;
import com.maxxvll.common.vo.GroupFileVO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 云盘服务接口
 */
public interface CloudDriveService {

    // ==================== 文件操作 ====================
    CloudFilePageVO listFiles(String userId, int limit, String cursor, String folderId) throws Exception;

    String upload(MultipartFile file, String userId, String folderId) throws Exception;

    String getDownloadUrl(String objectName);

    CloudDownloadVO downloadFile(String objectName) throws Exception;

    CloudDownloadVO downloadFile(String objectName, String userId, Long offset, Long length) throws Exception;

    CloudPreviewContentVO getPreviewContent(String objectName) throws Exception;

    void delete(String objectName, String userId) throws Exception;

    String importByUrl(String url, String userId) throws Exception;

    // ==================== 文件夹操作 ====================
    /**
     * 创建文件夹
     * @param folderName 文件夹名称
     * @param parentId 父文件夹ID（null表示根目录）
     * @param userId 用户ID
     * @return 创建的文件夹对象名
     */
    String createFolder(String folderName, String parentId, String userId) throws Exception;

    /**
     * 重命名文件或文件夹
     * @param objectName 对象名
     * @param newName 新名称
     * @param userId 用户ID
     */
    void rename(String objectName, String newName, String userId) throws Exception;

    /**
     * 移动文件到指定文件夹
     * @param objectName 对象名
     * @param targetFolderId 目标文件夹ID（null表示移动到根目录）
     * @param userId 用户ID
     */
    void moveToFolder(String objectName, String targetFolderId, String userId) throws Exception;

    // Cloud share methods
    CloudShareVO createShare(String fileId, String password, Integer expireDays, Long userId) throws Exception;

    List<CloudShareVO> listShares(Long userId) throws Exception;

    CloudShareVO getShareByCode(String shareCode, String password) throws Exception;

    void incrementDownloadCount(Long shareId);

    void cancelShare(Long shareId, Long userId);

    // Group file methods
    List<GroupFileVO> listGroupFiles(String groupId, Long userId) throws Exception;

    GroupFileVO saveFileFromChat(String fileId, String fileName, Long fileSize, String fileType, String groupId, Long userId) throws Exception;

    void deleteGroupFile(Long fileId, Long userId) throws Exception;
}
