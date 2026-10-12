package com.erp.ic.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;

@Data
@TableName("ic_transfer_entry")
public class IcTransferEntry {

    private Long id;
    private Long transferId;
    private Long materialId;
    private String batchNo;
    private BigDecimal qty;
    private String remark;
}








