package com.erp.fi.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;

@Data
@TableName("fi_voucher_entry")
public class FiVoucherEntry {

    public static final int DIRECTION_DEBIT = 1;
    public static final int DIRECTION_CREDIT = 2;

    /** 辅助核算类型：客户 */
    public static final String AUX_CUSTOMER = "CUSTOMER";
    /** 辅助核算类型：供应商 */
    public static final String AUX_SUPPLIER = "SUPPLIER";
    /** 辅助核算类型：部门 */
    public static final String AUX_DEPT = "DEPT";
    /** 辅助核算类型：项目 */
    public static final String AUX_PROJECT = "PROJECT";

    private Long id;
    private Long voucherId;
    private Long accountId;
    private Integer direction;
    private BigDecimal amount;
    private String summary;
    private String assist;
    private Integer sortNo;

    /** 原币金额，外币科目使用 */
    private String currency;
    private BigDecimal originalAmount;
    private BigDecimal exchangeRate;

    /** 结构化辅助核算项，替代无结构的 assist 字段 */
    private String auxType;
    private Long auxId;
}








