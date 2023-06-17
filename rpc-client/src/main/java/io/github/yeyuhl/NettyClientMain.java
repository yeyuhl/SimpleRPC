package io.github.yeyuhl;

import io.github.yeyuhl.annotation.RpcScan;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

/**
 * Netty客户端启动类
 *
 * @author yeyuhl
 * @since 2023/6/15
 */
@RpcScan(basePackage = {"io.github.yeyuhl"})
public class NettyClientMain {
    public static void main(String[] args) throws InterruptedException {
        AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext(NettyClientMain.class);
        HelloController helloController = (HelloController) applicationContext.getBean("helloController");
        helloController.test();
    }
}
