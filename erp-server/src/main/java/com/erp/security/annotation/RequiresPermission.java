package com.erp.security.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 接口级权限声明。
 * <p>
 * 标注在 Controller 方法或类上，声明调用该接口所需的权限码。
 * 权限码与 {@code sys_menu.perms} 保持一致，例如 {@code fi:voucher:audit}。
 * 类上的声明对类内所有方法生效。
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequiresPermission {

    /**
     * 所需的权限码，多个之间为「或」关系，任一满足即可访问。
     */
    String[] value();
}








