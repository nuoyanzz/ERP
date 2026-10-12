package com.erp.pm.domain.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class PmRoutingSaveRequest {

    private Long id;

    @NotNull(message = "生产物料不能为空")
    private Long materialId;

    @Size(max = 20, message = "版本号长度不能超过20")
    private String version;

    /** 生效日期 */
    private LocalDate effectiveDate;

    @Size(max = 255, message = "备注长度不能超过255")
    private String remark;

    @Valid
    @NotEmpty(message = "工序明细不能为空")
    private List<PmRoutingOperationRequest> operations;
}








