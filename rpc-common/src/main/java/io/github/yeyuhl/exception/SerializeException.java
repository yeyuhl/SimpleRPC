package io.github.yeyuhl.exception;

/**
 * 自定义序列化异常
 *
 * @author yeyuhl
 * @since 2023/5/27
 */
public class SerializeException extends RuntimeException {
    public SerializeException(String msg) {
        super(msg);
    }
}
