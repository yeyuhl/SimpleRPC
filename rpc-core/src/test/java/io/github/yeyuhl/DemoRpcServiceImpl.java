package io.github.yeyuhl;

import io.github.yeyuhl.annotation.RpcService;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RpcService(group = "test1", version = "version1")
public class DemoRpcServiceImpl implements DemoRpcService {
    @Override
    public String hello() {
        return "hello";
    }
}