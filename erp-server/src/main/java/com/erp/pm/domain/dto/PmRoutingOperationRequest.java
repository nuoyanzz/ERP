package com.erp.pm.domain.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class PmRoutingOperationRequest {

    private Long id;

    @NotBlank(message = "工序号不能为空")
    @Size(max = 30, message = "工序号长度不能超过30")
    private String operationNo;

    @NotBlank(message = "工序名称不能为空")
    @Size(max = 100, message = "工序名称长度不能超过100")
    private String operationName;

    /** 工作中心 */
    private Long workCenterId;

    @DecimalMin(value = "0", message = "标准工时不能小于0")
    private BigDecimal standardHours;

    @DecimalMin(value = "0", message = "标准产能不能小于0")
    private BigDecimal standardQty;

    @DecimalMin(value = "0", message = "人工费率不能小于0")
    private BigDecimal laborRate;

    @DecimalMin(value = "0", message = "制造费用率不能小于0")
    private BigDecimal overheadRate;

    /** 排序号 */
    private Integer sortNo;

    @Size(max = 255, message = "备注长度不能超过255")
    private String remark;
}








