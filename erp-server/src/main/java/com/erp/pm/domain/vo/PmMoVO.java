package com.erp.pm.domain.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class PmMoVO {

    private Long id;
    private String moNo;
    private Long materialId;
    private String materialCode;
    private String materialName;
    private Long bomId;
    private String bomNo;
    private BigDecimal qty;
    private Long warehouseId;
    private String warehouseName;
    private LocalDate planStartDate;
    private LocalDate planEndDate;
    private Integer status;
    private String remark;
    private LocalDateTime createTime;
    private List<PmMoEntryVO> entries;
}








