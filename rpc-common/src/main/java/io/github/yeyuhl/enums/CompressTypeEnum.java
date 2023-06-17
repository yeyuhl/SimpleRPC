package io.github.yeyuhl.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 压缩类型枚举
 *
 * @author yeyuhl
 * @since 2023/5/27
 */
@AllArgsConstructor
@Getter
public enum CompressTypeEnum {
    /**
     * 采用gzip压缩
     */
    GZIP((byte) 0x01, "gzip");

    private final byte code;
    private final String name;

    public static String getName(byte code) {
        for (CompressTypeEnum c : CompressTypeEnum.values()) {
            if (c.getCode() == code) {
                return c.name;
            }
        }
        return null;
    }
}
