package com.erp.ar.domain.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 坏账计提单保存请求。
 */
@Data
public class ArBadDebtSaveRequest {

    private Long id;

    @NotNull(message = "客户不能为空")
    private Long customerId;

    @NotBlank(message = "计提期间不能为空")
    @Pattern(regexp = "^\\d{4}-(0[1-9]|1[0-2])$", message = "计提期间格式应为 YYYY-MM")
    private String period;

    @NotNull(message = "计提金额不能为空")
    @DecimalMin(value = "0.0001", message = "计提金额必须大于0")
    private BigDecimal amount;

    @Size(max = 255, message = "计提原因长度不能超过255")
    private String reason;

    @Size(max = 255, message = "备注长度不能超过255")
    private String remark;
}













