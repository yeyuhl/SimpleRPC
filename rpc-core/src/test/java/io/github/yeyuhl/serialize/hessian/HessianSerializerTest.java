package io.github.yeyuhl.serialize.hessian;

import io.github.yeyuhl.remoting.dto.RpcRequest;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class HessianSerializerTest {
    @Test
    public void test() {
        RpcRequest target = RpcRequest.builder().methodName("hello")
                .parameters(new Object[]{"sayhello", "helloya"})
                .interfaceName("io.github.yeyuhl.HelloService")
                .paramTypes(new Class<?>[]{String.class, String.class})
                .requestId(UUID.randomUUID().toString())
                .group("group1")
                .version("version1")
                .build();
        HessianSerializer hessianSerializer = new HessianSerializer();
        byte[] bytes = hessianSerializer.serialize(target);
        RpcRequest actual = hessianSerializer.deserialize(bytes, RpcRequest.class);
        assertEquals(target.getGroup(), actual.getGroup());
        assertEquals(target.getVersion(), actual.getVersion());
        assertEquals(target.getRequestId(), actual.getRequestId());
    }
}
