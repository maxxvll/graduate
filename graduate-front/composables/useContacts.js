import { ref, computed } from 'vue'
import service from '@/utils/request'

/**
 * useContacts — 好友和群聊列表的状态管理与操作。
 *
 * 使用方式：
 *   const { friends, groups, selectedFriendTab, filteredFriends, filteredGroups,
 *           loadFriends, loadGroups, switchContactTab,
 *           chatWithFriend, chatWithGroup } = useContacts({
 *     sessions, currentSession, currentSidebarTab, CURRENT_USER_ID,
 *     switchSession, contactSearchText, SESSION_TYPE,
 *     onSwitchNotify,   // 切换到「通知」tab 时的回调
 *   })
 */
export function useContacts({
  sessions,
  currentSidebarTab,
  CURRENT_USER_ID,
  switchSession,
  contactSearchText,
  SESSION_TYPE,
  onSwitchNotify = () => {},
}) {
  const friends = ref([])
  const groups  = ref([])

  /** 通讯录当前激活 tab：'friends' | 'groups' | 'notify' */
  const selectedFriendTab = ref('friends')

  /** 好友列表实时过滤（响应搜索框输入） */
  const filteredFriends = computed(() => {
    if (!contactSearchText.value) return friends.value
    const q = contactSearchText.value.toLowerCase()
    return friends.value.filter((f) => f.nickname.toLowerCase().includes(q))
  })

  /** 群聊列表实时过滤（响应搜索框输入） */
  const filteredGroups = computed(() => {
    if (!contactSearchText.value) return groups.value
    const q = contactSearchText.value.toLowerCase()
    return groups.value.filter((g) => g.groupName.toLowerCase().includes(q))
  })

  /**
   * 拉取好友列表。
   * 将后端 FriendApplicationVO 映射为统一的前端好友对象格式。
   */
  const loadFriends = async () => {
    try {
      const res = await service.get('/friend/list')
      if (res.code === 200 && Array.isArray(res.data)) {
        friends.value = res.data.map((f) => ({
          userId:    String(f.applicantId),
          nickname:  f.applicantNickname || String(f.applicantId),
          avatar:    f.applicantAvatar || '',
          signature: f.remark || '',
          isFriend:  true,
        }))
      }
    } catch (e) {
      console.error('useContacts: 加载好友列表失败', e)
    }
  }

  /**
   * 拉取已加入的群聊列表。
   * 将后端 GroupInfoVO 映射为统一的前端群聊对象格式。
   */
  const loadGroups = async () => {
    try {
      const res = await service.get('/group/list')
      if (res.code === 200 && Array.isArray(res.data)) {
        groups.value = res.data.map((g) => ({
          groupId:     g.id,
          groupName:   g.groupName,
          groupAvatar: g.groupAvatar || '',
          memberCount: g.currentMemberCount || 0,
          creatorId:   g.creatorId,
          myRole:      g.myRole,
        }))
      }
    } catch (e) {
      console.error('useContacts: 加载群聊列表失败', e)
    }
  }

  /**
   * 切换通讯录 Tab 并按需刷新数据：
   *  - 切到「通知」时调用外部传入的 onSwitchNotify 回调刷新通知列表
   *  - 切到「好友」/「群聊」时各自刷新对应列表
   */
  const switchContactTab = (tab) => {
    selectedFriendTab.value = tab
    if (tab === 'notify')  onSwitchNotify()
    else if (tab === 'friends') loadFriends()
    else if (tab === 'groups')  loadGroups()
  }

  /**
   * 由好友列表点击「发消息」进入聊天。
   * 若本地会话列表中不存在对应会话，则创建临时会话，消息发送后后端自动持久化。
   * 单聊 sessionId 格式："{min(myId,friendId)}_{max(myId,friendId)}"（双向唯一）
   */
  const chatWithFriend = (friend) => {
    const myId = CURRENT_USER_ID.value || ''
    // 使用 BigInt 比较，避免 Snowflake ID（18-19位数字）超过 JS Number.MAX_SAFE_INTEGER 导致精度丢失
    // Number("1234567890123456789") 会变成 1234567890123456800，导致 sessionId 与后端不匹配
    const id1 = BigInt(myId || '0')
    const id2 = BigInt(friend.userId || '0')
    const sessionId = id1 < id2 ? `${myId}_${friend.userId}` : `${friend.userId}_${myId}`
    let session = sessions.value.find((s) => s.sessionId === sessionId)
    if (!session) {
      session = {
        sessionId,
        sessionName:        friend.nickname,
        sessionAvatar:      friend.avatar,
        targetType:         'person',
        targetId:           friend.userId,
        lastMessageContent: '',
        lastMessageTime:    new Date().toISOString(),
        unreadCount:        0,
        sessionType:        SESSION_TYPE.SINGLE,
      }
      sessions.value.unshift(session)
    }
    currentSidebarTab.value = 'chat'
    switchSession(session)
  }

  /**
   * 由群聊列表点击「发消息」进入聊天。
   * 群聊 sessionId 格式："group_{groupId}"
   */
  const chatWithGroup = (group) => {
    const sessionId = `group_${group.groupId}`
    let session = sessions.value.find((s) => s.sessionId === sessionId)
    if (!session) {
      session = {
        sessionId,
        sessionName:        group.groupName,
        sessionAvatar:      group.groupAvatar,
        targetType:         'group',
        targetId:           group.groupId,
        lastMessageContent: '',
        lastMessageTime:    new Date().toISOString(),
        unreadCount:        0,
        sessionType:        SESSION_TYPE.GROUP,
      }
      sessions.value.unshift(session)
    }
    currentSidebarTab.value = 'chat'
    switchSession(session)
  }

  return {
    friends,
    groups,
    selectedFriendTab,
    filteredFriends,
    filteredGroups,
    loadFriends,
    loadGroups,
    switchContactTab,
    chatWithFriend,
    chatWithGroup,
  }
}
