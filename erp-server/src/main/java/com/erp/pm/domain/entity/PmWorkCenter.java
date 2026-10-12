package com.erp.pm.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.erp.common.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 工作中心（车间/产线），是产能与制造费用的归集对象。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("pm_work_center")
public class PmWorkCenter extends BaseEntity {

    public static final int STATUS_DISABLED = 0;
    public static final int STATUS_ENABLED = 1;

    private String centerCode;
    private String centerName;

    /** 日产能（小时） */
    private BigDecimal capacityHours;

    private Integer status;
    private String remark;
}








