package com.erp.pm.service;

import com.erp.common.api.PageResult;
import com.erp.pm.domain.dto.PmFinishSaveRequest;
import com.erp.pm.domain.vo.PmFinishVO;

public interface PmFinishService {

    PageResult<PmFinishVO> pageFinish(int pageNum, int pageSize, Long moId, Integer status);

    PmFinishVO getFinishDetail(Long id);

    Long createFinish(PmFinishSaveRequest request);

    void updateFinish(PmFinishSaveRequest request);

    void deleteFinish(Long id);

    void auditFinish(Long id);
}








