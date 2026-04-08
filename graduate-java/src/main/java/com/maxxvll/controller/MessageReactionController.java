package com.maxxvll.controller;

import com.maxxvll.common.BaseController;
import com.maxxvll.common.Result;
import com.maxxvll.common.vo.MessageReactionVO;
import com.maxxvll.service.MessageReactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 消息反应控制器
 *
 * @author 20570
 */
@Slf4j
@RestController
@RequestMapping("/chat/reaction")
@Tag(name = "消息反应", description = "消息表情反应相关接口")
public class MessageReactionController extends BaseController {

    @jakarta.annotation.Resource
    private MessageReactionService messageReactionService;

    /**
     * 添加表情反应
     */
    @PostMapping("/add")
    @Operation(summary = "添加表情反应", description = "对消息添加表情反应")
    public Result<List<MessageReactionVO>> addReaction(@Valid @RequestBody AddReactionRequest request) {
        Long userId = Long.valueOf(getCurrentUserId());
        messageReactionService.addReaction(request.getMessageId(), userId, request.getEmoji());

        // 返回更新后的反应列表
        List<MessageReactionVO> reactions = messageReactionService.getReactionsByMessageId(request.getMessageId());
        return success(reactions);
    }

    /**
     * 移除表情反应
     */
    @PostMapping("/remove")
    @Operation(summary = "移除表情反应", description = "移除对消息的表情反应")
    public Result<Void> removeReaction(@Valid @RequestBody RemoveReactionRequest request) {
        Long userId = Long.valueOf(getCurrentUserId());
        messageReactionService.removeReaction(request.getMessageId(), userId, request.getEmoji());
        return success("反应已移除");
    }

    /**
     * 获取消息反应列表
     */
    @GetMapping("/list")
    @Operation(summary = "获取消息反应列表", description = "获取指定消息的所有表情反应")
    public Result<List<MessageReactionVO>> getReactions(@RequestParam Long messageId) {
        List<MessageReactionVO> reactions = messageReactionService.getReactionsByMessageId(messageId);
        return success(reactions);
    }

    /**
     * 添加反应请求
     */
    public static class AddReactionRequest {
        private Long messageId;
        @NotBlank(message = "表情不能为空")
        private String emoji;

        public Long getMessageId() {
            return messageId;
        }

        public void setMessageId(Long messageId) {
            this.messageId = messageId;
        }

        public String getEmoji() {
            return emoji;
        }

        public void setEmoji(String emoji) {
            this.emoji = emoji;
        }
    }

    /**
     * 移除反应请求
     */
    public static class RemoveReactionRequest {
        private Long messageId;
        @NotBlank(message = "表情不能为空")
        private String emoji;

        public Long getMessageId() {
            return messageId;
        }

        public void setMessageId(Long messageId) {
            this.messageId = messageId;
        }

        public String getEmoji() {
            return emoji;
        }

        public void setEmoji(String emoji) {
            this.emoji = emoji;
        }
    }
}
