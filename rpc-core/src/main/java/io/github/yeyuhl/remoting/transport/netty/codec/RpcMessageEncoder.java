package io.github.yeyuhl.remoting.transport.netty.codec;

import io.github.yeyuhl.compress.Compress;
import io.github.yeyuhl.enums.CompressTypeEnum;
import io.github.yeyuhl.enums.SerializationTypeEnum;
import io.github.yeyuhl.extension.ExtensionLoader;
import io.github.yeyuhl.remoting.constants.RpcConstants;
import io.github.yeyuhl.remoting.dto.RpcMessage;
import io.github.yeyuhl.serialize.Serializer;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * 自定义rpc消息编码器，将RpcMessage对象编码为ByteBuf对象，这样一来RPC消息可以通过网络传输
 * 0     1     2     3     4        5     6     7     8         9          10      11     12  13  14   15 16
 * +-----+-----+-----+-----+--------+----+----+----+------+-----------+-------+----- --+-----+-----+-------+
 * |   magic   code        |version | full length         | messageType| codec|compress|    RequestId       |
 * +-----------------------+--------+---------------------+-----------+-----------+-----------+------------+
 * |                                                                                                       |
 * |                                         body                                                          |
 * |                                                                                                       |
 * |                                        ... ...                                                        |
 * +-------------------------------------------------------------------------------------------------------+
 * 4B  magic code（魔法数）   1B version（版本）       4B full length（消息长度）    1B messageType（消息类型）
 * 1B compress（压缩类型）    1B codec（序列化类型）    4B  requestId（请求的Id）
 * body（object类型数据）
 *
 * @author yeyuhl
 * @since 2023/5/27
 */
@Slf4j
public class RpcMessageEncoder extends MessageToByteEncoder<RpcMessage> {
    // 线程安全的int，可以自增，相当于每个消息都能有自己的请求id
    private static final AtomicInteger ATOMIC_INTEGER = new AtomicInteger(0);

    @Override
    protected void encode(ChannelHandlerContext channelHandlerContext, RpcMessage rpcMessage, ByteBuf byteBuf) throws Exception {
        try {
            // 写入魔法数
            byteBuf.writeBytes(RpcConstants.MAGIC_NUMBER);
            // 写入版本号
            byteBuf.writeByte(RpcConstants.VERSION);
            // 写入消息长度，由于此时还不知道消息总长，因此先写入4个字节，占位
            byteBuf.writerIndex(byteBuf.writerIndex() + 4);
            // 写入消息类型
            byteBuf.writeByte(rpcMessage.getMessageType());
            // 写入序列化类型
            byteBuf.writeByte(rpcMessage.getCodec());
            // 写入压缩类型
            byteBuf.writeByte(CompressTypeEnum.GZIP.getCode());
            // 写入请求Id，这里使用自增的方式，每次请求都会自增
            byteBuf.writeInt(ATOMIC_INTEGER.getAndIncrement());
            // 构建消息长度
            byte[] bodyBytes = null;
            int fullLength = RpcConstants.HEAD_LENGTH;
            // 如果消息类型不是心跳相关消息，则消息长度=消息头长度+消息体长度
            if (rpcMessage.getMessageType() != RpcConstants.HEARTBEAT_REQUEST_TYPE
                    && rpcMessage.getMessageType() != RpcConstants.HEARTBEAT_RESPONSE_TYPE) {
                // 序列化
                String codecName = SerializationTypeEnum.getName(rpcMessage.getCodec());
                log.info("codec name: [{}] ", codecName);
                Serializer serializer = ExtensionLoader.getExtensionLoader(Serializer.class).getExtension(codecName);
                bodyBytes = serializer.serialize(rpcMessage.getData());
                // 对序列化后的数据压缩
                String compressName = CompressTypeEnum.getName(rpcMessage.getCompress());
                Compress compress = ExtensionLoader.getExtensionLoader(Compress.class).getExtension(compressName);
                bodyBytes = compress.compress(bodyBytes);
                // 消息长度增加
                fullLength += bodyBytes.length;
            }
            if (bodyBytes != null) {
                // 将rpcMessage的body数据写入byteBuf
                byteBuf.writeBytes(bodyBytes);
            }
            int writeIndex = byteBuf.writerIndex();
            byteBuf.writerIndex(writeIndex - fullLength + RpcConstants.MAGIC_NUMBER.length + 1);
            byteBuf.writeInt(fullLength);
            byteBuf.writerIndex(writeIndex);
        } catch (Exception e) {
            log.error("Encode request error!", e);
        }
    }
}
