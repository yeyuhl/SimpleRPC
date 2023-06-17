package io.github.yeyuhl.loadbalance;

import io.github.yeyuhl.extension.SPI;
import io.github.yeyuhl.remoting.dto.RpcRequest;

import java.util.List;

/**
 * 负载均衡策略接口
 *
 * @author yeyuhl
 * @since 2023/6/14
 */
@SPI
public interface LoadBalance {
    /**
     * 从服务列表中选择一个服务
     *
     * @param serviceUrlList 服务地址的列表
     * @param rpcRequest     rpc请求
     * @return 选择的服务
     */
    String selectServiceAddress(List<String> serviceUrlList, RpcRequest rpcRequest);
}
