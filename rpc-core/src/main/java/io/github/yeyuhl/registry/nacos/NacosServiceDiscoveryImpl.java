package io.github.yeyuhl.registry.nacos;

import cn.hutool.core.collection.CollectionUtil;
import io.github.yeyuhl.enums.LoadBalanceEnum;
import io.github.yeyuhl.enums.RpcErrorMessageEnum;
import io.github.yeyuhl.exception.RpcException;
import io.github.yeyuhl.extension.ExtensionLoader;
import io.github.yeyuhl.loadbalance.LoadBalance;
import io.github.yeyuhl.registry.ServiceDiscovery;
import io.github.yeyuhl.registry.nacos.util.NacosUtils;
import io.github.yeyuhl.remoting.dto.RpcRequest;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Nacos服务发现实现类
 *
 * @author yeyuhl
 * @since 2023/6/15
 */
@Slf4j
public class NacosServiceDiscoveryImpl implements ServiceDiscovery {
    private final LoadBalance loadBalance;

    public NacosServiceDiscoveryImpl() {
        this.loadBalance = ExtensionLoader.getExtensionLoader(LoadBalance.class).getExtension(LoadBalanceEnum.LOADBALANCE.getName());
    }

    @SneakyThrows
    @Override
    public InetSocketAddress lookupService(RpcRequest rpcRequest) {
        String rpcServiceName = rpcRequest.getRpcServiceName();
        List<String> serviceUrlList = NacosUtils.getAllInstance(rpcServiceName).stream()
                .map(instance -> instance.getIp() + ":" + instance.getPort())
                .collect(Collectors.toList());
        if (CollectionUtil.isEmpty(serviceUrlList)) {
            throw new RpcException(RpcErrorMessageEnum.SERVICE_CAN_NOT_BE_FOUND, rpcServiceName);
        }
        //负载均衡
        String targetServiceUrl = loadBalance.selectServiceAddress(serviceUrlList, rpcRequest);
        log.info("Successfully found the service address:[{}]", targetServiceUrl);
        String[] socketAddressArray = targetServiceUrl.split(":");
        String host = socketAddressArray[0];
        int port = Integer.parseInt(socketAddressArray[1]);
        return new InetSocketAddress(host, port);
    }
}
