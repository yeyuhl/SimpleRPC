package io.github.yeyuhl.compress.gzip;

import io.github.yeyuhl.compress.Compress;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * gzip压缩实现类
 *
 * @author yeyuhl
 * @since 2023/5/27
 */
public class GzipCompress implements Compress {
    private static final int BUFFER_SIZE = 1024 * 4;

    /**
     * gzip压缩
     *
     * @param bytes 压缩前的字节数组
     * @return 压缩后的字节数组
     */
    @Override
    public byte[] compress(byte[] bytes) {
        // 如果bytes为空，抛出空指针异常
        if (bytes == null) {
            throw new NullPointerException();
        }
        // ByteArrayOutputStream 对象将用于存储压缩数据
        // GZIPOutputStream 对象将用于压缩数据
        try (ByteArrayOutputStream out = new ByteArrayOutputStream();
             GZIPOutputStream gzip = new GZIPOutputStream(out)) {
            // 将bytes写入gzip
            gzip.write(bytes);
            // 刷新gzip并将其缓冲流的数据写入底层流
            gzip.flush();
            // 在不关闭基础流的情况下完成将压缩数据写入输出流
            gzip.finish();
            // 输出流的当前内容作为字节数组并返回
            return out.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("gzip compress error", e);
        }
    }

    /**
     * gzip解压
     *
     * @param bytes 压缩后的字节数组
     * @return 解压后的字节数组
     */
    @Override
    public byte[] decompress(byte[] bytes) {
        if (bytes == null) {
            throw new NullPointerException();
        }
        // ByteArrayOutputStream 对象将用于存储解压数据
        // GZIPInputStream 对象将用于解压数据
        try (ByteArrayOutputStream out = new ByteArrayOutputStream();
             GZIPInputStream gzip = new GZIPInputStream(new ByteArrayInputStream(bytes))) {
            byte[] buffer = new byte[BUFFER_SIZE];
            int n;
            // gzip.read(buffer)将解压数据读入buffer并返回读入的字节数
            while ((n = gzip.read(buffer)) >= 0) {
                // 将buffer中的n个字节写入输出流
                out.write(buffer, 0, n);
            }
            return out.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("gzip decompress error", e);
        }
    }
}
