package com.erp.wf.domain.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 转办请求。
 */
@Data
public class WfTransferRequest {

    /** 转办目标用户 ID */
    @NotNull(message = "转办目标不能为空")
    private Long targetUserId;

    @Size(max = 500, message = "备注长度不能超过500")
    private String comment;
}








