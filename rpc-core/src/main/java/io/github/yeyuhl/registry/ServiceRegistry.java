package io.github.yeyuhl.registry;

import io.github.yeyuhl.extension.SPI;

import java.net.InetSocketAddress;

/**
 * 服务注册接口
 *
 * @author yeyuhl
 * @since 2023/6/12
 */
@SPI
public interface ServiceRegistry {
    /**
     * 注册服务
     */
    void registerService(String rpcServiceName, InetSocketAddress inetSocketAddress);
}
