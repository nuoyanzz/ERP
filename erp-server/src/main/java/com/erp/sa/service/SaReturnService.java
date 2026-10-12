package com.erp.sa.service;

import com.erp.common.api.PageResult;
import com.erp.sa.domain.dto.SaReturnQueryRequest;
import com.erp.sa.domain.dto.SaReturnSaveRequest;
import com.erp.sa.domain.vo.SaReturnDetailVO;
import com.erp.sa.domain.vo.SaReturnListVO;

public interface SaReturnService {

    PageResult<SaReturnListVO> pageReturn(SaReturnQueryRequest request);

    SaReturnDetailVO getReturnDetail(Long id);

    Long createReturn(SaReturnSaveRequest request);

    void updateReturn(SaReturnSaveRequest request);

    void deleteReturn(Long id);

    void auditReturn(Long id);
}








