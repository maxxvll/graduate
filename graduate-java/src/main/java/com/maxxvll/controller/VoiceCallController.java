package com.maxxvll.controller;

import com.maxxvll.common.Result;
import com.maxxvll.common.dto.VoiceCallDTO;
import com.maxxvll.common.BaseController;
import com.maxxvll.component.NettyChannelManager;
import com.maxxvll.utils.UserContextUtil;
import io.netty.channel.Channel;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import com.alibaba.fastjson.JSON;

/**
 * 语音通话控制器
 */
@Slf4j
@RestController
@RequestMapping("/voice-call")
public class VoiceCallController extends BaseController {

    @Resource
    private NettyChannelManager nettyChannelManager;

    /**
     * 发起语音呼叫
     */
    @PostMapping("/call")
    public Result<Void> makeCall(@RequestBody VoiceCallDTO callDTO) {
        String currentUserId = UserContextUtil.getCurrentUserId();
        log.info("用户{}发起语音呼叫，目标用户{}", currentUserId, callDTO.getTargetId());

        // 1. 检查被呼叫人是否在线
        Channel calleeChannel = nettyChannelManager.getChannel(callDTO.getTargetId());
        if (calleeChannel == null || !calleeChannel.isActive()) {
            return Result.fail("对方不在线");
        }

        // 2. 构造呼叫信令
        callDTO.setCallType("1"); // 1-发起呼叫
        String messageJson = JSON.toJSONString(callDTO);

        // 3. 发送给被呼叫人
        calleeChannel.writeAndFlush(new TextWebSocketFrame(messageJson));

        log.info("语音呼叫信令已发送");
        return Result.success("呼叫已发起");
    }

    /**
     * 接听语音呼叫
     */
    @PostMapping("/answer")
    public Result<Void> answerCall(@RequestBody VoiceCallDTO callDTO) {
        String currentUserId = UserContextUtil.getCurrentUserId();
        log.info("用户{}接听语音呼叫", currentUserId);

        // 参数校验
        if (callDTO.getTargetId() == null || callDTO.getTargetId().trim().isEmpty()) {
            return Result.fail("目标用户 ID 不能为空");
        }

        // 1. 检查主叫人是否在线
        Channel callerChannel = nettyChannelManager.getChannel(callDTO.getTargetId());
        if (callerChannel == null || !callerChannel.isActive()) {
            return Result.fail("对方已离线");
        }

        // 2. 构造接听信令
        callDTO.setCallType("2"); // 2-接听
        String messageJson = JSON.toJSONString(callDTO);

        // 3. 发送给主叫人
        callerChannel.writeAndFlush(new TextWebSocketFrame(messageJson));

        log.info("语音接听信令已发送");
        return Result.success("已接听");
    }

    /**
     * 拒绝语音呼叫
     */
    @PostMapping("/reject")
    public Result<Void> rejectCall(@RequestBody VoiceCallDTO callDTO) {
        String currentUserId = UserContextUtil.getCurrentUserId();
        log.info("用户{}拒绝语音呼叫", currentUserId);

        // 参数校验
        if (callDTO.getTargetId() == null || callDTO.getTargetId().trim().isEmpty()) {
            return Result.fail("目标用户 ID 不能为空");
        }

        // 1. 检查主叫人是否在线
        Channel callerChannel = nettyChannelManager.getChannel(callDTO.getTargetId());
        if (callerChannel == null || !callerChannel.isActive()) {
            return Result.fail("对方已离线");
        }

        // 2. 构造拒绝信令
        callDTO.setCallType("3"); // 3-拒绝
        String messageJson = JSON.toJSONString(callDTO);

        // 3. 发送给主叫人
        callerChannel.writeAndFlush(new TextWebSocketFrame(messageJson));

        log.info("语音拒绝信令已发送");
        return Result.success("已拒绝");
    }

    /**
     * 挂断语音通话
     */
    @PostMapping("/hangup")
    public Result<Void> hangupCall(@RequestBody VoiceCallDTO callDTO) {
        String currentUserId = UserContextUtil.getCurrentUserId();
        log.info("用户{}挂断语音通话", currentUserId);

        // 参数校验
        if (callDTO.getTargetId() == null || callDTO.getTargetId().trim().isEmpty()) {
            return Result.fail("目标用户 ID 不能为空");
        }

        // 1. 通知对方
        Channel peerChannel = nettyChannelManager.getChannel(callDTO.getTargetId());
        if (peerChannel != null && peerChannel.isActive()) {
            // 2. 构造挂断信令
            callDTO.setCallType("4"); // 4-挂断
            String messageJson = JSON.toJSONString(callDTO);
            peerChannel.writeAndFlush(new TextWebSocketFrame(messageJson));
        }

        log.info("语音挂断信令已发送");
        return Result.success("已挂断");
    }

    /**
     * 交换 WebRTC SDP 信令
     */
    @PostMapping("/sdp")
    public Result<Void> exchangeSDP(@RequestBody VoiceCallDTO callDTO) {
        String currentUserId = UserContextUtil.getCurrentUserId();
        
        // 参数校验
        if (callDTO.getTargetId() == null || callDTO.getTargetId().trim().isEmpty()) {
            return Result.fail("目标用户 ID 不能为空");
        }
        if (callDTO.getSdp() == null) {
            return Result.fail("SDP 数据不能为空");
        }
        
        // 1. 发送给对方
        Channel peerChannel = nettyChannelManager.getChannel(callDTO.getTargetId());
        if (peerChannel != null && peerChannel.isActive()) {
            callDTO.setCallType("5"); // 5-SDP 交换
            String messageJson = JSON.toJSONString(callDTO);
            peerChannel.writeAndFlush(new TextWebSocketFrame(messageJson));
            log.info("SDP 信令已发送给用户{}", callDTO.getTargetId());
        } else {
            log.warn("用户{}不在线，无法发送 SDP 信令", callDTO.getTargetId());
            return Result.fail("对方不在线");
        }

        return Result.success();
    }

    /**
     * 交换 ICE candidate 信令
     */
    @PostMapping("/ice")
    public Result<Void> exchangeICE(@RequestBody VoiceCallDTO callDTO) {
        String currentUserId = UserContextUtil.getCurrentUserId();
        
        // 参数校验
        if (callDTO.getTargetId() == null || callDTO.getTargetId().trim().isEmpty()) {
            return Result.fail("目标用户 ID 不能为空");
        }
        if (callDTO.getCandidate() == null) {
            return Result.fail("ICE candidate 数据不能为空");
        }
        
        // 1. 发送给对方
        Channel peerChannel = nettyChannelManager.getChannel(callDTO.getTargetId());
        if (peerChannel != null && peerChannel.isActive()) {
            callDTO.setCallType("6"); // 6-ICE 交换
            String messageJson = JSON.toJSONString(callDTO);
            peerChannel.writeAndFlush(new TextWebSocketFrame(messageJson));
            log.info("ICE 信令已发送给用户{}", callDTO.getTargetId());
        } else {
            log.warn("用户{}不在线，无法发送 ICE 信令", callDTO.getTargetId());
            return Result.fail("对方不在线");
        }

        return Result.success();
    }
}
