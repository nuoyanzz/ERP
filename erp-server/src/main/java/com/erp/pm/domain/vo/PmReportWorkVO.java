package com.erp.pm.domain.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class PmReportWorkVO {

    private Long id;
    private String reportNo;
    private Long moId;
    private String moNo;
    private String materialName;

    private Long operationId;
    private String operationNo;
    private String operationName;

    private Long employeeId;
    private String employeeName;

    private LocalDate reportDate;
    private BigDecimal qty;
    private BigDecimal qualifiedQty;
    private BigDecimal scrapQty;
    private BigDecimal workHours;

    /** 归集的人工成本 */
    private BigDecimal laborCost;

    /** 归集的制造费用 */
    private BigDecimal overheadCost;

    private Integer status;
    private Long voucherId;
    private String remark;
    private LocalDateTime createTime;
}








