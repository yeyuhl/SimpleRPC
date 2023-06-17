package io.github.yeyuhl.exception;

import io.github.yeyuhl.enums.RpcErrorMessageEnum;

/**
 * 自定义rpc异常
 *
 * @author yeyuhl
 * @since 2023/5/27
 */
public class RpcException extends RuntimeException{
    public RpcException(RpcErrorMessageEnum rpcErrorMessageEnum, String detail) {
        super(rpcErrorMessageEnum.getMessage() + ":" + detail);
    }

    public RpcException(String message, Throwable cause) {
        super(message, cause);
    }

    public RpcException(RpcErrorMessageEnum rpcErrorMessageEnum) {
        super(rpcErrorMessageEnum.getMessage());
    }
}
