package com.erp.md.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class MdPriceSaveRequest {

    private Long id;

    @NotBlank(message = "价格单号不能为空")
    @Size(max = 30, message = "价格单号长度不能超过30")
    private String priceNo;

    @NotBlank(message = "价格名称不能为空")
    @Size(max = 100, message = "价格名称长度不能超过100")
    private String priceName;

    private Long customerId;
    private Long supplierId;

    @NotNull(message = "物料不能为空")
    private Long materialId;

    @NotNull(message = "单价不能为空")
    private BigDecimal price;

    private BigDecimal taxRate;

    @Size(max = 10, message = "币种长度不能超过10")
    private String currency;

    private LocalDate effectiveDate;
    private LocalDate expiryDate;

    private Integer status;

    @Size(max = 255, message = "备注长度不能超过255")
    private String remark;
}
