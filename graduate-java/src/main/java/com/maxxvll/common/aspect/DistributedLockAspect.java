package com.maxxvll.common.aspect;

import com.maxxvll.common.annotation.DistributedLock;
import com.maxxvll.common.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.core.annotation.Order;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * 分布式锁切面
 * 拦截标记了 @DistributedLock 注解的方法，自动获取和释放锁
 *
 * @author backend-friend
 */
@Slf4j
@Aspect
@Component
@Order(1) // 确保在事务切面之前执行
public class DistributedLockAspect {

    private final RedissonClient redissonClient;
    private final ParameterNameDiscoverer parameterNameDiscoverer = new DefaultParameterNameDiscoverer();
    private final ExpressionParser parser = new SpelExpressionParser();

    public DistributedLockAspect(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    @Around("@annotation(com.maxxvll.common.annotation.DistributedLock)")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        DistributedLock distributedLock = signature.getMethod().getAnnotation(DistributedLock.class);

        String lockKey = buildLockKey(distributedLock, signature, joinPoint.getArgs());
        RLock lock = redissonClient.getLock(lockKey);

        boolean acquired = false;
        try {
            acquired = lock.tryLock(
                    distributedLock.waitTime(),
                    distributedLock.leaseTime(),
                    distributedLock.timeUnit()
            );

            if (!acquired) {
                throw new BusinessException(429, distributedLock.message());
            }

            log.debug("分布式锁获取成功, key={}", lockKey);
            return joinPoint.proceed();

        } catch (BusinessException e) {
            throw e;
        } catch (Throwable e) {
            log.error("分布式锁执行异常, key={}", lockKey, e);
            throw e;
        } finally {
            if (acquired && lock.isHeldByCurrentThread()) {
                lock.unlock();
                log.debug("分布式锁释放成功, key={}", lockKey);
            }
        }
    }

    private String buildLockKey(DistributedLock distributedLock, MethodSignature signature, Object[] args) {
        String key = distributedLock.key();
        String prefix = distributedLock.prefix();

        // 如果不包含 SpEL 表达式，直接返回
        if (!key.contains("#")) {
            return prefix + key;
        }

        // 解析 SpEL 表达式
        EvaluationContext context = new StandardEvaluationContext();
        String[] parameterNames = parameterNameDiscoverer.getParameterNames(signature.getMethod());

        if (parameterNames != null) {
            for (int i = 0; i < parameterNames.length; i++) {
                context.setVariable(parameterNames[i], args[i]);
            }
        }

        Expression expression = parser.parseExpression(key);
        String resolvedKey = expression.getValue(context, String.class);

        return prefix + resolvedKey;
    }
}
