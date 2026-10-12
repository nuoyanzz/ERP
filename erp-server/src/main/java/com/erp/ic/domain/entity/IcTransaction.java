package com.erp.ic.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.erp.common.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ic_transaction")
public class IcTransaction extends BaseEntity {

    public static final int DIRECTION_IN = 1;
    public static final int DIRECTION_OUT = 2;

    private Long materialId;
    private Long warehouseId;
    private String batchNo;
    private String tranType;
    private Integer direction;
    private BigDecimal qty;
    private Long bizId;
    private String bizNo;
    private LocalDate bizDate;
    private String remark;
}








