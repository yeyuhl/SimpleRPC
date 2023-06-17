package io.github.yeyuhl.provider;

import io.github.yeyuhl.config.RpcServiceConfig;

/**
 * 存储和提供服务对象
 *
 * @author yeyuhl
 * @since 2023/6/12
 */
public interface ServiceProvider {
    /**
     * 添加服务
     */
    void addService(RpcServiceConfig rpcServiceConfig);

    /**
     * 获取服务
     */
    Object getService(String rpcServiceName);

    /**
     * 发布服务
     */
    void publishService(RpcServiceConfig rpcServiceConfig);
}
