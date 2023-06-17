package io.github.yeyuhl.remoting.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * rpc消息类
 *
 * @author yeyuhl
 * @since 2023/5/26
 */

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class RpcMessage {
    /**
     * rpc消息类型
     */
    private byte messageType;

    /**
     * 序列化类型
     */
    private byte codec;

    /**
     * 压缩类型
     */
    private byte compress;

    /**
     * 请求id
     */
    private int requestId;

    /**
     * 请求数据
     */
    private Object data;
}
