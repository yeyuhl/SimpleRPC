package io.github.yeyuhl.extension;

import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ExtensionLoader是Dubbo框架的一个核心类，负责加载和管理扩展
 * 扩展就是Dubbo注册的接口的实现类
 *
 * @author yeyuhl
 * @since 2023/6/10
 */
@Slf4j
public final class ExtensionLoader<T> {
    /**
     * 放置自定义扩展文件的配置目录
     */
    private static final String SERVICE_DIRECTORY = "META-INF/extensions/";
    /**
     * key:扩展类的抽象类型的class(接口) value:扩展类型对应的ExtensionLoader
     */
    private static final Map<Class<?>, ExtensionLoader<?>> EXTENSION_LOADERS = new ConcurrentHashMap<>();
    /**
     * key:扩展类的class(实现类) value:扩展类型对应的具体实例
     */
    private static final Map<Class<?>, Object> EXTENSION_INSTANCES = new ConcurrentHashMap<>();

    /**
     * 需要扩展的Class的类型(Extension type必须是接口)
     */
    private final Class<?> type;
    /**
     * key:扩展类名字 value:扩展类型对应的具体实例
     * EXTENSION_LOADERS存储的value是全局的，而cachedInstances是本对象的
     * EXTENSION_LOADERS是为了防止某个类实现了不同类型的扩展导致加载时，全局产生这个类的两个instance
     */
    private final Map<String, Holder<Object>> cachedInstances = new ConcurrentHashMap<>();
    /**
     * 缓存当前这个扩展类型下，适配器类型的instance
     */
    private final Holder<Map<String, Class<?>>> cachedClasses = new Holder<>();

    private ExtensionLoader(Class<?> type) {
        this.type = type;
    }

    /**
     * 根据type类型来获取ExtensionLoader对象
     * 如果还未加载，则new ExtensionLoader<T>(type)
     */
    public static <S> ExtensionLoader<S> getExtensionLoader(Class<S> type) {
        if (type == null) {
            throw new IllegalArgumentException("Extension type should not be null.");
        }
        if (!type.isInterface()) {
            throw new IllegalArgumentException("Extension type must be an interface.");
        }
        if (type.getAnnotation(SPI.class) == null) {
            throw new IllegalArgumentException("Extension type must be annotated by @SPI");
        }
        // 如果在EXTENSION_LOADERS中存在，则直接返回，没有则new
        ExtensionLoader<S> extensionLoader = (ExtensionLoader<S>) EXTENSION_LOADERS.get(type);
        if (extensionLoader == null) {
            EXTENSION_LOADERS.putIfAbsent(type, new ExtensionLoader<S>(type));
            extensionLoader = (ExtensionLoader<S>) EXTENSION_LOADERS.get(type);
        }
        return extensionLoader;
    }


    /**
     * 用于从缓存中获取与拓展类对应的ExtensionLoader，若缓存未命中，则创建一个新的实例
     */
    public T getExtension(String name) {
        if (StrUtil.isBlank(name)) {
            throw new IllegalArgumentException("Extension name should not be null or empty.");
        }
        // 从缓存中获取holder, 如果不命中则新建holder
        Holder<Object> holder = cachedInstances.get(name);
        if (holder == null) {
            cachedInstances.putIfAbsent(name, new Holder<>());
            holder = cachedInstances.get(name);
        }
        // 从holder中获取实例，如果不命中则新建实例
        Object instance = holder.get();
        // 双重检查
        if (instance == null) {
            synchronized (holder) {
                instance = holder.get();
                if (instance == null) {
                    // 创建拓展实例
                    instance = createExtension(name);
                    // 设置实例到holder中
                    holder.set(instance);
                }
            }
        }
        return (T) instance;
    }

    /**
     * 创建扩展对象
     */
    private T createExtension(String name) {
        // 从文件中加载类型T的所有扩展类并按名称获取特定的扩展类
        Class<?> clazz = getExtensionClasses().get(name);
        if (clazz == null) {
            throw new RuntimeException("No such extension of name " + name);
        }
        T instance = (T) EXTENSION_INSTANCES.get(clazz);
        if (instance == null) {
            try {
                // 通过反射创建实例
                EXTENSION_INSTANCES.putIfAbsent(clazz, clazz.newInstance());
                instance = (T) EXTENSION_INSTANCES.get(clazz);
            } catch (Exception e) {
                log.error(e.getMessage());
            }
        }
        return instance;
    }

    /**
     * 根据配置文件解析出扩展项名称到扩展类的映射关系表（Map<名称, 扩展类>）
     * 之后再根据扩展项名称从映射关系表中取出相应的扩展类即可
     */
    private Map<String, Class<?>> getExtensionClasses() {
        // 从缓存中获取已加载的扩展类
        Map<String, Class<?>> classes = cachedClasses.get();
        // 双重检查
        if (classes == null) {
            synchronized (cachedClasses) {
                classes = cachedClasses.get();
                if (classes == null) {
                    classes=new HashMap<>();
                    // 从配置文件中加载拓展类
                    loadDirectory(classes);
                    cachedClasses.set(classes);
                }
            }
        }
        return classes;
    }

    /**
     * 加载指定文件夹配置文件
     */
    private void loadDirectory(Map<String, Class<?>> extensionClasses) {
        String finalName = ExtensionLoader.SERVICE_DIRECTORY + type.getName();
        try {
            Enumeration<URL> urls;
            ClassLoader classLoader = ExtensionLoader.class.getClassLoader();
            // 根据文件名加载所有的同名文件
            urls = classLoader.getResources(finalName);
            if (urls != null) {
                while (urls.hasMoreElements()) {
                    URL resourceUrl = urls.nextElement();
                    // 加载资源
                    loadResource(extensionClasses, classLoader, resourceUrl);
                }
            }
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }

    /**
     * 加载资源
     */
    private void loadResource(Map<String, Class<?>> extensionClasses, ClassLoader classLoader, URL resourceURL) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(resourceURL.openStream(), "utf-8"))) {
            String line;
            // 按行读取配置内容
            while ((line = reader.readLine()) != null) {
                // 定位 # 字符
                final int ci = line.indexOf('#');
                if (ci >= 0) {
                    // 截取 # 之前的字符串，# 之后的内容为注释，需要忽略
                    line = line.substring(0, ci);
                }
                line = line.trim();
                if (line.length() > 0) {
                    try {
                        int i = line.indexOf('=');
                        // 以等于号 = 为界，截取键与值
                        String name = line.substring(0, i).trim();
                        String clazzName = line.substring(i + 1).trim();
                        // 均不能为空
                        if (name.length() > 0 && clazzName.length() > 0) {
                            // 加载类，并通过 loadClass 方法对类进行缓存
                            Class<?> clazz = classLoader.loadClass(clazzName);
                            extensionClasses.put(name, clazz);
                        }
                    } catch (ClassNotFoundException e) {
                        log.error(e.getMessage());
                    }
                }
            }
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }

}
