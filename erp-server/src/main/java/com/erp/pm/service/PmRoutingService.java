package com.erp.pm.service;

import com.erp.common.api.PageResult;
import com.erp.pm.domain.dto.PmRoutingOperationRequest;
import com.erp.pm.domain.dto.PmRoutingSaveRequest;
import com.erp.pm.domain.vo.PmRoutingOperationVO;
import com.erp.pm.domain.vo.PmRoutingVO;
import com.erp.pm.domain.vo.PmWorkCenterVO;

import java.util.List;

/**
 * 工艺路线管理。
 */
public interface PmRoutingService {

    PageResult<PmRoutingVO> pageRouting(int pageNum, int pageSize, Long materialId, Integer status);

    PmRoutingVO getRoutingDetail(Long id);

    Long createRouting(PmRoutingSaveRequest request);

    void updateRouting(PmRoutingSaveRequest request);

    void deleteRouting(Long id);

    /**
     * 新增工序。
     */
    Long addOperation(Long routingId, PmRoutingOperationRequest request);

    /**
     * 修改工序。
     */
    void updateOperation(Long routingId, Long operationId, PmRoutingOperationRequest request);

    /**
     * 删除工序。
     */
    void deleteOperation(Long routingId, Long operationId);

    /**
     * 工作中心列表（工序选择用）。
     */
    List<PmWorkCenterVO> listWorkCenters();
}








