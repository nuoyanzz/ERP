package com.erp.pa.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;

@Data
@TableName("pa_instock_entry")
public class PaInstockEntry {

    private Long id;
    private Long instockId;
    private Long orderEntryId;
    private Long materialId;
    private BigDecimal qty;
    private BigDecimal price;
    private BigDecimal taxRate;
    private BigDecimal amount;
    private BigDecimal returnedQty;
    private String batchNo;
    private String remark;
}








