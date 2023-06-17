package io.github.yeyuhl.config;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * rpc服务配置类
 *
 * @author yeyuhl
 * @since 2023/6/12
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RpcServiceConfig {
    /**
     * 服务的版本
     */
    private String version = "";
    /**
     * 当接口有多个实现类时，按组区分
     */
    private String group = "";
    /**
     * 服务对象
     */
    private Object service;

    public String getRpcServiceName() {
        return this.getServiceName() + this.getGroup() + this.getVersion();
    }

    public String getServiceName() {
        return this.getService().getClass().getInterfaces()[0].getCanonicalName();
    }
}
