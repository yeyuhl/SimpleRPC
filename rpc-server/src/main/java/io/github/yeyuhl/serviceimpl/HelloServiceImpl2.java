package io.github.yeyuhl.serviceimpl;

import io.github.yeyuhl.HelloService;
import io.github.yeyuhl.entity.Hello;
import lombok.extern.slf4j.Slf4j;

/**
 * HelloService的实现类
 *
 * @author yeyuhl
 * @since 2023/6/15
 */
@Slf4j
public class HelloServiceImpl2 implements HelloService {

    static {
        System.out.println("HelloServiceImpl2被创建");
    }

    @Override
    public String hello(Hello hello) {
        log.info("HelloServiceImpl2收到: {}.", hello.getMessage());
        String result = "Hello description is " + hello.getDescription();
        log.info("HelloServiceImpl2返回: {}.", result);
        return result;
    }
}
