package com.erp.md.domain.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CustomerSaveRequest {

    private Long id;

    @NotBlank(message = "客户编码不能为空")
    @Size(max = 50, message = "客户编码长度不能超过50")
    private String customerCode;

    @NotBlank(message = "客户名称不能为空")
    @Size(max = 100, message = "客户名称长度不能超过100")
    private String customerName;

    @DecimalMin(value = "0", message = "信用额度不能小于0")
    private BigDecimal creditLimit;

    @Size(max = 10, message = "结算币别长度不能超过10")
    private String currency;

    @Size(max = 50, message = "税号长度不能超过50")
    private String taxNo;

    @Size(max = 50, message = "联系人长度不能超过50")
    private String contactName;

    @Size(max = 20, message = "联系电话长度不能超过20")
    private String contactPhone;

    @Size(max = 255, message = "地址长度不能超过255")
    private String address;

    private Long salesDeptId;
    private Integer status;
    private String remark;
}








