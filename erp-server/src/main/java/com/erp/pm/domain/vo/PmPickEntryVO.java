package com.erp.pm.domain.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class PmPickEntryVO {

    private Long id;
    private Long moEntryId;
    private Long materialId;
    private String materialCode;
    private String materialName;

    /** BOM 主料 */
    private Long originalMaterialId;

    /** 是否使用替代料 */
    private Integer substituteFlag;

    /** 替代料 */
    private Long substituteMaterialId;
    private String substituteMaterialCode;
    private String substituteMaterialName;

    private BigDecimal qty;
    private BigDecimal costPrice;
    private BigDecimal costAmount;
    private String batchNo;
    private String remark;
}








