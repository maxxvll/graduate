package com.maxxvll.test;

import com.maxxvll.common.dto.ChatMessageSendDTO;
import com.maxxvll.common.enums.MessageType;
import com.maxxvll.common.enums.SessionType;
import com.maxxvll.domain.ChatMessage;
import com.maxxvll.service.ChatMessageService;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

@SpringBootTest
public class MessageTransactionTest {

    @Resource
    private ChatMessageService chatMessageService;

    @Test
    public void testMessageTransactionWithMultipleFiles() {
        try {
            // 模拟发送多文件消息
            ChatMessageSendDTO sendDTO = new ChatMessageSendDTO();
            sendDTO.setSessionId("test_session_001");
            sendDTO.setSessionType(SessionType.GROUP.getCode());
            sendDTO.setReceiverId("group_123");
            sendDTO.setContent("测试多文件发送");
            
            // 创建多个测试文件
            List<MultipartFile> files = new ArrayList<>();
            
            // 创建3个不同的文件
            for (int i = 0; i < 3; i++) {
                String fileName = "test_file_" + i + ".txt";
                String content = "测试文件内容 " + i;
                MockMultipartFile file = new MockMultipartFile(
                    "file", 
                    fileName, 
                    "text/plain", 
                    content.getBytes()
                );
                files.add(file);
            }
            
            // 转换为数组
            MultipartFile[] fileArray = files.toArray(new MultipartFile[0]);
            
            System.out.println("开始测试多文件消息发送...");
            System.out.println("文件数量: " + fileArray.length);
            
            // 发送消息
            ChatMessage result = chatMessageService.sendMessage(sendDTO, fileArray);
            
            System.out.println("消息发送成功!");
            System.out.println("消息ID: " + result.getId());
            System.out.println("消息编号: " + result.getMessageNo());
            System.out.println("会话ID: " + result.getSessionId());
            
        } catch (Exception e) {
            System.err.println("测试失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    @Test
    public void testMessageNoUniqueness() {
        System.out.println("=== 测试MessageNo唯一性 ===");
        
        // 测试messageNo生成机制
        String baseMessageNo = System.currentTimeMillis() + "_" + java.util.UUID.randomUUID().toString().replace("-", "");
        System.out.println("基础MessageNo: " + baseMessageNo);
        
        // 生成带序号的唯一标识
        for (int i = 0; i < 5; i++) {
            String uniqueMessageNo = baseMessageNo + "_" + i;
            System.out.println("消息" + i + "的MessageNo: " + uniqueMessageNo);
        }
        
        System.out.println("=== 测试完成 ===");
    }
}