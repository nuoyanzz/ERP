package com.erp.ic.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.erp.common.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ic_transfer")
public class IcTransfer extends BaseEntity {

    public static final int STATUS_DRAFT = 0;
    public static final int STATUS_AUDITED = 1;

    private String transferNo;
    private Long fromWarehouseId;
    private Long toWarehouseId;
    private LocalDate transferDate;
    private Integer status;
    private Long auditBy;
    private LocalDateTime auditTime;
    private String remark;

    /** 所属部门，数据权限按此字段过滤 */
    private Long deptId;
}








