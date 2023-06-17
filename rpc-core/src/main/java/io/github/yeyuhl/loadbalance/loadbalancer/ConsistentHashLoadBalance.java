package io.github.yeyuhl.loadbalance.loadbalancer;

import io.github.yeyuhl.loadbalance.AbstractLoadBalance;
import io.github.yeyuhl.remoting.dto.RpcRequest;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 一致性哈希负载均衡策略
 * 参考自Dubbo的一致性哈希负载均衡策略实现
 *
 * @author yeyuhl
 * @since 2023/6/14
 */
public class ConsistentHashLoadBalance extends AbstractLoadBalance {
    private final ConcurrentHashMap<String, ConsistentHashSelector> selectors = new ConcurrentHashMap<>();

    @Override
    protected String doSelect(List<String> serviceAddresses, RpcRequest rpcRequest) {
        int identityHashCode = System.identityHashCode(serviceAddresses);
        String rpcServiceName = rpcRequest.getRpcServiceName();
        ConsistentHashSelector selector = selectors.get(rpcServiceName);
        // 如果没有对应的selector，则创建一个
        if(selector == null || selector.identityHashCode != identityHashCode) {
            selectors.put(rpcServiceName, new ConsistentHashSelector(serviceAddresses, 160, identityHashCode));
            selector = selectors.get(rpcServiceName);
        }
        return selector.select(rpcServiceName + Arrays.stream(rpcRequest.getParameters()));
    }

    static class ConsistentHashSelector {
        // 使用TreeMap存储虚拟节点，key为虚拟节点的hash值，value为虚拟节点的名称
        private final TreeMap<Long, String> virtualInvokers;
        // 节点的身份hash值
        private final int identityHashCode;

        // 构造函数，参数为：虚拟节点列表，虚拟节点的个数，节点的身份hash值
        ConsistentHashSelector(List<String> invokers, int replicaNumber, int identityHashCode) {
            this.virtualInvokers = new TreeMap<>();
            this.identityHashCode = identityHashCode;

            for (String invoker : invokers) {
                for (int i = 0; i < replicaNumber / 4; i++) {
                    // 使用MD5算法计算invoker+i的hash值，得到一个长度为16的字节数组
                    byte[] digest = md5(invoker + i);
                    // 对 digest 部分字节进行4次hash运算，得到四个不同的long型正整数
                    for (int h = 0; h < 4; h++) {
                        // h=0时，取digest中下标为0 ~ 3的4个字节进行位运算
                        // h=1时，取digest中下标为4 ~ 7的4个字节进行位运算
                        // h=2，h=3时过程同上
                        long m = hash(digest, h);
                        // 将hash到invoker的映射关系存储到virtualInvokers中，
                        // virtualInvokers需要提供高效的查询操作，因此选用TreeMap作为存储结构
                        virtualInvokers.put(m, invoker);
                    }
                }
            }
        }

        static byte[] md5(String key) {
            MessageDigest md;
            try {
                md = MessageDigest.getInstance("MD5");
                byte[] bytes = key.getBytes(StandardCharsets.UTF_8);
                // 对bytes进行一次MD5的hash计算，得到长度为16的字节数组
                md.update(bytes);
            } catch (NoSuchAlgorithmException e) {
                throw new IllegalStateException(e.getMessage(), e);
            }
            return md.digest();
        }

        static long hash(byte[] digest, int number) {
            return (((long) (digest[3 + number * 4] & 0xFF) << 24)
                    | ((long) (digest[2 + number * 4] & 0xFF) << 16)
                    | ((long) (digest[1 + number * 4] & 0xFF) << 8)
                    | (digest[number * 4] & 0xFF))
                    & 0xFFFFFFFFL;
        }

        public String select(String rpcServiceKey) {
            byte[] digest = md5(rpcServiceKey);
            // 取digest数组的前四个字节进行hash运算，再将hash值传给selectForKey方法，得到对应的虚拟节点
            return selectForKey(hash(digest, 0));
        }

        public String selectForKey(long hashCode) {
            // 取大于或等于hashCode的第一个虚拟节点
            Map.Entry<Long, String> entry = virtualInvokers.ceilingEntry(hashCode);
            // 如果hash大于Invoker在圆环上最大的位置，此时entry=null，
            // 需要将TreeMap的头节点赋值给 entry
            if (entry == null) {
                entry = virtualInvokers.firstEntry();
            }
            // 返回对应的虚拟节点
            return entry.getValue();
        }

    }
}
