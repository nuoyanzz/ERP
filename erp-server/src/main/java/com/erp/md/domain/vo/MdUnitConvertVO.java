package com.erp.md.domain.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class MdUnitConvertVO {

    private Long id;
    private Long materialId;
    private String fromUnit;
    private String toUnit;
    private BigDecimal convertRate;
    private LocalDateTime createTime;

    /** 物料编码，由 md_material 带出 */
    private String materialCode;

    /** 物料名称，由 md_material 带出 */
    private String materialName;
}
