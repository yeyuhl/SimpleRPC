package io.github.yeyuhl.remoting.transport.netty.server;

import io.github.yeyuhl.config.CustomShutdownHook;
import io.github.yeyuhl.config.RpcServiceConfig;
import io.github.yeyuhl.factory.SingletonFactory;
import io.github.yeyuhl.provider.ServiceProvider;
import io.github.yeyuhl.provider.impl.ServiceProviderImpl;
import io.github.yeyuhl.remoting.transport.netty.codec.RpcMessageDecoder;
import io.github.yeyuhl.remoting.transport.netty.codec.RpcMessageEncoder;
import io.github.yeyuhl.utils.concurrent.threadpool.ThreadPoolFactoryUtil;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.concurrent.DefaultEventExecutorGroup;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.util.concurrent.TimeUnit;


/**
 * 基于netty的rpc服务器
 * 接收客户端消息，根据客户端消息调用相应的方法，然后将结果返回给客户端。
 *
 * @author yeyuhl
 * @since 2023/6/12
 */
@Slf4j
@Component
public class NettyRpcServer {
    public static final int PORT = 9998;
    private final ServiceProvider serviceProvider = SingletonFactory.getInstance(ServiceProviderImpl.class);

    public void registerService(RpcServiceConfig rpcServiceConfig) {
        serviceProvider.publishService(rpcServiceConfig);
    }

    @SneakyThrows
    public void start() {
        CustomShutdownHook.getCustomShutdownHook().clearAll();
        String host = InetAddress.getLocalHost().getHostAddress();
        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        DefaultEventExecutorGroup serviceHandlerGroup = new DefaultEventExecutorGroup(
                // 服务端默认线程数为CPU核心数的两倍
                Runtime.getRuntime().availableProcessors() * 2,
                ThreadPoolFactoryUtil.createThreadFactory("service-handler-group", false));
        try {
            ServerBootstrap serverBootstrap = new ServerBootstrap();
            // 服务端启动配置
            serverBootstrap.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    // TCP默认开启Nagle算法，其作用是尽可能的发送大数据块，减少网络传输。TCP_NODELAY，true就是启用Nagle算法
                    .childOption(ChannelOption.TCP_NODELAY, true)
                    // 是否开启TCP底层心跳机制，true为开启
                    .childOption(ChannelOption.SO_KEEPALIVE, true)
                    // 表示系统用于临时存放已完成三次握手的请求的队列的最大长度，如果连接建立频繁，服务器处理创建新连接较慢，可以适当调大这个参数
                    .childOption(ChannelOption.SO_BACKLOG, 128)
                    .handler(new LoggingHandler(LogLevel.INFO))
                    // 当客户端第一次进行请求的时候才会进行初始化
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel socketChannel) throws Exception {
                            // 30s内没有收到客户端请求，就关闭连接
                            ChannelPipeline pipeline = socketChannel.pipeline();
                            pipeline.addLast(new IdleStateHandler(30, 0, 0, TimeUnit.SECONDS));
                            pipeline.addLast(new RpcMessageEncoder());
                            pipeline.addLast(new RpcMessageDecoder());
                            pipeline.addLast(serviceHandlerGroup, new NettyRpcServerHandler());
                        }
                    });
            // 绑定端口，同步等待绑定成功
            ChannelFuture channelFuture = serverBootstrap.bind(host, PORT).sync();
            // 等待服务端监听端口关闭
            channelFuture.channel().closeFuture().sync();
        } catch (InterruptedException e) {
            log.error("occur exception when start server: ", e);
        } finally {
            log.error("shutdown bossGroup and workerGroup");
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
            serviceHandlerGroup.shutdownGracefully();
        }
    }
}
