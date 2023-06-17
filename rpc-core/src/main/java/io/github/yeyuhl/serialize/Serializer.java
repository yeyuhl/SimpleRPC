package io.github.yeyuhl.serialize;

import io.github.yeyuhl.extension.SPI;

/**
 * 序列化接口
 *
 * @author yeyuhl
 * @since 2023/5/27
 */
@SPI
public interface Serializer {
    /**
     * 序列化
     *
     * @param obj 要序列化的对象
     * @return 序列化后的字节数组
     */
    byte[] serialize(Object obj);

    /**
     * 反序列化
     *
     * @param bytes 序列化后的字节数组
     * @param clazz 目标类
     * @return 反序列化后的对象
     */
    <T> T deserialize(byte[] bytes, Class<T> clazz);
}
