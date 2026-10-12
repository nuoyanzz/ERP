package com.erp.wf.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class WfNodeRequest {

    @NotBlank(message = "节点编码不能为空")
    @Size(max = 50, message = "节点编码长度不能超过50")
    private String nodeKey;

    @NotBlank(message = "节点名称不能为空")
    @Size(max = 100, message = "节点名称长度不能超过100")
    private String nodeName;

    @NotNull(message = "节点类型不能为空")
    private Integer nodeType;

    private Integer sortNo;

    private Integer approverType;

    @Size(max = 200, message = "审批者长度不能超过200")
    private String approverValue;

    private Integer multiApprove;

    /** 条件表达式，如 {@code amount > 50000 && type == '采购'} */
    @Size(max = 500, message = "条件表达式长度不能超过500")
    private String conditionExpr;

    /** 超时小时数，大于 0 时系统按该时长提醒 */
    private Integer timeoutHours;

    /** 解析不到审批人时的兜底审批人（逗号分隔用户 ID） */
    @Size(max = 200, message = "兜底审批人长度不能超过200")
    private String defaultAssignee;

    /** 是否抄送：1=抄送 */
    private Integer ccFlag;

    private String remark;
}








