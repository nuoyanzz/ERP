package com.erp.pa.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;

@Data
@TableName("pa_return_entry")
public class PaReturnEntry {

    private Long id;
    private Long returnId;
    private Long sourceEntryId;
    private Long materialId;
    private BigDecimal qty;
    private BigDecimal price;
    private BigDecimal taxRate;
    private BigDecimal amount;
    private String batchNo;
    private String remark;
}








