package com.erp.md.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.erp.common.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 物料单位换算关系。
 * <p>
 * 业务单据（如出入库、采购销售）允许使用与物料主单位不同的计量单位，
 * 通过本表维护「来源单位 → 目标单位」的换算率。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("md_unit_convert")
public class MdUnitConvert extends BaseEntity {

    /** 物料 ID */
    private Long materialId;

    /** 来源单位 */
    private String fromUnit;

    /** 目标单位 */
    private String toUnit;

    /** 换算率：1 来源单位 = convert_rate 目标单位 */
    private BigDecimal convertRate;
}
