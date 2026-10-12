package com.erp.wf.domain.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 撤回请求。
 */
@Data
public class WfWithdrawRequest {

    @Size(max = 500, message = "撤回原因长度不能超过500")
    private String comment;
}








