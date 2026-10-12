package com.erp.ap.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;

@Data
@TableName("ap_verify")
public class ApVerify {

    private Long id;
    private Long paymentId;
    private Long payableId;
    private BigDecimal amount;
}








