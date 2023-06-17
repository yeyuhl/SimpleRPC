package io.github.yeyuhl.proxy;

import io.github.yeyuhl.config.RpcServiceConfig;
import io.github.yeyuhl.enums.RpcErrorMessageEnum;
import io.github.yeyuhl.enums.RpcResponseCodeEnum;
import io.github.yeyuhl.exception.RpcException;
import io.github.yeyuhl.remoting.dto.RpcRequest;
import io.github.yeyuhl.remoting.dto.RpcResponse;
import io.github.yeyuhl.remoting.transport.RpcRequestTransport;
import io.github.yeyuhl.remoting.transport.netty.client.NettyRpcClient;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * rpc客户端代理类
 * 当一个动态代理对象调用一个方法时，它实际上调用了invoke方法
 * 正是因为有了动态代理，客户端调用远程方法才能像调用本地方法一样（屏蔽了中间过程）
 *
 * @author yeyuhl
 * @since 2023/6/13
 */
@Slf4j
public class RpcClientProxy implements InvocationHandler {
    private static final String INTERFACE_NAME = "interfaceName";
    private final RpcRequestTransport rpcRequestTransport;
    private final RpcServiceConfig rpcServiceConfig;

    public RpcClientProxy(RpcRequestTransport rpcRequestTransport, RpcServiceConfig rpcServiceConfig) {
        this.rpcRequestTransport = rpcRequestTransport;
        this.rpcServiceConfig = rpcServiceConfig;
    }

    public RpcClientProxy(RpcRequestTransport rpcRequestTransport) {
        this.rpcRequestTransport = rpcRequestTransport;
        this.rpcServiceConfig = new RpcServiceConfig();
    }

    /**
     * 获取代理对象
     */
    @SuppressWarnings("unchecked")
    public <T> T getProxy(Class<T> clazz) {
        // 生成代理对象，参数为：目标对象的类加载器，代理对象需要实现的接口，InvocationHandler
        return (T) Proxy.newProxyInstance(clazz.getClassLoader(), new Class<?>[]{clazz}, this);
    }

    /**
     * 代理对象调用方法时，实际上调用的是invoke方法
     */
    @SneakyThrows
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        log.info("invoked method: [{}]", method.getName());
        RpcRequest rpcRequest = RpcRequest.builder()
                .interfaceName(method.getDeclaringClass().getName())
                .methodName(method.getName())
                .parameters(args)
                .paramTypes(method.getParameterTypes())
                .requestId(UUID.randomUUID().toString())
                .group(rpcServiceConfig.getGroup())
                .version(rpcServiceConfig.getVersion())
                .build();
        RpcResponse<Object> rpcResponse = null;
        if(rpcRequestTransport instanceof NettyRpcClient) {
            CompletableFuture<RpcResponse<Object>> completableFuture = (CompletableFuture<RpcResponse<Object>>) rpcRequestTransport.sendRpcRequest(rpcRequest);
            rpcResponse = completableFuture.get();
        }
        this.check(rpcResponse, rpcRequest);
        return rpcResponse.getData();
    }

    private void  check (RpcResponse<Object> rpcResponse,RpcRequest rpcRequest){
        if(rpcRequest== null){
            throw new RpcException(RpcErrorMessageEnum.SERVICE_INVOCATION_FAILURE, INTERFACE_NAME + ":" + rpcRequest.getInterfaceName());
        }

        if(!rpcRequest.getRequestId().equals(rpcResponse.getRequestId())){
            throw new RpcException(RpcErrorMessageEnum.REQUEST_NOT_MATCH_RESPONSE, INTERFACE_NAME + ":" + rpcRequest.getInterfaceName());
        }

        if(rpcResponse.getCode()==null||!rpcResponse.getCode().equals(RpcResponseCodeEnum.SUCCESS.getCode())){
            throw new RpcException(RpcErrorMessageEnum.SERVICE_INVOCATION_FAILURE, INTERFACE_NAME + ":" + rpcRequest.getInterfaceName());
        }
    }
}
