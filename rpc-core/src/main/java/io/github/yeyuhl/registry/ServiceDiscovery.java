package io.github.yeyuhl.registry;


import io.github.yeyuhl.extension.SPI;
import io.github.yeyuhl.remoting.dto.RpcRequest;

import java.net.InetSocketAddress;

/**
 * 服务发现接口
 *
 * @author yeyuhl
 * @since 2023/6/12
 */
@SPI
public interface ServiceDiscovery {
    /**
     * 根据rpc请求查找服务地址
     *
     * @param rpcRequest rpc请求
     * @return 服务地址
     */
    InetSocketAddress lookupService(RpcRequest rpcRequest);
}
