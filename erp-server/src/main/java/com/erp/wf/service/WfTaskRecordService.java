package com.erp.wf.service;

import com.erp.common.api.PageResult;
import com.erp.wf.domain.vo.WfTaskRecordVO;

public interface WfTaskRecordService {

    /**
     * 查询流程实例的审批动作流水。
     */
    PageResult<WfTaskRecordVO> pageInstanceRecords(Long instanceId, int pageNum, int pageSize);
}








