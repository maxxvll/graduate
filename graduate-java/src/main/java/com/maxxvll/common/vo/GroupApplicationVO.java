package com.maxxvll.common.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;

/**
 * 群申请信息VO
 */
@Data
public class GroupApplicationVO {
    
    /**
     * 申请ID
     */
    private Long id;
    
    /**
     * 群ID
     */
    private String groupId;
    
    /**
     * 群名称
     */
    private String groupName;
    
    /**
     * 申请人ID
     */
    private String applicantId;
    
    /**
     * 申请人昵称
     */
    private String applicantNickname;
    
    /**
     * 申请人头像
     */
    private String applicantAvatar;
    
    /**
     * 申请状态：0-待处理，1-已通过，2-已拒绝
     */
    private Integer status;
    
    /**
     * 状态描述
     */
    private String statusDesc;
    
    /**
     * 拒绝原因
     */
    private String rejectReason;
    
    /**
     * 操作人ID
     */
    private String operatorId;
    
    /**
     * 操作人昵称
     */
    private String operatorNickname;
    
    /**
     * 申请备注
     */
    private String remark;
    
    /**
     * 申请时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createTime;
    
    /**
     * 处理时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date updateTime;
}
