package com.maxxvll.component;

import com.maxxvll.service.ChatMessageService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ChatMessageRetentionScheduler {

    @Resource
    private ChatMessageService chatMessageService;

    @Scheduled(cron = "${app.chat.retention-cleanup-cron:0 10 4 * * ?}")
    public void purgeExpiredMessages() {
        int purgedCount = chatMessageService.purgeExpiredMessages();
        if (purgedCount > 0) {
            log.info("Chat retention cleanup finished, purgedCount={}", purgedCount);
        }
    }
}
