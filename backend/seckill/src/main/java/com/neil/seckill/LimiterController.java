package com.neil.seckill;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author :ZhangYi
 * @date :2022/2/16 17:36
 * @description:
 */
@RestController
public class LimiterController {
    private static final AtomicInteger ATOMIC_INTEGER_1=new AtomicInteger();
    private static final AtomicInteger ATOMIC_INTEGER_2=new AtomicInteger();
    private static final AtomicInteger ATOMIC_INTEGER_3=new AtomicInteger();

    @Limit(key="limitTest",period = 10,count = 3)
    @GetMapping("/limitTest1")
    public int testLimit1(){
        return ATOMIC_INTEGER_1.incrementAndGet();
    }

    @Limit(key = "customer_limit_test",period = 10,count = 3,limitType = LimitType.CUSTOMER)
    @GetMapping("/limitTest2")
    public int testLimit2(){
        return ATOMIC_INTEGER_2.incrementAndGet();
    }

    @Limit(key = "ip_limit_test",period = 10,count = 3,limitType = LimitType.IP)
    @GetMapping("/limitTest3")
    public int testLimit3(){
        return ATOMIC_INTEGER_3.incrementAndGet();
    }
}
