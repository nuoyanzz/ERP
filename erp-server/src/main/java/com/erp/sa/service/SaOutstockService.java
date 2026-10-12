package com.erp.sa.service;

import com.erp.common.api.PageResult;
import com.erp.sa.domain.dto.SaOutstockQueryRequest;
import com.erp.sa.domain.dto.SaOutstockSaveRequest;
import com.erp.sa.domain.vo.SaOutstockDetailVO;
import com.erp.sa.domain.vo.SaOutstockListVO;

public interface SaOutstockService {

    PageResult<SaOutstockListVO> pageOutstock(SaOutstockQueryRequest request);

    SaOutstockDetailVO getOutstockDetail(Long id);

    Long createOutstock(SaOutstockSaveRequest request);

    void updateOutstock(SaOutstockSaveRequest request);

    void deleteOutstock(Long id);

    void auditOutstock(Long id);
}








