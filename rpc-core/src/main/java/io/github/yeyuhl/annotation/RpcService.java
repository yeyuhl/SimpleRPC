package io.github.yeyuhl.annotation;

import java.lang.annotation.*;

/**
 * 注册服务的注解
 *
 * @author yeyuhl
 * @since 2023/6/14
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
@Inherited
public @interface RpcService {
    /**
     * 服务版本号，默认为空
     */
    String version() default "";

    /**
     * 服务分组，默认为空
     */
    String group() default "";
}
