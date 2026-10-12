package com.erp.pm.service;

import com.erp.common.api.PageResult;
import com.erp.pm.domain.dto.PmMoSaveRequest;
import com.erp.pm.domain.vo.PmMoVO;

public interface PmMoService {

    PageResult<PmMoVO> pageMo(int pageNum, int pageSize, Long materialId, Integer status);

    PmMoVO getMoDetail(Long id);

    Long createMo(PmMoSaveRequest request);

    void updateMo(PmMoSaveRequest request);

    void deleteMo(Long id);

    void auditMo(Long id);
}








