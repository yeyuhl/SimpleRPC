package io.github.yeyuhl.remoting.transport.netty.client;

import io.github.yeyuhl.enums.CompressTypeEnum;
import io.github.yeyuhl.enums.SerializationTypeEnum;
import io.github.yeyuhl.enums.ServiceDiscoveryEnum;
import io.github.yeyuhl.extension.ExtensionLoader;
import io.github.yeyuhl.factory.SingletonFactory;
import io.github.yeyuhl.registry.ServiceDiscovery;
import io.github.yeyuhl.remoting.constants.RpcConstants;
import io.github.yeyuhl.remoting.dto.RpcMessage;
import io.github.yeyuhl.remoting.dto.RpcRequest;
import io.github.yeyuhl.remoting.dto.RpcResponse;
import io.github.yeyuhl.remoting.transport.RpcRequestTransport;
import io.github.yeyuhl.remoting.transport.netty.codec.RpcMessageDecoder;
import io.github.yeyuhl.remoting.transport.netty.codec.RpcMessageEncoder;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.timeout.IdleStateHandler;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * 基于netty的rpc客户端
 *
 * @author yeyuhl
 * @since 2023/5/26
 */
@Slf4j
public class NettyRpcClient implements RpcRequestTransport {
    private final UnprocessedRequests unprocessedRequests;
    private final ServiceDiscovery serviceDiscovery;
    private final ChannelProvider channelProvider;
    private final EventLoopGroup eventLoopGroup;
    private final Bootstrap bootstrap;

    /**
     * NettyRpcClient的构造函数
     */
    public NettyRpcClient() {
        // 事件循环组
        eventLoopGroup = new NioEventLoopGroup();
        // 启动引导类
        bootstrap = new Bootstrap();
        bootstrap.group(eventLoopGroup)
                .channel(NioSocketChannel.class)
                // 往pipeline中添加日志处理器
                .handler(new LoggingHandler(LogLevel.INFO))
                // 设置连接超时时间，5s内没有连接上则连接失败
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel socketChannel) throws Exception {
                        ChannelPipeline channelPipeline = socketChannel.pipeline();
                        // 增加IdleStateHandler，如果5秒内没有写入数据（要发送到服务器的数据），则发送心跳请求
                        channelPipeline.addLast(new IdleStateHandler(0, 5, 0, TimeUnit.SECONDS));
                        channelPipeline.addLast(new RpcMessageEncoder());
                        channelPipeline.addLast(new RpcMessageDecoder());
                        channelPipeline.addLast(new NettyRpcClientHandler());
                    }
                });
        this.serviceDiscovery = ExtensionLoader.getExtensionLoader(ServiceDiscovery.class).getExtension(ServiceDiscoveryEnum.NACOS.getName());
        this.unprocessedRequests = SingletonFactory.getInstance(UnprocessedRequests.class);
        this.channelProvider = SingletonFactory.getInstance(ChannelProvider.class);
    }

    /**
     * 连接服务器并获取channel，以便向服务器发送rpc消息
     */
    @SneakyThrows
    public Channel doConnect(InetSocketAddress inetSocketAddress) {
        CompletableFuture<Channel> completableFuture = new CompletableFuture<>();
        bootstrap.connect(inetSocketAddress).addListener((ChannelFutureListener) future -> {
            if (future.isSuccess()) {
                log.info("The client has connected [{}] successful!", inetSocketAddress.toString());
                completableFuture.complete(future.channel());
            } else {
                throw new IllegalStateException();
            }
        });
        return completableFuture.get();
    }

    /**
     * 发送rpc请求
     */
    @Override
    public Object sendRpcRequest(RpcRequest rpcRequest) {
        // 构建返回值
        CompletableFuture<RpcResponse<Object>> resultFuture = new CompletableFuture<>();
        // 获取服务地址
        InetSocketAddress inetSocketAddress = serviceDiscovery.lookupService(rpcRequest);
        // 获取channel
        Channel channel = getChannel(inetSocketAddress);
        if (channel.isActive()) {
            // 将返回值放入未处理的请求中
            unprocessedRequests.put(rpcRequest.getRequestId(), resultFuture);
            RpcMessage rpcMessage = RpcMessage.builder()
                    .data(rpcRequest)
                    .codec(SerializationTypeEnum.HESSIAN.getCode())
                    .compress(CompressTypeEnum.GZIP.getCode())
                    .messageType(RpcConstants.REQUEST_TYPE)
                    .build();
            channel.writeAndFlush(rpcMessage).addListener((ChannelFutureListener) future -> {
                if (future.isSuccess()) {
                    // 发送成功，打印日志
                    log.info("client send message: [{}]", rpcMessage);
                } else {
                    // 发送失败，关闭channel，将异常信息放入返回值中
                    future.channel().close();
                    resultFuture.completeExceptionally(future.cause());
                    log.error("Send failed:", future.cause());
                }
            });
        } else {
            throw new IllegalStateException();
        }
        return resultFuture;
    }

    /**
     * 获取channel，如果channel不存在则创建一个新的channel
     */
    public Channel getChannel(InetSocketAddress inetSocketAddress) {
        Channel channel = channelProvider.get(inetSocketAddress);
        if (channel == null) {
            channel = doConnect(inetSocketAddress);
            channelProvider.set(inetSocketAddress, channel);
        }
        return channel;
    }

    /**
     * 关闭事件循环组
     */
    public void close() {
        eventLoopGroup.shutdownGracefully();
    }
}
