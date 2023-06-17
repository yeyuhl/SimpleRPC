package io.github.yeyuhl.remoting.transport.netty.codec;

import io.github.yeyuhl.compress.Compress;
import io.github.yeyuhl.enums.CompressTypeEnum;
import io.github.yeyuhl.enums.SerializationTypeEnum;
import io.github.yeyuhl.extension.ExtensionLoader;
import io.github.yeyuhl.remoting.constants.RpcConstants;
import io.github.yeyuhl.remoting.dto.RpcMessage;
import io.github.yeyuhl.remoting.dto.RpcRequest;
import io.github.yeyuhl.remoting.dto.RpcResponse;
import io.github.yeyuhl.serialize.Serializer;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;

/**
 * 自定义rpc消息解码器
 * 0     1     2     3     4        5     6     7     8         9          10      11     12  13  14   15 16
 * +-----+-----+-----+-----+--------+----+----+----+------+-----------+-------+----- --+-----+-----+-------+
 * |   magic   code        |version | full length         | messageType| codec|compress|    RequestId       |
 * +-----------------------+--------+---------------------+-----------+-----------+-----------+------------+
 * |                                                                                                       |
 * |                                         body                                                          |
 * |                                                                                                       |
 * |                                        ... ...                                                        |
 * +-------------------------------------------------------------------------------------------------------+
 * 4B  magic code（魔法数）   1B version（版本）   4B full length（消息长度）    1B messageType（消息类型）
 * 1B compress（压缩类型） 1B codec（序列化类型）    4B  requestId（请求的Id）
 * body（object类型数据）
 * LengthFieldBasedFrameDecoder是一个基于长度解码器, 它是Netty提供的4个解码器中使用最广泛的一个解码器
 *
 * @author yeyuhl
 * @since 2023/6/10
 */

@Slf4j
public class RpcMessageDecoder extends LengthFieldBasedFrameDecoder {
    public RpcMessageDecoder() {
        // maxFrameLength：消息帧的最大长度。如果帧的长度大于此值，则数据将被抛弃。
        // lengthFieldOffset: 长度域的偏移量。由于魔法数占4B，版本号占1B，然后是消息长度，所以值是5。
        // lengthFieldLength：长度域的长度。由于消息长度占4B，所以值是4。
        // lengthAdjustment：长度域修正值。Netty读取完长度域后，就会接着读取后续报文(我们称读取完长度域后剩下的所有的报文为后续报文)
        // 当后续报文和长度域的值相等时，则不需要修正，lengthAdjustment=0。如果不等时，则需要修正，lengthAdjustment(可为负)+长度域的值=后续报文长度
        // initialBytesToStrip：要跳过的初始字节数。如果接收所有的标题+正文数据，这个值是0；如果只接收正文数据，那么需要跳过标题所消耗的字节数。
        this(RpcConstants.MAX_FRAME_LENGTH, 5, 4, -9, 0);
    }

    public RpcMessageDecoder(int maxFrameLength, int lengthFieldOffset, int lengthFieldLength,
                             int lengthAdjustment, int initialBytesToStrip) {
        super(maxFrameLength, lengthFieldOffset, lengthFieldLength, lengthAdjustment, initialBytesToStrip);
    }

    /**
     * 将ByteBuf对象解码为RpcMessage对象
     */
    @Override
    protected Object decode(ChannelHandlerContext channelHandlerContext, ByteBuf byteBuf) throws Exception {
        Object decoded = super.decode(channelHandlerContext, byteBuf);
        if (decoded instanceof ByteBuf) {
            ByteBuf frame = (ByteBuf) decoded;
            // 检查ByteBuf对象是否有足够的可读字节来解码一个完整的RPC消息
            if (frame.readableBytes() >= RpcConstants.TOTAL_LENGTH) {
                try {
                    // 如果有，则调用decodeFrame()方法对其进行解码
                    return decodeFrame(frame);
                } catch (Exception e) {
                    log.error("Decode frame error!", e);
                    throw e;
                } finally {
                    // 最后要释放ByteBuf对象
                    frame.release();
                }
            }
        }
        return decoded;
    }

    /**
     * 解码
     */
    private Object decodeFrame(ByteBuf in) {
        // 检查魔法数
        checkMagicNumber(in);
        // 检查版本号
        checkVersion(in);
        // 读取消息长度
        int fullLength = in.readInt();
        // 读取消息类型
        byte messageType = in.readByte();
        // 读取序列化类型
        byte codecType = in.readByte();
        // 读取压缩类型
        byte compressType = in.readByte();
        // 读取请求Id
        int requestId = in.readInt();
        // 创建RpcMessage对象
        RpcMessage rpcMessage = RpcMessage.builder()
                .messageType(messageType)
                .codec(codecType)
                .compress(compressType)
                .requestId(requestId)
                .build();
        // 如果消息类型是心跳消息，则直接返回RpcMessage对象
        if (messageType == RpcConstants.HEARTBEAT_REQUEST_TYPE) {
            rpcMessage.setData(RpcConstants.PING);
            return rpcMessage;
        }
        if (messageType == RpcConstants.HEARTBEAT_RESPONSE_TYPE) {
            rpcMessage.setData(RpcConstants.PONG);
            return rpcMessage;
        }
        // 如果消息类型是其他消息，则需要读取附加信息
        int bodyLength = fullLength - RpcConstants.HEAD_LENGTH;
        if (bodyLength > 0) {
            // 读取附加信息
            byte[] bs = new byte[bodyLength];
            in.readBytes(bs);
            // 解压
            String compressName = CompressTypeEnum.getName(compressType);
            Compress compress = ExtensionLoader.getExtensionLoader(Compress.class).getExtension(compressName);
            bs = compress.decompress(bs);
            // 反序列化
            String codecName = SerializationTypeEnum.getName(codecType);
            log.info("codecName: [{}]", codecName);
            Serializer serializer = ExtensionLoader.getExtensionLoader(Serializer.class).getExtension(codecName);
            if (messageType == RpcConstants.REQUEST_TYPE) {
                // 如果消息类型是请求消息，则需要读取请求信息
                RpcRequest tmpValue = serializer.deserialize(bs, RpcRequest.class);
                rpcMessage.setData(tmpValue);
            } else {
                // 如果消息类型是响应消息，则需要读取响应信息
                RpcResponse tmpValue = serializer.deserialize(bs, RpcResponse.class);
                rpcMessage.setData(tmpValue);
            }
        }
        return rpcMessage;
    }

    /**
     * 检查版本号
     */
    private void checkVersion(ByteBuf in) {
        byte version = in.readByte();
        if (version != RpcConstants.VERSION) {
            throw new IllegalArgumentException("version is not compatible:" + version);
        }
    }

    /**
     * 检查魔法数
     */
    private void checkMagicNumber(ByteBuf in) {
        // 读取魔法数长度
        int len = RpcConstants.MAGIC_NUMBER.length;
        byte[] bytes = new byte[len];
        in.readBytes(bytes);
        // 比较魔法数
        for (int i = 0; i < len; i++) {
            if (bytes[i] != RpcConstants.MAGIC_NUMBER[i]) {
                throw new IllegalArgumentException("Unknown magic code:" + Arrays.toString(bytes));
            }
        }
    }


}
