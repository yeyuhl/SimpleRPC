package io.github.yeyuhl.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 传输方式枚举类
 *
 * @author yeyuhl
 * @since 2023/6/14
 */
@AllArgsConstructor
@Getter
public enum RpcRequestTransportEnum {
    NETTY("netty"),
    SOCKET("socket");

    private final String name;

}
