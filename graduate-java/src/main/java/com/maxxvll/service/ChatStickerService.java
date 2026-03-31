package com.maxxvll.service;

import com.maxxvll.domain.ChatSticker;
import com.baomidou.mybatisplus.extension.service.IService;
import java.util.List;

/**
 * 表情包服务接口
 */
public interface ChatStickerService extends IService<ChatSticker> {

    /**
     * 添加收藏表情
     * @param userId 用户ID
     * @param url 表情图片URL
     * @param name 表情名称
     * @param category 分类
     * @return 创建的表情
     */
    ChatSticker addSticker(Long userId, String url, String name, String category);

    /**
     * 获取用户收藏的表情列表
     * @param userId 用户ID
     * @return 表情列表
     */
    List<ChatSticker> getUserStickers(Long userId);

    /**
     * 删除收藏的表情
     * @param stickerId 表情ID
     * @param userId 用户ID（校验权限）
     */
    void deleteSticker(Long stickerId, Long userId);

    /**
     * 更新表情名称
     * @param stickerId 表情ID
     * @param userId 用户ID
     * @param name 新名称
     */
    void renameSticker(Long stickerId, Long userId, String name);
}