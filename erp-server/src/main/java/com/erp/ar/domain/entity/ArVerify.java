package com.erp.ar.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;

@Data
@TableName("ar_verify")
public class ArVerify {

    private Long id;
    private Long receiptId;
    private Long receivableId;
    private BigDecimal amount;
}








