package com.erp.pm.service;

import com.erp.common.api.PageResult;
import com.erp.pm.domain.dto.PmBomSaveRequest;
import com.erp.pm.domain.vo.PmBomVO;

public interface PmBomService {

    PageResult<PmBomVO> pageBom(int pageNum, int pageSize, Long materialId, Integer status);

    PmBomVO getBomDetail(Long id);

    Long createBom(PmBomSaveRequest request);

    void updateBom(PmBomSaveRequest request);

    void deleteBom(Long id);
}








