package com.erp.hr.domain.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

@Data
public class HrEmployeeSaveRequest {

    private Long id;

    private String employeeNo;

    @NotBlank(message = "员工姓名不能为空")
    @Size(max = 50, message = "员工姓名长度不能超过50")
    private String employeeName;

    private Integer gender;

    @NotNull(message = "部门不能为空")
    private Long deptId;

    @Size(max = 50, message = "岗位长度不能超过50")
    private String position;

    private LocalDate entryDate;

    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    @Email(message = "邮箱格式不正确")
    private String email;

    private Integer status;
    private String remark;
}








