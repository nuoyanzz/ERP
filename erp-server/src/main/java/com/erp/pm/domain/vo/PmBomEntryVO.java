package com.erp.pm.domain.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class PmBomEntryVO {

    private Long id;
    private Long materialId;
    private String materialCode;
    private String materialName;
    private BigDecimal qty;
    private BigDecimal lossRate;

    /** 是否替代料 */
    private Integer substituteFlag;

    /** 可替代物料 */
    private Long substituteMaterialId;
    private String substituteMaterialCode;
    private String substituteMaterialName;

    /** 明细排序号 */
    private Integer positionNo;

    private String remark;
}








