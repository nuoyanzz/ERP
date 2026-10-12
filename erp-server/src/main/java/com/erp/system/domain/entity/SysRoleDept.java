package com.erp.system.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

/**
 * 角色与部门的关联，用于角色的自定义数据范围。
 */
@Data
@TableName("sys_role_dept")
public class SysRoleDept implements Serializable {

    private Long id;
    private Long roleId;
    private Long deptId;
}








