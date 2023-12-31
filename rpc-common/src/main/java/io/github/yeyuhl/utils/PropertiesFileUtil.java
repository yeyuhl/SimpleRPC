package io.github.yeyuhl.utils;

import lombok.extern.slf4j.Slf4j;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

/**
 * 读取配置文件工具类
 *
 * @author yeyuhl
 * @since 2023/6/13
 */
@Slf4j
public final class PropertiesFileUtil {
    private PropertiesFileUtil(){

    }

    public static Properties readPropertiesFile(String fileName){
        // 从当前线程的类加载器中获取具有指定名称的资源
        URL url=Thread.currentThread().getContextClassLoader().getResource("");
        String rpcConfigPath="";
        if(url!=null){
            // 构造PropertiesFile的路径
            rpcConfigPath=url.getPath()+fileName;
        }
        Properties properties = null;
        try(InputStreamReader inputStreamReader = new InputStreamReader(new FileInputStream(rpcConfigPath), StandardCharsets.UTF_8)){
            properties= new Properties();
            properties.load(inputStreamReader);
        }catch(IOException e){
            log.error("occur exception when read properties file [{}]", fileName);
        }
        return properties;
    }
}
