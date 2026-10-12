package com.erp.system.domain.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class RoleVO {

    private Long id;
    private String roleCode;
    private String roleName;
    private Integer dataScope;
    private Integer status;
    private String remark;
    private LocalDateTime createTime;
    private List<Long> menuIds;
}








