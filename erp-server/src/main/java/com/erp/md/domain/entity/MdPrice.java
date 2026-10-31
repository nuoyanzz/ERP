package com.erp.md.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.erp.common.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 价格表主数据。
 * <p>
 * 一条记录描述「某物料在某个客户或供应商下的价格」，
 * 可通过生效日期与失效日期限定价格的有效区间。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("md_price_list")
public class MdPrice extends BaseEntity {

    /** 价格单号，全局唯一 */
    private String priceNo;

    /** 价格名称 */
    private String priceName;

    /** 客户 ID，为空表示通用价格 */
    private Long customerId;

    /** 供应商 ID，为空表示通用价格 */
    private Long supplierId;

    /** 物料 ID */
    private Long materialId;

    /** 单价（不含税） */
    private BigDecimal price;

    /** 税率（百分数，如 13 表示 13%） */
    private BigDecimal taxRate;

    /** 币种，如 CNY */
    private String currency;

    /** 生效日期 */
    private LocalDate effectiveDate;

    /** 失效日期 */
    private LocalDate expiryDate;

    /** 状态：0 启用，1 停用 */
    private Integer status;

    private String remark;
}
