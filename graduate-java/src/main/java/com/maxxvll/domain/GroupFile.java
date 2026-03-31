package com.maxxvll.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;


/**
 * 群文件表
 * @TableName group_file
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("group_file")
public class GroupFile extends BaseEntity {

    /**
     * 群组ID
     */
    private String groupId;

    /**
     * 文件ID（关联cloud_storage或其他文件系统）
     */
    private Long fileId;

    /**
     * 文件名称
     */
    private String fileName;

    /**
     * 文件大小（字节）
     */
    private Long fileSize;

    /**
     * 文件类型（MIME类型或扩展名）
     */
    private String fileType;

    /**
     * 上传者ID
     */
    private Long uploaderId;
}
