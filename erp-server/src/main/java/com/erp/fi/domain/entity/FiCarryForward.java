package com.erp.fi.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.erp.common.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 期末结转记录。用于结转幂等控制与反结转追溯。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("fi_carry_forward")
public class FiCarryForward extends BaseEntity {

    /** 结转类型：1 损益结转 2 年末结转 */
    public static final int TYPE_PROFIT_LOSS = 1;
    public static final int TYPE_YEAR_END = 2;

    /** 状态：0 已生成 1 已反结转 */
    public static final int STATUS_ACTIVE = 0;
    public static final int STATUS_REVERSED = 1;

    private String period;
    private Integer carryType;
    private Long voucherId;
    private BigDecimal totalAmount;
    private Integer status;
}








