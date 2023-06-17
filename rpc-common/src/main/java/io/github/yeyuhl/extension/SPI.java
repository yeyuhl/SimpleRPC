package io.github.yeyuhl.extension;

import java.lang.annotation.*;

/**
 * SPI注解，模仿Dubbo的SPI注解
 *
 * @author yeyuhl
 * @since 2023/5/26
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface SPI {
}
