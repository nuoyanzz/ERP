package com.erp.wf.domain.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 加签请求。
 */
@Data
public class WfCountersignRequest {

    /** 加签人用户 ID */
    @NotNull(message = "加签人不能为空")
    private Long targetUserId;

    /** 加签位置：1=前加签，2=后加签 */
    private Integer position;

    @Size(max = 500, message = "备注长度不能超过500")
    private String comment;
}








