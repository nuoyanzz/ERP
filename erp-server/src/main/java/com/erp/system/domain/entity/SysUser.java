package com.erp.system.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.erp.common.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_user")
public class SysUser extends BaseEntity {

    private String username;
    private String password;
    private String nickname;
    private String email;
    private String phone;
    private Long deptId;
    private Integer status;
    private String remark;

    /** 连续登录失败次数 */
    private Integer failCount;
    /** 锁定截止时间 */
    private LocalDateTime lockUntil;
    /** 密码最后修改时间，用于密码有效期策略 */
    private LocalDateTime pwdUpdateTime;
    private LocalDateTime lastLoginTime;
    private String lastLoginIp;
}








