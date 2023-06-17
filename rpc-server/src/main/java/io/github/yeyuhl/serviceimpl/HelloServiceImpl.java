package io.github.yeyuhl.serviceimpl;

import io.github.yeyuhl.HelloService;
import io.github.yeyuhl.annotation.RpcService;
import io.github.yeyuhl.entity.Hello;
import lombok.extern.slf4j.Slf4j;

/**
 * HelloService的实现类
 *
 * @author yeyuhl
 * @since 2023/6/15
 */
@Slf4j
@RpcService(group = "test1", version = "version1")
public class HelloServiceImpl implements HelloService {
    static {
        System.out.println("HelloServiceImpl被创建");
    }

    @Override
    public String hello(Hello hello) {
        log.info("HelloServiceImpl收到: {}.", hello.getMessage());
        String result = "Hello description is " + hello.getDescription();
        log.info("HelloServiceImpl返回: {}.", result);
        return result;
    }
}
