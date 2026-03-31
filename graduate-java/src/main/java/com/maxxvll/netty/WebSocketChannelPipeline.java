package com.maxxvll.netty;

import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.timeout.IdleStateHandler;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * WebSocket ChannelPipeline 构建器
 * 负责配置 Netty 的 ChannelPipeline
 *
 * @author backend-friend
 */
@Slf4j
@Component
public class WebSocketChannelPipeline {

    @Resource
    private MessageAckHandler messageAckHandler;

    @Resource
    private NettyManagerHandle nettyManagerHandle;

    @Resource
    private WebSocketAuthInterceptor authInterceptor;

    /**
     * 心跳配置
     */
    private int readerIdleTime = 90;
    private int writerIdleTime = 0;
    private int allIdleTime = 30;

    /**
     * WebSocket 最大帧大小
     */
    private int maxFrameSize = 64 * 1024;

    /**
     * HTTP 聚合最大大小
     */
    private int maxContentLength = 1024 * 1024;

    public void setHeartbeatConfig(int readerIdleTime, int writerIdleTime, int allIdleTime) {
        this.readerIdleTime = readerIdleTime;
        this.writerIdleTime = writerIdleTime;
        this.allIdleTime = allIdleTime;
    }

    /**
     * 创建 ChannelInitializer
     */
    public ChannelInitializer<Channel> createChannelInitializer() {
        return new ChannelInitializer<>() {
            @Override
            protected void initChannel(Channel channel) {
                configurePipeline(channel.pipeline());
            }
        };
    }

    /**
     * 配置 ChannelPipeline
     */
    protected void configurePipeline(ChannelPipeline pipeline) {
        // 基础 HTTP 编解码器
        pipeline.addLast("httpCodec", new HttpServerCodec());
        pipeline.addLast("httpAggregator", new HttpObjectAggregator(maxContentLength));

        // 心跳处理器
        if (readerIdleTime > 0 || writerIdleTime > 0 || allIdleTime > 0) {
            pipeline.addLast("idleStateHandler", new IdleStateHandler(readerIdleTime, writerIdleTime, allIdleTime));
        }

        // WebSocket 协议处理器
        pipeline.addLast("webSocketProtocol", new WebSocketServerProtocolHandler(
                "/ws", null, true, maxFrameSize, true, true, 10000L));

        // 自定义处理器（按顺序添加）
        addCustomHandlers(pipeline);
    }

    /**
     * 添加自定义处理器
     * 子类可以重写此方法来添加或修改处理器
     */
    protected void addCustomHandlers(ChannelPipeline pipeline) {
        // 认证拦截器
        pipeline.addLast("authInterceptor", authInterceptor);

        // 消息ACK处理器
        pipeline.addLast("messageAckHandler", messageAckHandler);

        // 消息处理器
        pipeline.addLast("nettyManagerHandle", nettyManagerHandle);

        log.debug("WebSocket ChannelPipeline configured, handlers={}", getHandlerNames(pipeline));
    }

    /**
     * 获取 Pipeline 中所有处理器的名称
     */
    protected List<String> getHandlerNames(ChannelPipeline pipeline) {
        List<String> names = new ArrayList<>();
        for (String name : pipeline.names()) {
            names.add(name);
        }
        return names;
    }

    /**
     * 创建带有额外配置的 ChannelInitializer
     *
     * @param extraHandlers 额外的处理器
     * @return ChannelInitializer
     */
    public ChannelInitializer<Channel> createChannelInitializer(java.util.function.Consumer<ChannelPipeline> customizer) {
        return new ChannelInitializer<>() {
            @Override
            protected void initChannel(Channel channel) {
                configurePipeline(channel.pipeline());
                if (customizer != null) {
                    customizer.accept(channel.pipeline());
                }
            }
        };
    }
}
