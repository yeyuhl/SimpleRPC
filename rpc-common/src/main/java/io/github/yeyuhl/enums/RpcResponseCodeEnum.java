package io.github.yeyuhl.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

/**
 * rpc响应状态码
 *
 * @author yeyuhl
 * @since 2023/5/26
 */
@AllArgsConstructor
@ToString
@Getter
public enum RpcResponseCodeEnum {
    SUCCESS(200, "The remote call is successful"),
    FAIL(500, "The remote call is fail");
    private final int code;
    private final String message;
}
