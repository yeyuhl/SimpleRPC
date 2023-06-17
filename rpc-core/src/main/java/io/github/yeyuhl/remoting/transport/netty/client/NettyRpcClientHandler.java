package io.github.yeyuhl.remoting.transport.netty.client;

import io.github.yeyuhl.enums.CompressTypeEnum;
import io.github.yeyuhl.enums.SerializationTypeEnum;
import io.github.yeyuhl.factory.SingletonFactory;
import io.github.yeyuhl.remoting.constants.RpcConstants;
import io.github.yeyuhl.remoting.dto.RpcMessage;
import io.github.yeyuhl.remoting.dto.RpcResponse;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.ReferenceCountUtil;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;

/**
 * 自定义客户端ChannelHandler处理服务器发送的数据
 *
 * @author yeyuhl
 * @since 2023/6/12
 */
@Slf4j
public class NettyRpcClientHandler extends ChannelInboundHandlerAdapter {
    private final UnprocessedRequests unprocessedRequests;
    private final NettyRpcClient nettyRpcClient;

    public NettyRpcClientHandler() {
        this.unprocessedRequests = SingletonFactory.getInstance(UnprocessedRequests.class);
        this.nettyRpcClient = SingletonFactory.getInstance(NettyRpcClient.class);
    }

    /**
     * 读取服务器发送的数据
     */
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        try {
            log.info("client receive msg: [{}]", msg);
            // 检查是否是RpcMessage类型的数据
            if (msg instanceof RpcMessage) {
                RpcMessage tmp = (RpcMessage) msg;
                byte messageType = tmp.getMessageType();
                // 如果是心跳消息，则直接打印
                if (messageType == RpcConstants.HEARTBEAT_RESPONSE_TYPE) {
                    log.info("heart [{}]", tmp.getData());
                } else if (messageType == RpcConstants.RESPONSE_TYPE) {
                    // 如果是响应消息，则将响应结果放入unprocessedRequests中
                    RpcResponse<Object> rpcResponse = (RpcResponse<Object>) tmp.getData();
                    unprocessedRequests.complete(rpcResponse);
                }
            }
        } finally {
            // 释放资源
            ReferenceCountUtil.release(msg);
        }
    }

    /**
     * 当连接的空闲时间（读或者写）太长时，将会触发一个IdleStateEvent事件，利用userEventTrigged方法来处理该事件
     */
    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        // 查看evt是否是IdleStateEvent事件
        if (evt instanceof IdleStateEvent) {
            // 如果是，获取空闲事件的状态
            IdleState state = ((IdleStateEvent) evt).state();
            // 如果是写空闲，将消息记录到控制台并向远程服务器发送心跳请求
            if (state == IdleState.WRITER_IDLE) {
                log.info("write idle happen [{}]", ctx.channel().remoteAddress());
                Channel channel = nettyRpcClient.getChannel((InetSocketAddress) ctx.channel().remoteAddress());
                RpcMessage rpcMessage = new RpcMessage();
                rpcMessage.setCodec(SerializationTypeEnum.PROTOSTUFF.getCode());
                rpcMessage.setCompress(CompressTypeEnum.GZIP.getCode());
                rpcMessage.setMessageType(RpcConstants.HEARTBEAT_REQUEST_TYPE);
                rpcMessage.setData(RpcConstants.PING);
                // 写入并刷新，增加监听器，如果写入失败则关闭通道
                channel.writeAndFlush(rpcMessage).addListener(ChannelFutureListener.CLOSE_ON_FAILURE);
            }
            // 如果不是，调用super方法来处理
        } else {
            super.userEventTriggered(ctx, evt);
        }
    }

    /**
     * 处理client message时出现异常时调用
     */
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        // 打印异常
        log.error("client catch exception：", cause);
        // 打印异常的堆栈跟踪
        cause.printStackTrace();
        // 关闭通道
        ctx.close();
    }
}
