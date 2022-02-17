package com.neil.seckill;

import java.lang.annotation.*;

/**
 * @author :ZhangYi
 * @date :2022/2/16 16:58
 * @description:
 */
@Target({ElementType.METHOD,ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
public @interface Limit {
    String name() default "";

    String key() default "";

    String prefix() default "";

    //    给定的前缀范围
    int period();

    //  访问次数
    int count();

    LimitType limitType() default LimitType.CUSTOMER;
}
