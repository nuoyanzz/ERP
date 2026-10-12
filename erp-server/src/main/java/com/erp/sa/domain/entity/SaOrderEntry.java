package com.erp.sa.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;

@Data
@TableName("sa_order_entry")
public class SaOrderEntry {

    private Long id;
    private Long orderId;
    private Long materialId;
    private BigDecimal qty;
    private BigDecimal price;
    private BigDecimal taxRate;
    private BigDecimal amount;
    private BigDecimal deliveredQty;
    private String remark;
}








