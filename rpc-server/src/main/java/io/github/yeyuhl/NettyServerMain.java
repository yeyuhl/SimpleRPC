package io.github.yeyuhl;

import io.github.yeyuhl.annotation.RpcScan;
import io.github.yeyuhl.config.RpcServiceConfig;
import io.github.yeyuhl.remoting.transport.netty.server.NettyRpcServer;
import io.github.yeyuhl.serviceimpl.HelloServiceImpl2;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

/**
 * Netty服务端启动类
 *
 * @author yeyuhl
 * @since 2023/6/15
 */
@RpcScan(basePackage = {"io.github.yeyuhl"})
public class NettyServerMain {
    public static void main(String[] args) {
        // Register service via annotation
        AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext(NettyServerMain.class);
        NettyRpcServer nettyRpcServer = (NettyRpcServer) applicationContext.getBean("nettyRpcServer");
        // Register service manually
        HelloService helloService2 = new HelloServiceImpl2();
        RpcServiceConfig rpcServiceConfig = RpcServiceConfig.builder()
                .group("test2").version("version2").service(helloService2).build();
        nettyRpcServer.registerService(rpcServiceConfig);
        nettyRpcServer.start();
    }
}
