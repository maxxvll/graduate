package com.maxxvll.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.maxxvll.domain.Favorite;

/**
 * 收藏服务接口
 */
public interface FavoriteService extends IService<Favorite> {

    /**
     * 添加收藏
     */
    Favorite addFavorite(String userId, Long messageId, String content, String messageType,
                         String fileUrl, String senderId, String sessionId);

    /**
     * 取消收藏
     */
    boolean removeFavorite(Long favoriteId, String userId);

    /**
     * 获取收藏列表
     */
    Page<Favorite> getFavoriteList(String userId, int current, int size);

    /**
     * 搜索收藏
     */
    Page<Favorite> searchFavorites(String userId, String keyword, String messageType, int current, int size);

    /**
     * 检查是否已收藏
     */
    boolean isFavorited(String userId, Long messageId);
}