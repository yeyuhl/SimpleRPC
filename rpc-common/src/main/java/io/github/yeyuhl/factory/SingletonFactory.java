package io.github.yeyuhl.factory;

import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 获取单例对象的工厂类
 *
 * @author yeyuhl
 * @since 2023/5/26
 */
public final class SingletonFactory {
    private static final Map<String, Object> OBJECT_MAP = new ConcurrentHashMap<>();

    private SingletonFactory() {
    }

    /**
     * 根据Class获取单例对象
     *
     * @param c Class
     * @param <T>   泛型
     * @return 单例对象
     */
    public static <T> T getInstance(Class<T> c) {
        if (c == null) {
            throw new IllegalArgumentException();
        }
        String key = c.toString();
        // 如果key存在
        if (OBJECT_MAP.containsKey(key)) {
            // cast方法用于将Object类型强制转换为T类型，如果不行则返回null
            return c.cast(OBJECT_MAP.get(key));
        } else {
            // 如果key不存在，则将key和c的实例存入Map中
            return c.cast(OBJECT_MAP.computeIfAbsent(key, k -> {
                try {
                    // 使用带有参数的构造函数创建实例并返回
                    return c.getDeclaredConstructor().newInstance();
                } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                    throw new RuntimeException(e.getMessage(), e);
                }
            }));
        }
    }
}
