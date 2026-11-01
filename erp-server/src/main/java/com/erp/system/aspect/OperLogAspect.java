package com.erp.system.aspect;

import com.erp.security.LoginUser;
import com.erp.security.SecurityUtils;
import com.erp.system.annotation.OperLog;
import com.erp.system.domain.entity.SysOperLog;
import com.erp.system.mapper.SysOperLogMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;

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

    /**
     * 需要脱敏的字段名。命中这些名字的参数在写日志前替换为掩码，
     * 避免明文口令、令牌落进操作日志表。
     */
    private static final Set<String> SENSITIVE_FIELDS = Set.of(
            "password", "oldpassword", "newpassword", "confirmpassword",
            "passwd", "pwd", "secret", "token", "accesstoken", "refreshtoken"
    );

    /** 脱敏后的占位值 */
    private static final String MASK = "******";

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
            return objectMapper.writeValueAsString(maskSensitive(value));
        } catch (Exception e) {
            return String.valueOf(value);
        }
    }

    /**
     * 脱敏敏感字段。
     * <p>
     * 请求参数以 POJO 或 Map 形式传入，这里先转成树形结构，
     * 递归替换掉命中敏感字段名的值，再序列化。
     */
    private Object maskSensitive(Object value) {
        try {
            JsonNode node = objectMapper.valueToTree(value);
            maskNode(node);
            return node;
        } catch (Exception e) {
            return value;
        }
    }

    private void maskNode(JsonNode node) {
        if (node == null) {
            return;
        }
        if (node.isObject()) {
            Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> entry = fields.next();
                if (isSensitive(entry.getKey()) && entry.getValue().isValueNode()) {
                    ((ObjectNode) node).put(entry.getKey(), MASK);
                } else {
                    maskNode(entry.getValue());
                }
            }
        } else if (node.isArray()) {
            for (JsonNode child : node) {
                maskNode(child);
            }
        }
    }

    private boolean isSensitive(String fieldName) {
        if (fieldName == null) {
            return false;
        }
        // 去掉下划线与连字符后再比对，兼容 old_password / oldPassword 两种写法
        String normalized = fieldName.toLowerCase().replace("_", "").replace("-", "");
        for (String sensitive : SENSITIVE_FIELDS) {
            if (normalized.equals(sensitive.replace("_", ""))) {
                return true;
            }
        }
        return false;
    }

    private String truncate(String text) {
        if (text == null) {
            return null;
        }
        return text.length() <= MAX_TEXT_LENGTH ? text : text.substring(0, MAX_TEXT_LENGTH);
    }
}








