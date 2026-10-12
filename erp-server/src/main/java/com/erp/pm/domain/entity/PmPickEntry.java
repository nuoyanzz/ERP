package com.erp.pm.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;

@Data
@TableName("pm_pick_entry")
public class PmPickEntry {

    private Long id;
    private Long pickId;
    private Long moEntryId;

    /** 实际领用的物料（使用替代料时为替代料 ID） */
    private Long materialId;

    /** BOM 中要求的主料 */
    private Long originalMaterialId;

    /** 是否使用替代料：1 表示用替代料顶替主料 */
    private Integer substituteFlag;

    /** 替代料 */
    private Long substituteMaterialId;

    private BigDecimal qty;
    private BigDecimal costPrice;
    private BigDecimal costAmount;
    private String batchNo;
    private String remark;
}








