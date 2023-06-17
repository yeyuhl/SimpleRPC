package io.github.yeyuhl.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 服务发现枚举类
 *
 * @author yeyuhl
 * @since 2023/6/12
 */
@AllArgsConstructor
@Getter
public enum ServiceDiscoveryEnum {
    ZK("zk"),
    NACOS("nacos");
    private final String name;
}
