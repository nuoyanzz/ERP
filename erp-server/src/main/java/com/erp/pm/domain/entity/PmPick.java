package com.erp.pm.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.erp.common.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("pm_pick")
public class PmPick extends BaseEntity {

    public static final int STATUS_DRAFT = 0;
    public static final int STATUS_AUDITED = 1;

    private String pickNo;
    private Long moId;
    private Long warehouseId;
    private LocalDate pickDate;
    private Integer status;
    private Long auditBy;
    private LocalDateTime auditTime;
    private String remark;
}








