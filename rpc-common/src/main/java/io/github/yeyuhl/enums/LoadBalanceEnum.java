package io.github.yeyuhl.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 负载均衡策略枚举类
 *
 * @author yeyuhl
 * @since 2023/6/14
 */
@AllArgsConstructor
@Getter
public enum LoadBalanceEnum {
    LOADBALANCE("loadBalance");

    private final String name;
}
