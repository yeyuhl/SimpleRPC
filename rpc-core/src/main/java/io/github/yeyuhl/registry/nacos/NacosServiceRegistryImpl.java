package io.github.yeyuhl.registry.nacos;

import com.alibaba.nacos.api.exception.NacosException;
import io.github.yeyuhl.exception.RpcException;
import io.github.yeyuhl.registry.ServiceRegistry;
import io.github.yeyuhl.registry.nacos.util.NacosUtils;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;

/**
 * Nacos服务注册实现类
 *
 * @author yeyuhl
 * @since 2023/6/15
 */
@Slf4j
public class NacosServiceRegistryImpl implements ServiceRegistry {
    @Override
    public void registerService(String rpcServiceName, InetSocketAddress inetSocketAddress) {
        try {
            NacosUtils.registerService(rpcServiceName, inetSocketAddress);
        } catch (NacosException e) {
            log.error("An error occurred while registering service [{}]", rpcServiceName);
            throw new RpcException(e.getMessage(),e);
        }
    }
}
