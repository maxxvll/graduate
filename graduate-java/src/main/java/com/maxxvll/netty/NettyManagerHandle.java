package com.maxxvll.netty;

import cn.dev33.satoken.stp.StpUtil;
import com.maxxvll.component.NettyChannelManager;
import com.maxxvll.utils.StringTools;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.util.AttributeKey;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@ChannelHandler.Sharable
@Slf4j
public class NettyManagerHandle extends SimpleChannelInboundHandler<TextWebSocketFrame> {
    @Resource
    private NettyChannelManager nettyChannelManager;

    // 1. 定义 AttributeKey，用于将 userId 绑定到 Channel 上
    private static final AttributeKey<String> USER_ID_KEY = AttributeKey.valueOf("userId");

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        log.info("客户端连接建立：{}", ctx.channel().remoteAddress());
        super.channelActive(ctx);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, TextWebSocketFrame textWebSocketFrame) throws Exception {
        log.info("收到消息：{}", textWebSocketFrame.text());
        // 在这里处理收到的消息
        // 可以解析消息并做相应的处理
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {

        if (evt instanceof WebSocketServerProtocolHandler.HandshakeComplete) {
            String url = ((WebSocketServerProtocolHandler.HandshakeComplete) evt).requestUri();
            String token = getToken(url);
            String userId;

            if (StringTools.isEmpty(token)) {
                log.warn("连接失败：token为空，关闭连接");
                ctx.close();
                return;
            }

            try {
                // 验证 token 并获取 userId
                Object userIdObj = StpUtil.getLoginIdByToken(token);
                if (userIdObj == null) {
                    log.warn("连接失败：token无效（getLoginIdByToken返回null），关闭连接");
                    ctx.close();
                    return;
                }
                userId = userIdObj.toString();
            } catch (Exception e) {
                log.error("连接失败：token无效，关闭连接", e);
                ctx.close();
                return;
            }

            // 3. 核心操作：将 userId 绑定到 Channel 的属性中
            ctx.channel().attr(USER_ID_KEY).set(userId);
            // 同时绑定到管理器
            nettyChannelManager.bindChannel(userId, ctx.channel());
            log.info("WebSocket握手成功，userId={}，地址={}", userId, ctx.channel().remoteAddress());
        }

        super.userEventTriggered(ctx, evt);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        // 4. 断开连接时：从 Channel 属性中取出 userId
        String userId = ctx.channel().attr(USER_ID_KEY).get();

        if (!StringTools.isEmpty(userId)) {
            // 从管理器中移除
            nettyChannelManager.removeChannel(userId);
            log.info("客户端断开连接，userId={}，地址={}", userId, ctx.channel().remoteAddress());
        } else {
            log.info("客户端断开连接（未绑定userId），地址={}", ctx.channel().remoteAddress());
        }

        // 清理属性（可选，Netty会自动回收，但显式清理更安全）
        ctx.channel().attr(USER_ID_KEY).set(null);
        super.channelInactive(ctx);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.error("连接异常，地址={}", ctx.channel().remoteAddress(), cause);
        ctx.close(); // 异常时关闭连接，触发 channelInactive 自动清理
    }

    private String getToken(String url) {
        if (StringTools.isEmpty(url) || !url.contains("?")) {
            return null;
        }
        String[] queryParams = url.split("\\?");
        if (queryParams.length < 2) {
            return null;
        }
        // 简单解析 token 参数（假设格式为 ?token=xxx）
        String[] params = queryParams[1].split("&");
        for (String param : params) {
            String[] keyValue = param.split("=");
            if (keyValue.length == 2 && "token".equals(keyValue[0])) {
                return keyValue[1]; // 返回值，即使是空字符串
            }
        }
        return null;
    }
}