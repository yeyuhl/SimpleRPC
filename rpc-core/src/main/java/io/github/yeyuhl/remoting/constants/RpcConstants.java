package io.github.yeyuhl.remoting.constants;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * rpc常量类
 *
 * @author yeyuhl
 * @since 2023/5/26
 */
public class RpcConstants {
    /**
     * 魔数，验证RpcMessage
     */
    public static final byte[] MAGIC_NUMBER = {(byte) 'g', (byte) 'r', (byte) 'p', (byte) 'c'};

    /**
     * 默认字符编码
     */
    public static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;

    /**
     * 版本号
     */
    public static final byte VERSION = 1;

    /**
     * RPC消息长度
     */
    public static final byte TOTAL_LENGTH = 16;

    /**
     * RPC请求的类型
     */
    public static final byte REQUEST_TYPE = 1;

    /**
     * RPC响应的类型
     */
    public static final byte RESPONSE_TYPE = 2;

    /**
     * 心跳请求，ping
     */
    public static final byte HEARTBEAT_REQUEST_TYPE = 3;

    /**
     * 心跳响应，pong
     */
    public static final byte HEARTBEAT_RESPONSE_TYPE = 4;

    /**
     * rpc消息头部长度
     */
    public static final int HEAD_LENGTH = 16;

    /**
     * 用于表示检测信号请求的字符串
     */
    public static final String PING = "ping";

    /**
     * 用于表示检测信号响应的字符串
     */
    public static final String PONG = "pong";

    /**
     * RPC帧的最大长度
     */
    public static final int MAX_FRAME_LENGTH = 8 * 1024 * 1024;
}
