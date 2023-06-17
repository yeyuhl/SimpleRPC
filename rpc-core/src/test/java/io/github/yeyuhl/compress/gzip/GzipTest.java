package io.github.yeyuhl.compress.gzip;


import io.github.yeyuhl.compress.Compress;
import io.github.yeyuhl.remoting.dto.RpcRequest;
import io.github.yeyuhl.serialize.hessian.HessianSerializer;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class GzipTest {
    @Test
    void test() {
        Compress compress = new GzipCompress();
        RpcRequest rpcRequest = RpcRequest.builder().methodName("hello")
                .parameters(new Object[]{"sayhello", "helloya"})
                .interfaceName("io.github.yeyuhl.HelloService")
                .paramTypes(new Class<?>[]{String.class, String.class})
                .requestId(UUID.randomUUID().toString())
                .group("group1")
                .version("version1")
                .build();
        HessianSerializer serializer = new HessianSerializer();
        byte[] rpcRequestBytes = serializer.serialize(rpcRequest);
        byte[] compressRpcRequestBytes = compress.compress(rpcRequestBytes);
        byte[] decompressRpcRequestBytes = compress.decompress(compressRpcRequestBytes);
        assertEquals(rpcRequestBytes.length, decompressRpcRequestBytes.length);
    }
}
