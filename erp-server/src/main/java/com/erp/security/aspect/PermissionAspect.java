package com.erp.security.aspect;

import com.erp.common.api.ResultCode;
import com.erp.common.exception.BizException;
import com.erp.security.LoginUser;
import com.erp.security.SecurityUtils;
import com.erp.security.annotation.RequiresAdmin;
import com.erp.security.annotation.RequiresPermission;
import com.erp.security.annotation.RequiresPermissions;
import com.erp.system.mapper.SysMenuMapper;
import com.erp.security.SuperAdminChecker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 接口权限校验切面。
 * <p>
 * 权限从数据库实时查询，保证角色调整权限后立即生效（不依赖 JWT 中缓存的权限快照）。
 * 超级管理员（角色 ID = 1）绕过全部校验。
 */
@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class PermissionAspect {


    private final SysMenuMapper menuMapper;
    private final SuperAdminChecker superAdminChecker;

    @Around("@within(com.erp.security.annotation.RequiresPermission) "
            + "|| @within(com.erp.security.annotation.RequiresPermissions) "
            + "|| @within(com.erp.security.annotation.RequiresAdmin) "
            + "|| @annotation(com.erp.security.annotation.RequiresPermission) "
            + "|| @annotation(com.erp.security.annotation.RequiresPermissions) "
            + "|| @annotation(com.erp.security.annotation.RequiresAdmin)")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        LoginUser loginUser = SecurityUtils.getCurrentUser();
        if (loginUser == null) {
            throw new BizException(ResultCode.UNAUTHORIZED);
        }
        if (superAdminChecker.isSuperAdmin(loginUser.getUserId())) {
            return joinPoint.proceed();
        }

        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        Class<?> targetClass = joinPoint.getTarget().getClass();

        if (hasAdminAnnotation(method, targetClass)) {
            throw new BizException(ResultCode.FORBIDDEN);
        }

        Set<String> owned = loadOwnedPermissions(loginUser);

        Set<String> anyOf = collectAnyOf(method, targetClass);
        if (!anyOf.isEmpty() && anyOf.stream().noneMatch(owned::contains)) {
            // 越权尝试属于审计事件，保留告警日志便于排查权限配置问题
            log.warn("权限不足：用户={} 需要={} 拥有={}", loginUser.getUsername(), anyOf, owned);
            throw new BizException(ResultCode.FORBIDDEN);
        }

        Set<String> allOf = collectAllOf(method, targetClass);
        for (String perm : allOf) {
            if (!owned.contains(perm)) {
                throw new BizException(ResultCode.FORBIDDEN);
            }
        }
        return joinPoint.proceed();
    }


    private boolean hasAdminAnnotation(Method method, Class<?> targetClass) {
        return AnnotationUtils.findAnnotation(method, RequiresAdmin.class) != null
                || AnnotationUtils.findAnnotation(targetClass, RequiresAdmin.class) != null;
    }

    /** 收集「任一满足即可」的权限码。 */
    private Set<String> collectAnyOf(Method method, Class<?> targetClass) {
        Set<String> required = new LinkedHashSet<>();
        RequiresPermission annotation = method.getAnnotation(RequiresPermission.class);
        if (annotation == null) {
            annotation = AnnotationUtils.findAnnotation(targetClass, RequiresPermission.class);
        }
        if (annotation != null) {
            required.addAll(Arrays.asList(annotation.value()));
        }
        return required;
    }

    /** 收集「全部满足」的权限码。 */
    private Set<String> collectAllOf(Method method, Class<?> targetClass) {
        Set<String> required = new LinkedHashSet<>();
        RequiresPermissions annotation = method.getAnnotation(RequiresPermissions.class);
        if (annotation == null) {
            annotation = AnnotationUtils.findAnnotation(targetClass, RequiresPermissions.class);
        }
        if (annotation != null) {
            required.addAll(Arrays.asList(annotation.value()));
        }
        return required;
    }

    private Set<String> loadOwnedPermissions(LoginUser loginUser) {
        List<String> perms = menuMapper.selectPermsByUserId(loginUser.getUserId());
        if (perms == null || perms.isEmpty()) {
            return Set.of();
        }
        return new LinkedHashSet<>(perms);
    }
}








