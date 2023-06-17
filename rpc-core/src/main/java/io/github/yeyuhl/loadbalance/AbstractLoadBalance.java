package io.github.yeyuhl.loadbalance;

import cn.hutool.core.collection.CollectionUtil;
import io.github.yeyuhl.remoting.dto.RpcRequest;

import java.util.List;

/**
 * 负载均衡策略的抽象类
 *
 * @author yeyuhl
 * @since 2023/6/14
 */
public abstract class AbstractLoadBalance implements LoadBalance{
    @Override
    public String selectServiceAddress(List<String> serviceAddresses, RpcRequest rpcRequest) {
        // 如果服务地址列表为空，返回null
        if (CollectionUtil.isEmpty(serviceAddresses)) {
            return null;
        }
        // 如果只有一个服务地址，直接返回
        if (serviceAddresses.size() == 1) {
            return serviceAddresses.get(0);
        }
        // 如果有多个，则调用子类的doSelect方法
        return doSelect(serviceAddresses, rpcRequest);
    }

    protected abstract String doSelect(List<String> serviceAddresses, RpcRequest rpcRequest);
}
