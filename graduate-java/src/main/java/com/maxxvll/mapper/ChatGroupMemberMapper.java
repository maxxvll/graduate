package com.maxxvll.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.maxxvll.domain.ChatGroupMember;
import lombok.Getter;
import lombok.Setter;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.MapKey;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface ChatGroupMemberMapper extends BaseMapper<ChatGroupMember> {

    @Select("""
            <script>
            SELECT group_id AS groupId, COUNT(*) AS memberCount
            FROM chat_group_member
            WHERE group_id IN
            <foreach collection='groupIds' item='id' open='(' separator=',' close=')'>
                #{id}
            </foreach>
              AND is_quit = 0
            GROUP BY group_id
            </script>
            """)
    List<GroupMemberCount> getMemberCountsByGroupIds(@Param("groupIds") List<Long> groupIds);

    @Getter
    @Setter
    public static class GroupMemberCount {
        public Long groupId;
        public Long memberCount;
    }

    @Getter
    @Setter
    public static class UserGroupRole {
        public Long groupId;
        public Integer role;
    }

    @Select("""
            <script>
            SELECT group_id AS groupId, role
            FROM chat_group_member
            WHERE user_id = #{userId}
              AND group_id IN
            <foreach collection='groupIds' item='id' open='(' separator=',' close=')'>
                #{id}
            </foreach>
              AND is_quit = 0
            </script>
            """)
    List<UserGroupRole> getUserRolesInGroups(@Param("userId") String userId, @Param("groupIds") List<Long> groupIds);

    @Select("SELECT user_id FROM chat_group_member WHERE group_id = #{groupId} AND is_quit = 0")
    List<String> selectActiveUserIdsByGroupId(@Param("groupId") Long groupId);

    @Select("""
            <script>
            SELECT user_id
            FROM chat_group_member
            WHERE group_id = #{groupId}
              AND is_quit = 0
              AND user_id IN
            <foreach collection='userIds' item='userId' open='(' separator=',' close=')'>
                #{userId}
            </foreach>
            </script>
            """)
    List<String> selectActiveUserIdsByGroupIdAndUserIds(@Param("groupId") Long groupId,
                                                        @Param("userIds") Collection<String> userIds);

    @Select("SELECT group_id FROM chat_group_member WHERE user_id = #{userId} AND is_quit = 0")
    List<Long> selectActiveGroupIdsByUserId(@Param("userId") String userId);

    @Select("""
            <script>
            SELECT group_id, user_id
            FROM chat_group_member
            WHERE group_id IN
            <foreach collection='groupIds' item='id' open='(' separator=',' close=')'>
                #{id}
            </foreach>
              AND is_quit = 0
            </script>
            """)
    List<ChatGroupMember> selectActiveMembersByGroupIds(@Param("groupIds") Collection<Long> groupIds);

    @Insert("""
            <script>
            INSERT INTO chat_group_member
                (group_id, user_id, role, join_time, inviter_id, is_mute, is_quit, quit_time, quit_reason, created_at, updated_at)
            VALUES
            <foreach collection='members' item='member' separator=','>
                (#{member.groupId}, #{member.userId}, #{member.role}, #{member.joinTime}, #{member.inviterId},
                 #{member.isMute}, #{member.isQuit}, #{member.quitTime}, #{member.quitReason},
                 #{member.createTime}, #{member.updateTime})
            </foreach>
            ON DUPLICATE KEY UPDATE
                role = VALUES(role),
                join_time = VALUES(join_time),
                inviter_id = VALUES(inviter_id),
                is_mute = VALUES(is_mute),
                is_quit = VALUES(is_quit),
                quit_time = VALUES(quit_time),
                quit_reason = VALUES(quit_reason),
                updated_at = VALUES(updated_at)
            </script>
            """)
    int batchUpsertMembers(@Param("members") List<ChatGroupMember> members);
}
