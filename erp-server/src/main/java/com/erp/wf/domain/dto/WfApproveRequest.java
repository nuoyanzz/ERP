package com.erp.wf.domain.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class WfApproveRequest {

    @Size(max = 500, message = "审批意见长度不能超过500")
    private String comment;
}








