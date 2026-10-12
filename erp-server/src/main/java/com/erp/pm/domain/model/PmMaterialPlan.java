package com.erp.pm.domain.model;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 物料计划属性快照。
 * <p>
 * MRP 需要批量/提前期等计划字段，这些字段定义在 md_material 上。
 * 为了避免与主数据模块的并行改动冲突，这里用 pm 模块自有的查询把它读出来，
 * 而不是给 MdMaterial 实体加字段。
 */
@Data
public class PmMaterialPlan {

    private Long id;
    private String materialCode;
    private String materialName;
    private String spec;
    private String unit;

    /** 成本价（采购建议时作为参考单价） */
    private BigDecimal costPrice;

    /** 安全库存 */
    private BigDecimal safeStock;

    /** 采购类型：1 采购，2 自制 */
    private Integer procureType;

    /** 通用提前期（天） */
    private Integer leadTimeDays;

    /** 采购提前期（天） */
    private Integer purchaseLeadDays;

    /** 生产提前期（天） */
    private Integer productionLeadDays;

    /** 最小订购量 */
    private BigDecimal minOrderQty;

    /** 批量（订购批量的整数倍） */
    private BigDecimal lotSize;

    /** 税率（百分数） */
    private BigDecimal taxRate;

    /** 默认供应商 */
    private Long defaultSupplierId;

    private Integer status;
}








