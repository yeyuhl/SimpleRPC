package io.github.yeyuhl.registry.zk;

import cn.hutool.core.collection.CollectionUtil;
import io.github.yeyuhl.enums.LoadBalanceEnum;
import io.github.yeyuhl.enums.RpcErrorMessageEnum;
import io.github.yeyuhl.exception.RpcException;
import io.github.yeyuhl.extension.ExtensionLoader;
import io.github.yeyuhl.loadbalance.LoadBalance;
import io.github.yeyuhl.registry.ServiceDiscovery;
import io.github.yeyuhl.registry.zk.util.CuratorUtils;
import io.github.yeyuhl.remoting.dto.RpcRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.curator.framework.CuratorFramework;

import java.net.InetSocketAddress;
import java.util.List;

/**
 * 基于zk的服务注册实现类
 *
 * @author yeyuhl
 * @since 2023/6/14
 */
@Slf4j
public class ZkServiceDiscoveryImpl implements ServiceDiscovery {
    private final LoadBalance loadBalance;

    public ZkServiceDiscoveryImpl() {
        this.loadBalance = ExtensionLoader.getExtensionLoader(LoadBalance.class).getExtension(LoadBalanceEnum.LOADBALANCE.getName());
    }

    @Override
    public InetSocketAddress lookupService(RpcRequest rpcRequest) {
        String rpcServiceName = rpcRequest.getRpcServiceName();
        CuratorFramework zkClient = CuratorUtils.getZkClient();
        List<String> serviceUrlList = CuratorUtils.getChildrenNodes(zkClient, rpcServiceName);
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
