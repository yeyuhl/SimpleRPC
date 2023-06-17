package io.github.yeyuhl.remoting.transport.netty.client;

import io.netty.channel.Channel;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 存放Channel，并且可以从这获取Channel
 *
 * @author yeyuhl
 * @since 2023/5/26
 */
@Slf4j
public class ChannelProvider {
    /**
     * 用Map存放Channel，key为地址，value为Channel
     */
    private final Map<String, Channel> channelMap;

    /**
     * 构造函数，为了线程安全使用ConcurrentHashMap
     */
    public ChannelProvider() {
        this.channelMap = new ConcurrentHashMap<>();
    }

    /**
     * 根据地址获取Channel
     *
     * @param address 地址
     * @return Channel
     */
    public Channel get(InetSocketAddress address) {
        String key = address.toString();
        if (channelMap.containsKey(key)) {
            Channel channel = channelMap.get(key);
            // 如果Channel不为空且为活跃状态（即可用），直接返回
            if (channel != null && channel.isActive()) {
                return channel;
            } else {
                // 否则从Map中移除
                channelMap.remove(key);
            }
        }
        return null;
    }

    /**
     * 存放Channel
     *
     * @param address 地址
     * @param channel Channel
     */
    public void set(InetSocketAddress address, Channel channel) {
        String key = address.toString();
        channelMap.put(key, channel);
    }

    /**
     * 移除Channel
     *
     * @param address 地址
     */
    public void remove(InetSocketAddress address) {
        String key = address.toString();
        channelMap.remove(key);
        log.info("Channel map size :[{}]", channelMap.size());
    }
}
