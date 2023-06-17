package io.github.yeyuhl.remoting.transport;

import io.github.yeyuhl.extension.SPI;
import io.github.yeyuhl.remoting.dto.RpcRequest;

/**
 * rpc客户端传输接口，这个接口可以用netty实现，也可以用socket实现
 *
 * @author yeyuhl
 * @since 2023/5/26
 */
@SPI
public interface RpcRequestTransport {
    /**
     * 发送rpc请求并从服务器接收响应
     *
     * @param rpcRequest rpc请求
     * @return 服务器返回的对象
     */
    Object sendRpcRequest(RpcRequest rpcRequest);
}
