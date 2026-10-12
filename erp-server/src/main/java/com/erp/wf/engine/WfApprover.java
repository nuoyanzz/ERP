package com.erp.wf.engine;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 审批人解析结果。
 * <p>
 * 区分「直接解析出的审批人」与「解析失败原因」：引擎在拿不到任何审批人时需要把实例挂起并记录原因，
 * 因此解析结果必须携带失败信息，而不是简单返回空集合。
 */
@Data
public class WfApprover {

    /** 审批人用户 ID 列表 */
    private List<Long> userIds = new ArrayList<>();

    /** 解析失败原因，成功时为 null */
    private String failReason;

    public static WfApprover of(List<Long> userIds) {
        WfApprover approver = new WfApprover();
        if (userIds != null && !userIds.isEmpty()) {
            approver.setUserIds(new ArrayList<>(userIds));
        }
        return approver;
    }

    public static WfApprover fail(String reason) {
        WfApprover approver = new WfApprover();
        approver.setFailReason(reason);
        return approver;
    }

    public boolean isEmpty() {
        return userIds.isEmpty();
    }
}








