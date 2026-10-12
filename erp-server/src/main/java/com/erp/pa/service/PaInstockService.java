package com.erp.pa.service;

import com.erp.common.api.PageResult;
import com.erp.pa.domain.dto.PaInstockQueryRequest;
import com.erp.pa.domain.dto.PaInstockSaveRequest;
import com.erp.pa.domain.vo.PaInstockDetailVO;
import com.erp.pa.domain.vo.PaInstockListVO;

public interface PaInstockService {

    PageResult<PaInstockListVO> pageInstock(PaInstockQueryRequest request);

    PaInstockDetailVO getInstockDetail(Long id);

    Long createInstock(PaInstockSaveRequest request);

    void updateInstock(PaInstockSaveRequest request);

    void deleteInstock(Long id);

    void auditInstock(Long id);
}








