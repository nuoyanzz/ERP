package com.erp.system.aspect;

import com.erp.security.LoginUser;
import com.erp.security.SecurityUtils;
import com.erp.system.annotation.OperLog;
import com.erp.system.domain.entity.SysOperLog;
import com.erp.system.mapper.SysOperLogMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Arrays;

/**
 * 操作日志切面。记录写操作的请求参数、结果与耗时。
 * <p>
 * 日志写入使用独立事务，失败不影响业务操作。
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class OperLogAspect {

    private static final int MAX_TEXT_LENGTH = 2000;

    private final SysOperLogMapper operLogMapper;
    private final ObjectMapper objectMapper;

    @Around("@annotation(com.erp.system.annotation.OperLog)")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        long start = System.currentTimeMillis();
        Object result = null;
        Throwable error = null;
        try {
            result = joinPoint.proceed();
            return result;
        } catch (Throwable throwable) {
            error = throwable;
            throw throwable;
        } finally {
            try {
                saveLog(joinPoint, result, error, System.currentTimeMillis() - start);
            } catch (Exception e) {
                log.warn("记录操作日志失败: {}", e.getMessage());
            }
        }
    }

    private void saveLog(ProceedingJoinPoint joinPoint, Object result, Throwable error, long costTime) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        OperLog annotation = signature.getMethod().getAnnotation(OperLog.class);
        if (annotation == null) {
            return;
        }

        SysOperLog operLog = new SysOperLog();
        operLog.setModule(annotation.module());
        operLog.setOperType(annotation.type());
        operLog.setOperDesc(annotation.value());
        operLog.setMethod(signature.getMethod().getName());
        operLog.setParams(truncate(toJson(joinPoint.getArgs())));
        operLog.setResult(truncate(toJson(result)));
        operLog.setCostTime(costTime);
        operLog.setSuccess(error == null ? 1 : 0);
        operLog.setErrorMsg(error == null ? null : truncate(error.getMessage()));

        LoginUser loginUser = SecurityUtils.getCurrentUser();
        if (loginUser != null) {
            operLog.setOperBy(loginUser.getUserId());
            operLog.setOperUsername(loginUser.getUsername());
        }
        operLog.setOperIp(currentIp());

        operLogMapper.insert(operLog);
    }

    private String currentIp() {
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return null;
        }
        String forwarded = attributes.getRequest().getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return attributes.getRequest().getRemoteAddr();
    }

    private String toJson(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            return String.valueOf(value);
        }
    }

    private String truncate(String text) {
        if (text == null) {
            return null;
        }
        return text.length() <= MAX_TEXT_LENGTH ? text : text.substring(0, MAX_TEXT_LENGTH);
    }
}








