package io.github.yeyuhl.compress;

import io.github.yeyuhl.extension.SPI;

/**
 * 压缩解压接口
 *
 * @author yeyuhl
 * @since 2023/5/27
 */
@SPI
public interface Compress {
    byte[] compress(byte[] bytes);

    byte[] decompress(byte[] bytes);
}
