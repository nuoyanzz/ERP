package com.erp.pm.domain.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class PmRoutingVO {

    private Long id;
    private String routingNo;
    private Long materialId;
    private String materialCode;
    private String materialName;
    private String version;
    private Integer status;
    private LocalDate effectiveDate;
    private String remark;
    private LocalDateTime createTime;

    /** 工序明细 */
    private List<PmRoutingOperationVO> operations;

    /** 汇总：标准总工时 */
    private BigDecimal totalStandardHours;

    /** 汇总：标准人工成本合计 */
    private BigDecimal totalLaborCost;

    /** 汇总：标准制造费用合计 */
    private BigDecimal totalOverheadCost;
}








