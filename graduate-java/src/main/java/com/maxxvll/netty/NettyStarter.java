package com.maxxvll.netty;

import cn.dev33.satoken.context.SaHolder;
import cn.dev33.satoken.stp.StpUtil;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LoggingHandler;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;

/**
 * Netty WebSocket 服务器启动器
 * 负责启动和管理 Netty WebSocket 服务器
 *
 * @author backend-friend
 */
@Slf4j
@Component
public class NettyStarter implements SmartInitializingSingleton {

    @Resource
    private NettyManagerHandle nettyManagerHandle;

    @Resource
    private MessageAckHandler messageAckHandler;

    @Resource
    private WebSocketChannelPipeline channelPipeline;

    @Value("${ws.port:5051}")
    private int wsPort;

    @Value("${ws.host:0.0.0.0}")
    private String wsHost;

    @Value("${ws.heartbeat.reader-idle-time:90}")
    private int readerIdleTime;

    @Value("${ws.heartbeat.writer-idle-time:0}")
    private int writerIdleTime;

    @Value("${ws.heartbeat.all-idle-time:30}")
    private int allIdleTime;

    @Value("${netty.boss-threads:1}")
    private int bossThreads;

    @Value("${netty.worker-threads:0}")
    private int workerThreads;

    @Value("${netty.shutdown.timeout:30}")
    private long shutdownTimeoutSeconds;

    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private Channel serverChannel;
    private int resolvedWorkerThreads;

    @PostConstruct
    public void validateConfiguration() {
        if (wsPort < 1024 || wsPort > 65535) {
            throw new IllegalArgumentException("Invalid WebSocket port: " + wsPort);
        }
        if (bossThreads <= 0) {
            throw new IllegalArgumentException("netty.boss-threads must be greater than 0");
        }

        int processors = Runtime.getRuntime().availableProcessors();
        resolvedWorkerThreads = workerThreads > 0 ? workerThreads : Math.max(4, processors * 2);

        if (resolvedWorkerThreads > processors * 8) {
            log.warn("Configured worker threads look too high, configured={}, cpu={}", resolvedWorkerThreads, processors);
        }

        // 配置心跳参数到 ChannelPipeline
        channelPipeline.setHeartbeatConfig(readerIdleTime, writerIdleTime, allIdleTime);

        log.info("Netty configuration ready, host={}, port={}, bossThreads={}, workerThreads={}, cpu={}",
                wsHost, wsPort, bossThreads, resolvedWorkerThreads, processors);
    }

    /**
     * 等待 Sa-Token 初始化完成
     */
    private void waitForSaTokenInit() {
        int maxWaitSeconds = 60;
        int waitedSeconds = 0;
        while (waitedSeconds < maxWaitSeconds) {
            try {
                // 检查 StpLogic 是否已设置
                if (StpUtil.getStpLogic() != null) {
                    // 尝试访问 SaHolder 来检查上下文是否可用
                    SaHolder.getContext();
                    log.info("Sa-Token context initialized after {} seconds", waitedSeconds);
                    return;
                }
            } catch (Exception e) {
                // Sa-Token 尚未初始化，等待
            }

            try {
                TimeUnit.SECONDS.sleep(1);
                waitedSeconds++;
                if (waitedSeconds % 5 == 0) {
                    log.debug("Waiting for Sa-Token init... {}s", waitedSeconds);
                }
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        log.warn("Waited {} seconds for Sa-Token init, proceeding anyway", maxWaitSeconds);
    }

    @PreDestroy
    public void shutdown() {
        log.info("Shutting down Netty WebSocket server...");

        // 关闭服务器 Channel
        if (serverChannel != null) {
            try {
                serverChannel.close().syncUninterruptibly();
                log.info("Netty server channel closed");
            } catch (Exception e) {
                log.error("Failed to close server channel", e);
            }
        }

        // 优雅关闭 EventLoopGroup
        if (workerGroup != null) {
            try {
                workerGroup.shutdownGracefully();
                log.info("Worker group shutdown initiated");
            } catch (Exception e) {
                log.error("Failed to shutdown worker group", e);
            }
        }

        if (bossGroup != null) {
            try {
                bossGroup.shutdownGracefully();
                log.info("Boss group shutdown initiated");
            } catch (Exception e) {
                log.error("Failed to shutdown boss group", e);
            }
        }

        log.info("Netty WebSocket server stopped");
    }

    @Override
    public void afterSingletonsInstantiated() {
        // 确保所有单例 Bean 完全初始化后再启动 Netty
        log.info("All singletons instantiated, starting Netty WebSocket server...");
        try {
            start();
        } catch (Exception e) {
            log.error("Failed to start Netty server", e);
            throw new RuntimeException("Failed to start Netty server", e);
        }
    }

    /**
     * 启动 Netty 服务器
     */
    private void start() throws Exception {
        // 等待 Sa-Token 初始化完成
        log.info("Waiting for Sa-Token initialization...");
        waitForSaTokenInit();

        // 初始化 EventLoopGroup
        bossGroup = new NioEventLoopGroup(bossThreads);
        workerGroup = new NioEventLoopGroup(resolvedWorkerThreads);

        log.info("Netty event loops initialized, bossThreads={}, workerThreads={}",
                bossThreads, resolvedWorkerThreads);

        // 配置 ServerBootstrap
        ServerBootstrap bootstrap = new ServerBootstrap();
        bootstrap.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .handler(new LoggingHandler())
                .childHandler(createChannelInitializer());

        // 配置 TCP 参数
        configureSocketOptions(bootstrap);

        // 绑定端口并启动
        InetSocketAddress address = new InetSocketAddress(wsHost, wsPort);
        ChannelFuture future = bootstrap.bind(address).sync();
        serverChannel = future.channel();

        log.info("Netty WebSocket server started on {}:{}", wsHost, wsPort);
        log.info("WebSocket endpoint: ws://{}:{}/ws?token=<token>",
                "0.0.0.0".equals(wsHost) ? "localhost" : wsHost,
                wsPort);
    }

    /**
     * 创建 ChannelInitializer
     */
    private ChannelInitializer<Channel> createChannelInitializer() {
        return channelPipeline.createChannelInitializer();
    }

    /**
     * 配置 TCP 参数
     */
    private void configureSocketOptions(ServerBootstrap bootstrap) {
        bootstrap.option(ChannelOption.SO_BACKLOG, 1024)
                .option(ChannelOption.SO_REUSEADDR, true)
                .childOption(ChannelOption.TCP_NODELAY, true)
                .childOption(ChannelOption.SO_KEEPALIVE, true)
                .childOption(ChannelOption.SO_RCVBUF, 32 * 1024)
                .childOption(ChannelOption.SO_SNDBUF, 32 * 1024);
    }
}
