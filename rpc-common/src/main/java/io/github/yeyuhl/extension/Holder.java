package io.github.yeyuhl.extension;

/**
 * 用来保存共享变量
 *
 * @author yeyuhl
 * @since 2023/5/27
 */
public class Holder<T> {
    /**
     * volatile关键字保证当变量被修改时，其他线程能够立即看到修改的值
     */
    private volatile T value;

    public void set(T value) {
        this.value = value;
    }

    public T get() {
        return value;
    }
}
