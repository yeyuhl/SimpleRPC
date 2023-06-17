package io.github.yeyuhl.remoting.dto;

import io.github.yeyuhl.enums.RpcResponseCodeEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * rpc响应类
 *
 * @author yeyuhl
 * @since 2023/5/26
 */
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class RpcResponse<T> implements Serializable {
    /**
     * 序列化id
     */
    private static final long serialVersionUID = 2023052602L;

    /**
     * 处理的请求的id
     */
    private String requestId;

    /**
     * 响应状态码
     */
    private Integer code;

    /**
     * 响应消息
     */
    private String message;

    /**
     * 响应数据
     */
    private T data;

    /**
     * 响应成功
     */
    public static <T> RpcResponse<T> success(T data, String requestId) {
        RpcResponse<T> response = new RpcResponse<>();
        response.setCode(RpcResponseCodeEnum.SUCCESS.getCode());
        response.setMessage(RpcResponseCodeEnum.SUCCESS.getMessage());
        response.setRequestId(requestId);
        if (data != null) {
            response.setData(data);
        }
        return response;
    }

    /**
     * 响应失败
     */
    public static <T> RpcResponse<T> fail(RpcResponseCodeEnum rpcResponseCodeEnum) {
        RpcResponse<T> response = new RpcResponse<>();
        response.setCode(rpcResponseCodeEnum.getCode());
        response.setMessage(rpcResponseCodeEnum.getMessage());
        return response;
    }

}
