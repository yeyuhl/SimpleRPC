package io.github.yeyuhl.remoting.handler;

import io.github.yeyuhl.exception.RpcException;
import io.github.yeyuhl.factory.SingletonFactory;
import io.github.yeyuhl.provider.ServiceProvider;
import io.github.yeyuhl.provider.impl.ServiceProviderImpl;
import io.github.yeyuhl.remoting.dto.RpcRequest;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Method;

/**
 * 处理RpcRequest的Handler
 *
 * @author yeyuhl
 * @since 2023/6/13
 */
@Slf4j
public class RpcRequestHandler {
    private final ServiceProvider serviceProvider;

    public RpcRequestHandler() {
        this.serviceProvider = SingletonFactory.getInstance(ServiceProviderImpl.class);
    }

    /**
     * 处理RpcRequest，调用对应的方法，然后返回该方法的结果
     */
    public Object handle(RpcRequest rpcRequest) {
        Object service = serviceProvider.getService(rpcRequest.getRpcServiceName());
        return invokeTargetMethod(rpcRequest, service);
    }

    /**
     * 获取方法的执行结果
     */
    public Object invokeTargetMethod(RpcRequest rpcRequest, Object service) {
        Object result;
        try {
            Method method = service.getClass().getMethod(rpcRequest.getMethodName(), rpcRequest.getParamTypes());
            result = method.invoke(service, rpcRequest.getParameters());
            log.info("Service: [{}] successful invoke method: [{}]", rpcRequest.getInterfaceName(), rpcRequest.getMethodName());
        } catch (Exception e) {
            throw new RpcException(e.getMessage(), e);
        }
        return result;
    }

}
