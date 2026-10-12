package com.erp.pa.service;

import com.erp.common.api.PageResult;
import com.erp.pa.domain.dto.PaReturnQueryRequest;
import com.erp.pa.domain.dto.PaReturnSaveRequest;
import com.erp.pa.domain.vo.PaReturnDetailVO;
import com.erp.pa.domain.vo.PaReturnListVO;

public interface PaReturnService {

    PageResult<PaReturnListVO> pageReturn(PaReturnQueryRequest request);

    PaReturnDetailVO getReturnDetail(Long id);

    Long createReturn(PaReturnSaveRequest request);

    void updateReturn(PaReturnSaveRequest request);

    void deleteReturn(Long id);

    void auditReturn(Long id);
}








