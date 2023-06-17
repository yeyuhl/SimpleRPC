package io.github.yeyuhl.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 测试用api的实体类
 *
 * @author yeyuhl
 * @since 2023/6/15
 */
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class Hello implements Serializable {
    private String message;
    private String description;
}
