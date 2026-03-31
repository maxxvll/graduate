package com.maxxvll.controller;

import com.alibaba.fastjson2.JSON;
import com.maxxvll.common.BaseController;
import com.maxxvll.common.Result;
import com.maxxvll.common.constants.VoiceCallConstants;
import com.maxxvll.common.dto.VoiceCallDTO;
import com.maxxvll.common.vo.UserInfoVO;
import com.maxxvll.common.vo.VoiceCallConfigVO;
import com.maxxvll.config.VoiceCallProperties;
import com.maxxvll.component.NettyChannelManager;
import com.maxxvll.utils.UserContextUtil;
import io.netty.channel.Channel;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 语音通话控制器
 * 提供语音通话相关的REST接口，包括发起、接听、拒绝、挂断等操作
 * 通过WebSocket信令实现WebRTC语音通话的建立和管理
 */
@Slf4j
@RestController
@RequestMapping("/voice-call")
@Tag(name = "语音通话", description = "语音通话相关接口")
public class VoiceCallController extends BaseController {

    @Resource
    private NettyChannelManager nettyChannelManager;

    @Resource
    private VoiceCallProperties voiceCallProperties;

    /**
     * 获取语音通话配置
     */
    @GetMapping("/config")
    public Result<VoiceCallConfigVO> getCallConfig() {
        VoiceCallConfigVO configVO = new VoiceCallConfigVO();
        configVO.setSupportedTransports(voiceCallProperties.getSupportedTransports());
        configVO.setPushBaseUrl(voiceCallProperties.getPushBaseUrl());
        configVO.setPlayBaseUrl(voiceCallProperties.getPlayBaseUrl());
        configVO.setIceServers(voiceCallProperties.getIceServers().stream().map(item -> {
            VoiceCallConfigVO.IceServerVO serverVO = new VoiceCallConfigVO.IceServerVO();
            serverVO.setUrls(item.getUrls());
            serverVO.setUsername(item.getUsername());
            serverVO.setCredential(item.getCredential());
            return serverVO;
        }).toList());
        return success(configVO);
    }

    /**
     * 发起语音呼叫
     */
    @PostMapping("/call")
    public Result<Void> makeCall(@Valid @RequestBody VoiceCallDTO callDTO) {
        String currentUserId = getCurrentUserId();
        log.info("用户{}发起语音呼叫，目标用户{}", currentUserId, callDTO.getTargetId());

        // 检查被呼叫人是否在线
        Channel calleeChannel = nettyChannelManager.getChannel(callDTO.getTargetId());
        if (calleeChannel == null || !calleeChannel.isActive()) {
            return fail("对方不在线");
        }

        // 构造呼叫信令，注入主叫人信息
        callDTO.setCallType(VoiceCallConstants.CallType.CALL);
        callDTO.setFromId(currentUserId);
        enrichCallerInfo(callDTO);

        String messageJson = JSON.toJSONString(callDTO);
        calleeChannel.writeAndFlush(new TextWebSocketFrame(messageJson));

        log.info("语音呼叫信令已发送");
        return success("呼叫已发起");
    }

    /**
     * 接听语音呼叫
     */
    @PostMapping("/answer")
    public Result<Void> answerCall(@Valid @RequestBody VoiceCallDTO callDTO) {
        String currentUserId = getCurrentUserId();
        log.info("用户{}接听语音呼叫", currentUserId);

        Channel callerChannel = nettyChannelManager.getChannel(callDTO.getTargetId());
        if (callerChannel == null || !callerChannel.isActive()) {
            return fail("对方已离线");
        }

        callDTO.setCallType(VoiceCallConstants.CallType.ANSWER);
        String messageJson = JSON.toJSONString(callDTO);
        callerChannel.writeAndFlush(new TextWebSocketFrame(messageJson));

        log.info("语音接听信令已发送");
        return success("已接听");
    }

    /**
     * 拒绝语音呼叫
     */
    @PostMapping("/reject")
    public Result<Void> rejectCall(@Valid @RequestBody VoiceCallDTO callDTO) {
        String currentUserId = getCurrentUserId();
        log.info("用户{}拒绝语音呼叫", currentUserId);

        Channel callerChannel = nettyChannelManager.getChannel(callDTO.getTargetId());
        if (callerChannel == null || !callerChannel.isActive()) {
            return fail("对方已离线");
        }

        callDTO.setCallType(VoiceCallConstants.CallType.REJECT);
        String messageJson = JSON.toJSONString(callDTO);
        callerChannel.writeAndFlush(new TextWebSocketFrame(messageJson));

        log.info("语音拒绝信令已发送");
        return success("已拒绝");
    }

    /**
     * 挂断语音通话
     */
    @PostMapping("/hangup")
    public Result<Void> hangupCall(@Valid @RequestBody VoiceCallDTO callDTO) {
        String currentUserId = getCurrentUserId();
        log.info("用户{}挂断语音通话", currentUserId);

        Channel peerChannel = nettyChannelManager.getChannel(callDTO.getTargetId());
        if (peerChannel != null && peerChannel.isActive()) {
            callDTO.setCallType(VoiceCallConstants.CallType.HANGUP);
            String messageJson = JSON.toJSONString(callDTO);
            peerChannel.writeAndFlush(new TextWebSocketFrame(messageJson));
        }

        log.info("语音挂断信令已发送");
        return success("已挂断");
    }

    /**
     * 交换 WebRTC SDP 信令
     */
    @PostMapping("/sdp")
    public Result<Void> exchangeSDP(@Valid @RequestBody VoiceCallDTO callDTO) {
        String currentUserId = getCurrentUserId();

        Channel peerChannel = nettyChannelManager.getChannel(callDTO.getTargetId());
        if (peerChannel == null || !peerChannel.isActive()) {
            return fail("对方不在线");
        }

        callDTO.setCallType(VoiceCallConstants.CallType.SDP);
        String messageJson = JSON.toJSONString(callDTO);
        peerChannel.writeAndFlush(new TextWebSocketFrame(messageJson));
        log.info("SDP 信令已发送给用户{}", callDTO.getTargetId());

        return success();
    }

    /**
     * 交换 ICE candidate 信令
     */
    @PostMapping("/ice")
    public Result<Void> exchangeICE(@Valid @RequestBody VoiceCallDTO callDTO) {
        String currentUserId = getCurrentUserId();

        Channel peerChannel = nettyChannelManager.getChannel(callDTO.getTargetId());
        if (peerChannel == null || !peerChannel.isActive()) {
            return fail("对方不在线");
        }

        callDTO.setCallType(VoiceCallConstants.CallType.ICE);
        String messageJson = JSON.toJSONString(callDTO);
        peerChannel.writeAndFlush(new TextWebSocketFrame(messageJson));
        log.info("ICE 信令已发送给用户{}", callDTO.getTargetId());

        return success();
    }

    /**
     * 注入主叫人信息到 DTO
     */
    private void enrichCallerInfo(VoiceCallDTO callDTO) {
        try {
            UserInfoVO caller = UserContextUtil.getCurrentUser();
            if (caller != null) {
                callDTO.setFromNickname(caller.getNickname());
                callDTO.setFromAvatar(caller.getAvatar());
            }
        } catch (Exception e) {
            log.warn("获取主叫用户信息失败，跳过注入昵称/头像", e);
        }
    }
}
