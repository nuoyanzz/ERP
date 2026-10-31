package com.erp.wf.service;

import com.erp.common.api.PageResult;
import com.erp.wf.domain.dto.WfApproveRequest;
import com.erp.wf.domain.vo.WfTaskVO;

public interface WfTaskService {

    /**
     * 我的待办。
     *
     * @param bizType 业务类型过滤（如 PA_ORDER），为空表示不限
     * @param bizId   业务单据 ID 过滤，为空表示不限
     */
    PageResult<WfTaskVO> todoPage(int pageNum, int pageSize, String bizType, Long bizId);

    /**
     * 我的已办。
     *
     * @param bizType 业务类型过滤（如 PA_ORDER），为空表示不限
     * @param bizId   业务单据 ID 过滤，为空表示不限
     */
    PageResult<WfTaskVO> donePage(int pageNum, int pageSize, String bizType, Long bizId);

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








