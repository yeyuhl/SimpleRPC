package io.github.yeyuhl.serialize.hessian;

import com.caucho.hessian.io.HessianInput;
import com.caucho.hessian.io.HessianOutput;
import io.github.yeyuhl.exception.SerializeException;
import io.github.yeyuhl.serialize.Serializer;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

/**
 * Hessian序列化类
 *
 * @author yeyuhl
 * @since 2023/5/27
 */
public class HessianSerializer implements Serializer {
    @Override
    public byte[] serialize(Object obj) {
        try(ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream()) {
            // HessianOutput是Hessian序列化的入口，它的构造函数需要传入一个OutputStream对象
            HessianOutput hessianOutput = new HessianOutput(byteArrayOutputStream);
            // writeObject()方法将对象序列化为字节数组，并将其写入到HessianOutput中，实际上写到ByteArrayOutputStream中
            hessianOutput.writeObject(obj);
            return byteArrayOutputStream.toByteArray();
        } catch (Exception e) {
            throw new SerializeException("Serialization failed");
        }
    }

    @Override
    public <T> T deserialize(byte[] bytes, Class<T> clazz) {
        try (ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(bytes)) {
            // 同理，HessianInput的构造函数需要传入一个InputStream对象
            HessianInput hessianInput = new HessianInput(byteArrayInputStream);
            // readObject()方法从HessianInput中读取字节数组，并反序列化为一个对象
            Object o = hessianInput.readObject();
            // 将对象转换为指定类型的对象
            return clazz.cast(o);
        } catch (Exception e) {
            throw new SerializeException("Deserialization failed");
        }
    }
}
