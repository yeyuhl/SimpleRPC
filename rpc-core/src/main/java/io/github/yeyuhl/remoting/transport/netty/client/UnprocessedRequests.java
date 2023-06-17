package io.github.yeyuhl.remoting.transport.netty.client;

import io.github.yeyuhl.remoting.dto.RpcResponse;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 存放未被服务器处理的请求
 * CompletableFuture是一个用于异步编程的工具类
 *
 * @author yeyuhl
 * @since 2023/5/26
 */
public class UnprocessedRequests {
    /**
     * 存放请求id和对应的RpcResponse
     */
    private static final Map<String, CompletableFuture<RpcResponse<Object>>> UNPROCESSED_RESPONSE_FUTURES = new ConcurrentHashMap<>();

    /**
     * 存放请求
     *
     * @param requestId 请求id
     * @param future    请求id对应的响应
     */
    public void put(String requestId, CompletableFuture<RpcResponse<Object>> future) {
        UNPROCESSED_RESPONSE_FUTURES.put(requestId, future);
    }

    /**
     * 完成请求
     *
     * @param rpcResponse 响应
     */
    public void complete(RpcResponse<Object> rpcResponse) {
        CompletableFuture<RpcResponse<Object>> future = UNPROCESSED_RESPONSE_FUTURES.remove(rpcResponse.getRequestId());
        if (null != future) {
            // complete方法只能调用一次，将处理完的响应放入future中
            future.complete(rpcResponse);
        } else {
            throw new IllegalStateException();
        }
    }
}
