package io.github.yeyuhl.remoting.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * rpc请求类
 *
 * @author yeyuhl
 * @since 2023/5/26
 */
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class RpcRequest implements Serializable {
    /**
     * 序列化id
     */
    private static final long serialVersionUID = 2023052601L;

    /**
     * 请求id
     */
    private String requestId;

    /**
     * 要调用的方法的接口名
     */
    private String interfaceName;

    /**
     * 要调用的方法名
     */
    private String methodName;

    /**
     * 要传递的参数
     */
    private Object[] parameters;

    /**
     * 参数类型
     */
    private Class<?>[] paramTypes;

    /**
     * 版本号
     */
    private String version;

    /**
     * 同一接口不同实现类的区分标识
     */
    private String group;

    /**
     * 获取rpc服务名
     */
    public String getRpcServiceName() {
        return this.getInterfaceName() + this.getGroup() + this.getVersion();
    }
}
