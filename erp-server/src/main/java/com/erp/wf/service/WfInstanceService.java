package com.erp.wf.service;

import com.erp.common.api.PageResult;
import com.erp.wf.domain.dto.WfInstanceQueryRequest;
import com.erp.wf.domain.dto.WfStartRequest;
import com.erp.wf.domain.vo.WfInstanceDetailVO;
import com.erp.wf.domain.vo.WfInstanceVO;

public interface WfInstanceService {

    PageResult<WfInstanceVO> pageInstance(WfInstanceQueryRequest request);

    WfInstanceDetailVO getInstanceDetail(Long id);

    Long startInstance(WfStartRequest request);

    void cancelInstance(Long id);

    /**
     * 发起人撤回流程。
     */
    void withdrawInstance(Long id, String comment);
}








