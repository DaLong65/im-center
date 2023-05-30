package com.im.core.trigger;

import com.im.core.coder.AppDecoder;
import com.im.core.coder.AppEncoder;
import com.im.core.coder.WebDecoder;
import com.im.core.coder.WebEncoder;
import com.im.core.constant.ChannelPipelineHandlerConstants;
import com.im.core.constant.ProtocolConstants;
import com.im.core.handler.AuthHandler;
import com.im.core.handler.BusinessProcessor;
import com.im.core.handler.IdleCheckHandler;
import com.im.core.handler.MetricsHandler;
import com.im.core.protocol.MessageBody;
import com.im.core.protocol.RequestContext;
import com.works.constants.CommonConstants;
import com.works.thread.ThreadConstants;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.codec.http.websocketx.extensions.compression.WebSocketServerCompressionHandler;
import io.netty.handler.flush.FlushConsolidationHandler;
import io.netty.handler.ipfilter.IpFilterRuleType;
import io.netty.handler.ipfilter.IpSubnetFilterRule;
import io.netty.handler.ipfilter.RuleBasedIpFilter;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.SelfSignedCertificate;
import io.netty.handler.stream.ChunkedWriteHandler;
import io.netty.handler.traffic.GlobalTrafficShapingHandler;
import io.netty.util.concurrent.DefaultThreadFactory;
import io.netty.util.concurrent.UnorderedThreadPoolEventExecutor;
import lombok.extern.slf4j.Slf4j;

import javax.net.ssl.SSLException;
import java.security.cert.CertificateException;
import java.util.concurrent.ThreadFactory;

/**
 * @author DaLong
 * @date 2023/5/29 17:36
 */
@Slf4j
@ChannelHandler.Sharable
public class ServerTrigger extends SimpleChannelInboundHandler<RequestContext<MessageBody>> {

    private final ThreadFactory bossThreadFactory;

    private final ThreadFactory workerThreadFactory;

    private final ThreadFactory bizThreadFactory;

    private final Integer appPort;

    private final Integer webPort;

    private EventLoopGroup appBossGroup;

    private EventLoopGroup appWorkerGroup;

    private EventLoopGroup webBossGroup;

    private EventLoopGroup webWorkerGroup;

    private final BusinessProcessor businessProcessor;


    private UnorderedThreadPoolEventExecutor appBizWorkerGroup;

    private UnorderedThreadPoolEventExecutor webBizWorkerGroup;


    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, RequestContext<MessageBody> messageBodyImRequestContext) throws Exception {
        businessProcessor.process(channelHandlerContext.channel(), messageBodyImRequestContext);
    }

    public void bind() {
        try {
            if (this.appPort != null) {
                bindAppPort();
            }

            if (this.webPort != null) {
                bindWebPort();
            }
        } catch (Exception e) {

        }
    }

    private void bindAppPort() throws CertificateException, SSLException {
        createAppEventGroup();
        ServerBootstrap serverBootstrap = createServerBootstrap(appBossGroup, appWorkerGroup);
        //DEBUG 日志
        LoggingHandler debugLogHandler = new LoggingHandler(LogLevel.DEBUG);
        //监控
        MetricsHandler metricsHandler = new MetricsHandler();
        //INFO 日志
        LoggingHandler infoLogHandler = new LoggingHandler(LogLevel.INFO);
        //授权
        AuthHandler authHandler = new AuthHandler();
        //SSL
        SelfSignedCertificate selfSignedCertificate = new SelfSignedCertificate();
        SslContext sslContext = SslContextBuilder.forServer(selfSignedCertificate.certificate(), selfSignedCertificate.privateKey()).build();
        //ipFilter
        IpSubnetFilterRule ruleA = new IpSubnetFilterRule(ProtocolConstants.IP_ADDRESS_FILTER__RULE_REJECT, ProtocolConstants.IP_ADDRESS_FILTER_RULE_REJECT_CIDR_PREFIX, IpFilterRuleType.REJECT);
        //ipFilter
        IpSubnetFilterRule RuleB = new IpSubnetFilterRule(ProtocolConstants.IP_ADDRESS_FILTER_RULE_ACCEPT, ProtocolConstants.IP_ADDRESS_FILTER_RULE_ACCEPT_CIDR_PREFIX, IpFilterRuleType.ACCEPT);

        //OR 使用IpFilterRuleHandler,传入
        RuleBasedIpFilter ruleBasedIpFilter = new RuleBasedIpFilter(ruleA, RuleB);

        serverBootstrap.childHandler(new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(SocketChannel socketChannel) throws Exception {

                GlobalTrafficShapingHandler globalTrafficShapingHandler = new GlobalTrafficShapingHandler(socketChannel.eventLoop().parent(), 10 * 1024, 50 * 1024);
                globalTrafficShapingHandler.setMaxGlobalWriteSize(50 * 1024);
                globalTrafficShapingHandler.setMaxWriteSize(5 * 1024);


                socketChannel.pipeline().addLast(ChannelPipelineHandlerConstants.DEBUG_LOG_HANDLER, debugLogHandler);

                socketChannel.pipeline().addLast(ChannelPipelineHandlerConstants.IP_FILTER_HANDLER, ruleBasedIpFilter);

                socketChannel.pipeline().addLast(ChannelPipelineHandlerConstants.TS_HANDLER, globalTrafficShapingHandler);

                socketChannel.pipeline().addLast(ChannelPipelineHandlerConstants.METRIC_HANDLER, metricsHandler);

                socketChannel.pipeline().addLast(ChannelPipelineHandlerConstants.SSL_CONTEXT_HANDLE, sslContext.newHandler(socketChannel.alloc()));

                socketChannel.pipeline().addLast(ChannelPipelineHandlerConstants.IDLE_HANDLER, new IdleCheckHandler());

                socketChannel.pipeline().addLast(ChannelPipelineHandlerConstants.DECODER_HANDLER, new AppDecoder());
                socketChannel.pipeline().addLast(ChannelPipelineHandlerConstants.ENCODER_HANDLER, new AppEncoder());
                socketChannel.pipeline().addLast(ChannelPipelineHandlerConstants.INFO_LOG_HANDLER, infoLogHandler);

                socketChannel.pipeline().addLast(ChannelPipelineHandlerConstants.FLUSH_ENHANCE, new FlushConsolidationHandler(ProtocolConstants.EXPLICIT_FLUSH_AFTER_FLUSHES, Boolean.TRUE));

                socketChannel.pipeline().addLast(ChannelPipelineHandlerConstants.AUTH_HANDLER, authHandler);

                socketChannel.pipeline().addLast(appBizWorkerGroup, ChannelPipelineHandlerConstants.BIZ_HANDLER, ServerTrigger.this);
            }
        });

        ChannelFuture channelFuture = serverBootstrap.bind(appPort).syncUninterruptibly();

        channelFuture.channel().newSucceededFuture().addListener(future -> {
            log.info("[im-center][appServer] start on port:{}", appPort);
        });

        channelFuture.channel().closeFuture().addListener(future -> {
            this.appDestroy();
        });
    }

    private void bindWebPort() {
        createWebEventGroup();
        ServerBootstrap serverBootstrap = createServerBootstrap(webBossGroup, webWorkerGroup);

        MetricsHandler metricsHandler = new MetricsHandler();

        LoggingHandler debugLogHandler = new LoggingHandler(LogLevel.DEBUG);

        LoggingHandler infoLogHandler = new LoggingHandler(LogLevel.INFO);

        AuthHandler authHandler = new AuthHandler();

        serverBootstrap.childHandler(new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(SocketChannel socketChannel) throws Exception {
                socketChannel.pipeline().addLast(ChannelPipelineHandlerConstants.DECODER_HANDLER, debugLogHandler);
                socketChannel.pipeline().addLast(ChannelPipelineHandlerConstants.METRIC_HANDLER, metricsHandler);
                socketChannel.pipeline().addLast(ChannelPipelineHandlerConstants.IDLE_HANDLER, new IdleCheckHandler());
                socketChannel.pipeline().addLast(ChannelPipelineHandlerConstants.HTTP_CODEC, new HttpServerCodec());
                socketChannel.pipeline().addLast(ChannelPipelineHandlerConstants.AGGREGATOR, new HttpObjectAggregator(ProtocolConstants.AGGREGATOR_MAX_CONTENT_LENGTH));
                socketChannel.pipeline().addLast(ChannelPipelineHandlerConstants.HTTP_CHUNKED, new ChunkedWriteHandler());
                socketChannel.pipeline().addLast(ChannelPipelineHandlerConstants.COMPRESSION, new WebSocketServerCompressionHandler());
                socketChannel.pipeline().addLast(ChannelPipelineHandlerConstants.WEB_SOCKET_PROTOCOL_NAME,
                        new WebSocketServerProtocolHandler(ChannelPipelineHandlerConstants.WEBSOCKET_PATH, ChannelPipelineHandlerConstants.WEBSOCKET_PROTOCOL, Boolean.TRUE, ProtocolConstants.SERVER_PROTOCOL_MAX_FRAME_SIZE));
                socketChannel.pipeline().addLast(ChannelPipelineHandlerConstants.WEBSOCKET_DECODER_HANDLER, new WebDecoder());
                socketChannel.pipeline().addLast(ChannelPipelineHandlerConstants.WEBSOCKET_ENCODER_HANDLER, new WebEncoder());
                socketChannel.pipeline().addLast(ChannelPipelineHandlerConstants.INFO_LOG_HANDLER, infoLogHandler);
                socketChannel.pipeline().addLast(ChannelPipelineHandlerConstants.AUTH_HANDLER, authHandler);
                socketChannel.pipeline().addLast(webBizWorkerGroup, ChannelPipelineHandlerConstants.BIZ_HANDLER, ServerTrigger.this);
            }
        });

        ChannelFuture channelFuture = serverBootstrap.bind(webPort).syncUninterruptibly();
        channelFuture.channel().newSucceededFuture().addListener(future -> {
            log.info("[im-center][webServer] start on port:{}", webPort);
        });

        channelFuture.channel().closeFuture().addListener(future -> this.webDestroy());
    }


    private ServerBootstrap createServerBootstrap(EventLoopGroup bossGroup, EventLoopGroup webGroup) {
        ServerBootstrap bootstrap = new ServerBootstrap();
        bootstrap.group(bossGroup, webGroup);
        bootstrap.childOption(ChannelOption.TCP_NODELAY, Boolean.TRUE);
        bootstrap.childOption(ChannelOption.SO_KEEPALIVE, Boolean.TRUE);
        bootstrap.channel(isLinuxSystem() ? EpollServerSocketChannel.class : NioServerSocketChannel.class);
        return bootstrap;
    }

    private void createAppEventGroup() {
        if (isLinuxSystem()) {
            appBossGroup = new EpollEventLoopGroup(ThreadConstants.BOSS_THREAD_COUNT_DEFAULT, bossThreadFactory);
            appWorkerGroup = new EpollEventLoopGroup(ThreadConstants.WORKER_THREAD_COUNT_DEFAULT, workerThreadFactory);
        } else {
            appBossGroup = new NioEventLoopGroup(ThreadConstants.BOSS_THREAD_COUNT_DEFAULT, bizThreadFactory);
            appWorkerGroup = new NioEventLoopGroup(ThreadConstants.WORKER_THREAD_COUNT_DEFAULT, bizThreadFactory);
        }
        appBizWorkerGroup = new UnorderedThreadPoolEventExecutor(ThreadConstants.UN_ORDERED_THREAD_POOL_EVENT_CORE_POOL_SIZE, bizThreadFactory);
    }


    private void createWebEventGroup() {
        if (isLinuxSystem()) {
            webBossGroup = new EpollEventLoopGroup(ThreadConstants.BOSS_THREAD_COUNT_DEFAULT, bossThreadFactory);
            webWorkerGroup = new EpollEventLoopGroup(ThreadConstants.WORKER_THREAD_COUNT_DEFAULT, workerThreadFactory);
        } else {
            webBossGroup = new NioEventLoopGroup(ThreadConstants.BOSS_THREAD_COUNT_DEFAULT, bossThreadFactory);
            webWorkerGroup = new NioEventLoopGroup(ThreadConstants.WORKER_THREAD_COUNT_DEFAULT, workerThreadFactory);
        }
        webBizWorkerGroup = new UnorderedThreadPoolEventExecutor(ThreadConstants.UN_ORDERED_THREAD_POOL_EVENT_CORE_POOL_SIZE, bizThreadFactory);
    }

    private void appDestroy() {
        if (appBossGroup != null && !appBossGroup.isShuttingDown() && !appBossGroup.isShutdown()) {
            try {
                appBossGroup.shutdownGracefully();
            } catch (Exception e) {
                log.error("[im-center][appBossGroup] destroy exception.", e);
            }
        }
        if (appWorkerGroup != null && !appWorkerGroup.isShuttingDown() && !appWorkerGroup.isShutdown()) {
            try {
                appWorkerGroup.shutdownGracefully();
            } catch (Exception e) {
                log.error("[im-center][appWorkerGroup] destroy exception.", e);
            }
        }
    }

    private void webDestroy() {
        if (webBossGroup != null && !webBossGroup.isShuttingDown() && !webBossGroup.isShutdown()) {
            try {
                webBossGroup.shutdownGracefully();
            } catch (Exception e) {
                log.error("[im-center][webBossGroup] destroy exception.", e);
            }
        }
        if (webWorkerGroup != null && !webWorkerGroup.isShuttingDown() && !webBossGroup.isShutdown()) {
            try {
                webWorkerGroup.shutdownGracefully();
            } catch (Exception e) {
                log.error("[im-center][webWorkerGroup] destroy exception.", e);
            }
        }
    }

    public void destroy() {
        this.appDestroy();
        this.webDestroy();
    }



    private boolean isLinuxSystem() {
        String osName = System.getProperty(CommonConstants.OS_NAME);
        log.info("[im-center][isLinux] osName:{}", osName);
        return osName.contains(CommonConstants.OS_LINUX);
    }

    public ServerTrigger(Builder builder) {
        this.appPort = builder.appPort;
        this.webPort = builder.webPort;

        this.bossThreadFactory = new DefaultThreadFactory(ThreadConstants.BOSS_THREAD);
        this.workerThreadFactory = new DefaultThreadFactory(ThreadConstants.WORKER_THREAD);
        this.bizThreadFactory = new DefaultThreadFactory(ThreadConstants.BIZ_THREAD);

        this.businessProcessor = builder.build().businessProcessor;
    }

    public static class Builder {
        private Integer appPort;

        private Integer webPort;

        public Builder setAppPort(Integer appPort) {
            this.appPort = webPort;
            return this;
        }

        public Builder setWebPort(Integer webPort) {
            this.webPort = webPort;
            return this;
        }

        public Builder setBusinessProcessor(BusinessProcessor businessProcessor) {
            return this;
        }

        public ServerTrigger build() {
            return new ServerTrigger(this);
        }

    }
}
