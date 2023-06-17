package io.github.yeyuhl.loadbalance.loadbalancer;

import io.github.yeyuhl.loadbalance.AbstractLoadBalance;
import io.github.yeyuhl.remoting.dto.RpcRequest;

import java.util.List;
import java.util.Random;

/**
 * 随机负载均衡策略
 *
 * @author yeyuhl
 * @since 2023/6/14
 */
public class RandomLoadBalance extends AbstractLoadBalance {
    @Override
    protected String doSelect(List<String> serviceAddresses, RpcRequest rpcRequest) {
        Random random = new Random();
        return serviceAddresses.get(random.nextInt(serviceAddresses.size()));
    }
}
