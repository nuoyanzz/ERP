package com.erp.wf.service;

import com.erp.common.api.PageResult;
import com.erp.wf.domain.dto.WfApproveRequest;
import com.erp.wf.domain.vo.WfTaskVO;

public interface WfTaskService {

    PageResult<WfTaskVO> todoPage(int pageNum, int pageSize);

    PageResult<WfTaskVO> donePage(int pageNum, int pageSize);

    /**
     * 查询某个流程实例下的全部任务。
     */
    PageResult<WfTaskVO> pageInstanceTasks(Long instanceId, int pageNum, int pageSize);

    void approveTask(Long taskId, WfApproveRequest request);

    void rejectTask(Long taskId, WfApproveRequest request);

    /**
     * 退回到上一节点重新审批。
     */
    void rollbackTask(Long taskId, WfApproveRequest request);

    /**
     * 转办：把待办转给其他人。
     */
    void transferTask(Long taskId, Long targetUserId, String comment);

    /**
     * 加签：在当前节点增加审批人。
     *
     * @param position 1=前加签，2=后加签
     */
    void countersignTask(Long taskId, Long targetUserId, Integer position, String comment);

    /**
     * 催办。
     */
    void urgeTask(Long taskId);

    void cancelPendingTasks(Long instanceId);
}








