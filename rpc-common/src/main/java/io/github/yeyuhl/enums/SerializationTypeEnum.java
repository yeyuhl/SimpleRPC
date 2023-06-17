package io.github.yeyuhl.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 序列化类型枚举
 *
 * @author yeyuhl
 * @since 2023/5/27
 */
@AllArgsConstructor
@Getter
public enum SerializationTypeEnum {
    /**
     * 采用kryo序列化
     */
    KRYO((byte) 0x01, "kryo"),
    /**
     * 采用protostuff序列化
     */
    PROTOSTUFF((byte) 0x02, "protostuff"),
    /**
     * 采用hessian序列化
     */
    HESSIAN((byte) 0x03, "hessian");


    private final byte code;
    private final String name;

    public static String getName(byte code) {
        for (SerializationTypeEnum c : SerializationTypeEnum.values()) {
            if (c.getCode() == code) {
                return c.name;
            }
        }
        return null;
    }
}
