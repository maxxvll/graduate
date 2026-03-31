package com.maxxvll.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.maxxvll.common.BaseController;
import com.maxxvll.common.Result;
import com.maxxvll.domain.Favorite;
import com.maxxvll.service.FavoriteService;
import com.maxxvll.utils.UserContextUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 收藏控制器
 */
@Slf4j
@RestController
@RequestMapping("/favorite")
@Validated
@RequiredArgsConstructor
@Tag(name = "收藏", description = "消息收藏相关接口")
public class FavoriteController extends BaseController {

    private final FavoriteService favoriteService;

    /**
     * 添加收藏
     */
    @PostMapping("/add")
    public Result<Favorite> addFavorite(
            @RequestParam Long messageId,
            @RequestParam(required = false) String content,
            @RequestParam(required = false) String messageType,
            @RequestParam(required = false) String fileUrl,
            @RequestParam(required = false) String senderId,
            @RequestParam(required = false) String sessionId) {

        String userId = UserContextUtil.getCurrentUserId();
        Favorite favorite = favoriteService.addFavorite(
                userId, messageId, content, messageType, fileUrl, senderId, sessionId);
        return Result.success("收藏成功", favorite);
    }

    /**
     * 取消收藏
     */
    @DeleteMapping("/{id}")
    public Result<Boolean> removeFavorite(@PathVariable Long id) {
        String userId = UserContextUtil.getCurrentUserId();
        boolean success = favoriteService.removeFavorite(id, userId);
        return Result.success("取消收藏成功", success);
    }

    /**
     * 获取收藏列表
     */
    @GetMapping("/list")
    public Result<Page<Favorite>> getFavoriteList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {

        String userId = UserContextUtil.getCurrentUserId();
        Page<Favorite> favoritePage = favoriteService.getFavoriteList(userId, page, size);
        return Result.success("获取成功", favoritePage);
    }

    /**
     * 搜索收藏
     */
    @GetMapping("/search")
    public Result<Page<Favorite>> searchFavorites(
            @RequestParam String keyword,
            @RequestParam(required = false) String messageType,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {

        String userId = UserContextUtil.getCurrentUserId();
        Page<Favorite> favoritePage = favoriteService.searchFavorites(userId, keyword, messageType, page, size);
        return Result.success("搜索成功", favoritePage);
    }

    /**
     * 检查是否已收藏
     */
    @GetMapping("/check")
    public Result<Boolean> checkFavorite(@RequestParam Long messageId) {
        String userId = UserContextUtil.getCurrentUserId();
        boolean isFavorited = favoriteService.isFavorited(userId, messageId);
        return Result.success(isFavorited);
    }
}
