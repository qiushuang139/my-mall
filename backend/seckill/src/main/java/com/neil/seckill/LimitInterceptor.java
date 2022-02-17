package com.neil.seckill;

import com.google.common.collect.ImmutableList;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import java.io.Serializable;
import java.lang.reflect.Method;


/**
 * @author :ZhangYi
 * @date :2022/2/16 17:00
 * @description:
 */
@Aspect
@Configuration
public class LimitInterceptor {
    private static final Logger logger= LoggerFactory.getLogger(LimitInterceptor.class);

    private static final String UNKNOWN="unknown";

    private final RedisTemplate<String, Serializable> limitRedisTemplate;

    @Autowired
    public LimitInterceptor(RedisTemplate<String, Serializable> limitRedisTemplate) {
        this.limitRedisTemplate = limitRedisTemplate;
    }

    @Around("execution(public * * (..))&&@annotation(com.neil.seckill.Limit)")
    public Object interceptor(ProceedingJoinPoint point) throws Throwable {
        MethodSignature signature=(MethodSignature) point.getSignature();
        Method method=signature.getMethod();
        Limit limitAnnotation=method.getAnnotation(Limit.class);
        LimitType limitType=limitAnnotation.limitType();
        String name=limitAnnotation.name();
        String key;
        int limitPeriod=limitAnnotation.period();
        int limitCount=limitAnnotation.count();
        switch (limitType){
            case IP:
                key=getIpAddress();
                break;
            case CUSTOMER:
                key=limitAnnotation.key();
                break;
            default:
                key= StringUtils.upperCase(method.getName());
        }
        ImmutableList<String> keys=ImmutableList.of(StringUtils.join(limitAnnotation.prefix(),key));
        try {
            String luaScript=buildLuaScript();
            RedisScript<Number> redisScript=new DefaultRedisScript<>(luaScript,Number.class);
            Number count=limitRedisTemplate.execute(redisScript,keys,limitCount,limitPeriod);
            logger.info("Access try count is {} for name={} and key={}",count,name,key);
            if(count!=null&&count.intValue()<=limitCount){
                return point.proceed();
            }else{
                throw new RuntimeException("You have been dragged into the blackList");
            }
        }catch (Throwable e){
            if(e instanceof RuntimeException){
                throw new RuntimeException(e.getLocalizedMessage());
            }
            throw new RuntimeException("server exception");
        }
    }

    public String buildLuaScript(){
        StringBuilder lua = new StringBuilder();
        lua.append("local c");
        lua.append("\nc = redis.call('get',KEYS[1])");
        // 调用不超过最大值，则直接返回
        lua.append("\nif c and tonumber(c) > tonumber(ARGV[1]) then");
        lua.append("\nreturn c;");
        lua.append("\nend");
        // 执行计算器自加
        lua.append("\nc = redis.call('incr',KEYS[1])");
        lua.append("\nif tonumber(c) == 1 then");
        // 从第一次调用开始限流，设置对应键值的过期
        lua.append("\nredis.call('expire',KEYS[1],ARGV[2])");
        lua.append("\nend");
        lua.append("\nreturn c;");
        return lua.toString();
    }

    public String getIpAddress(){
        HttpServletRequest request=((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
        String ip=request.getHeader("x-forwarded-for");
        if(ip==null||ip.length()==0||UNKNOWN.equalsIgnoreCase(ip)){
            ip=request.getHeader("Proxy-Client-IP");
        }
        if(ip==null||ip.length()==0||UNKNOWN.equalsIgnoreCase(ip)){
            ip=request.getHeader("WL-Proxy-Client-IP");
        }
        if(ip==null||ip.length()==0||UNKNOWN.equalsIgnoreCase(ip)){
            ip=request.getRemoteAddr();
        }
        return ip;
    }
}
