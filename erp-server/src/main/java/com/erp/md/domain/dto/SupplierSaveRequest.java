package com.erp.md.domain.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SupplierSaveRequest {

    private Long id;

    @NotBlank(message = "供应商编码不能为空")
    @Size(max = 50, message = "供应商编码长度不能超过50")
    private String supplierCode;

    @NotBlank(message = "供应商名称不能为空")
    @Size(max = 100, message = "供应商名称长度不能超过100")
    private String supplierName;

    @Size(max = 50, message = "税号长度不能超过50")
    private String taxNo;

    @Size(max = 50, message = "结算方式长度不能超过50")
    private String settleType;

    @Min(value = 1, message = "评级取值1-5")
    @Max(value = 5, message = "评级取值1-5")
    private Integer level;

    @Size(max = 50, message = "联系人长度不能超过50")
    private String contactName;

    @Size(max = 20, message = "联系电话长度不能超过20")
    private String contactPhone;

    @Size(max = 255, message = "地址长度不能超过255")
    private String address;

    private Integer status;
    private String remark;
}








