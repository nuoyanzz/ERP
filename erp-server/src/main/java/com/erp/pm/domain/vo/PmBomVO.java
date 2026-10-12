package com.erp.pm.domain.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class PmBomVO {

    private Long id;
    private String bomNo;
    private Long materialId;
    private String materialCode;
    private String materialName;
    private String version;
    private Integer status;

    /** 基准数量 */
    private BigDecimal baseQty;

    /** 生效日期 */
    private LocalDate effectiveDate;

    /** 失效日期 */
    private LocalDate expiryDate;

    /** 是否默认版本 */
    private Integer isDefault;

    private String remark;
    private LocalDateTime createTime;
    private List<PmBomEntryVO> entries;
}








