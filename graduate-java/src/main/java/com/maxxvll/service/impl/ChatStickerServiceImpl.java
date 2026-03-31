package com.maxxvll.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.maxxvll.common.exception.BusinessException;
import com.maxxvll.domain.ChatSticker;
import com.maxxvll.mapper.ChatStickerMapper;
import com.maxxvll.service.ChatStickerService;
import com.maxxvll.utils.MinioUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

/**
 * 表情包服务实现
 */
@Service
@Slf4j
public class ChatStickerServiceImpl extends ServiceImpl<ChatStickerMapper, ChatSticker>
        implements ChatStickerService {

    @Resource
    private MinioUtil minioUtil;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ChatSticker addSticker(Long userId, String url, String name, String category) {
        if (userId == null) {
            throw new BusinessException("用户ID不能为空");
        }
        if (url == null || url.trim().isEmpty()) {
            throw new BusinessException("表情图片URL不能为空");
        }

        ChatSticker sticker = new ChatSticker();
        sticker.setUserId(userId);
        sticker.setUrl(url.trim());
        sticker.setName(name != null && !name.trim().isEmpty() ? name.trim() : "表情");
        sticker.setCategory(category != null ? category : "custom");
        sticker.setCreateTime(new Date());

        this.save(sticker);
        log.info("用户[{}]添加了表情[{}], URL: {}", userId, name, url);

        return sticker;
    }

    @Override
    public List<ChatSticker> getUserStickers(Long userId) {
        if (userId == null) {
            throw new BusinessException("用户ID不能为空");
        }

        List<ChatSticker> stickers = list(new LambdaQueryWrapper<ChatSticker>()
                .eq(ChatSticker::getUserId, userId)
                .orderByDesc(ChatSticker::getCreateTime));

        // 补充完整的表情 URL
        for (ChatSticker sticker : stickers) {
            if (sticker.getUrl() != null && !sticker.getUrl().startsWith("http")) {
                String fullUrl = minioUtil.getChatFileUrl(sticker.getUrl(), true);
                sticker.setUrl(fullUrl);
            }
        }

        return stickers;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteSticker(Long stickerId, Long userId) {
        if (stickerId == null) {
            throw new BusinessException("表情ID不能为空");
        }
        if (userId == null) {
            throw new BusinessException("用户ID不能为空");
        }

        ChatSticker sticker = this.getById(stickerId);
        if (sticker == null) {
            throw new BusinessException("表情不存在");
        }

        // 校验权限：只能删除自己的表情
        if (!sticker.getUserId().equals(userId)) {
            throw new BusinessException("无权删除该表情");
        }

        // 删除 MinIO 中的表情文件
        try {
            if (sticker.getUrl() != null && !sticker.getUrl().startsWith("http")) {
                minioUtil.removeObject(sticker.getUrl());
                log.info("删除 MinIO 中的表情文件: {}", sticker.getUrl());
            }
        } catch (Exception e) {
            log.warn("删除 MinIO 表情文件失败: {}", e.getMessage());
        }

        this.removeById(stickerId);
        log.info("用户[{}]删除了表情[{}]", userId, stickerId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void renameSticker(Long stickerId, Long userId, String name) {
        if (stickerId == null) {
            throw new BusinessException("表情ID不能为空");
        }
        if (userId == null) {
            throw new BusinessException("用户ID不能为空");
        }
        if (name == null || name.trim().isEmpty()) {
            throw new BusinessException("表情名称不能为空");
        }

        ChatSticker sticker = this.getById(stickerId);
        if (sticker == null) {
            throw new BusinessException("表情不存在");
        }

        // 校验权限：只能修改自己的表情
        if (!sticker.getUserId().equals(userId)) {
            throw new BusinessException("无权修改该表情");
        }

        sticker.setName(name.trim());
        this.updateById(sticker);
        log.info("用户[{}]重命名了表情[{}]为[{}]", userId, stickerId, name);
    }
}