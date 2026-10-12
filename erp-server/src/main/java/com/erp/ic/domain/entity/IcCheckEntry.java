package com.erp.ic.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;

@Data
@TableName("ic_check_entry")
public class IcCheckEntry {

    private Long id;
    private Long checkId;
    private Long materialId;
    private String batchNo;
    private BigDecimal bookQty;
    private BigDecimal countQty;
    private BigDecimal gainQty;
    private BigDecimal lossQty;
    /** 盘盈金额 */
    private BigDecimal gainAmount;
    /** 盘亏金额 */
    private BigDecimal lossAmount;
    /** 差异原因码 */
    private String reasonCode;
    private String remark;
}








