package com.maxxvll.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.maxxvll.domain.GroupApplication;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Collection;
import java.util.List;

/**
* @author 20570
* @description 针对表【group_application(群申请表)】的数据库操作Mapper
* @createDate 2026-02-19 12:02:21
* @Entity com.maxxvll.domain.GroupApplication
*/
public interface GroupApplicationMapper extends BaseMapper<GroupApplication> {

    @Select("""
            <script>
            SELECT applicant_id
            FROM group_application
            WHERE group_id = #{groupId}
              AND status = #{status}
              AND applicant_id IN
            <foreach collection='applicantIds' item='applicantId' open='(' separator=',' close=')'>
                #{applicantId}
            </foreach>
            </script>
            """)
    List<Long> selectApplicantIdsByGroupIdAndStatusAndApplicantIds(@Param("groupId") Long groupId,
                                                                   @Param("status") Integer status,
                                                                   @Param("applicantIds") Collection<Long> applicantIds);

    @Select("""
            <script>
            SELECT group_id
            FROM group_application
            WHERE applicant_id = #{applicantId}
              AND status = #{status}
              AND group_id IN
            <foreach collection='groupIds' item='groupId' open='(' separator=',' close=')'>
                #{groupId}
            </foreach>
            </script>
            """)
    List<Long> selectGroupIdsByApplicantAndStatusAndGroupIds(@Param("applicantId") Long applicantId,
                                                             @Param("status") Integer status,
                                                             @Param("groupIds") Collection<Long> groupIds);

    @Insert("""
            <script>
            INSERT INTO group_application
                (applicant_id, group_id, status, reject_reason, operator_id, created_at, updated_at)
            VALUES
            <foreach collection='applications' item='application' separator=','>
                (#{application.applicantId}, #{application.groupId}, #{application.status}, #{application.rejectReason},
                 #{application.operatorId}, #{application.createTime}, #{application.updateTime})
            </foreach>
            ON DUPLICATE KEY UPDATE
                status = VALUES(status),
                reject_reason = VALUES(reject_reason),
                operator_id = VALUES(operator_id),
                created_at = VALUES(created_at),
                updated_at = VALUES(updated_at)
            </script>
            """)
    int batchUpsertApplications(@Param("applications") List<GroupApplication> applications);
}




