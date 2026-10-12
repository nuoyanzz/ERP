package com.erp.pa.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;

@Data
@TableName("pa_order_entry")
public class PaOrderEntry {

    private Long id;
    private Long orderId;
    private Long materialId;
    private BigDecimal qty;
    private BigDecimal price;
    private BigDecimal taxRate;
    private BigDecimal amount;
    private BigDecimal receivedQty;
    private String remark;
}








