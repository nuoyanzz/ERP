package com.erp.system.service;

import com.erp.common.api.PageResult;
import com.erp.system.domain.dto.SysParamQueryRequest;
import com.erp.system.domain.dto.SysParamSaveRequest;
import com.erp.system.domain.vo.SysParamVO;

import java.util.List;

public interface SysParamService {

    PageResult<SysParamVO> pageParam(SysParamQueryRequest request);

    /** 列出全部启用中的参数。 */
    List<SysParamVO> listEnabledParams();

    SysParamVO getParamDetail(Long id);

    Long createParam(SysParamSaveRequest request);

    void updateParam(SysParamSaveRequest request);

    void deleteParam(Long id);

    /**
     * 按参数键取值。
     *
     * @param paramKey 参数键
     * @return 参数值，未找到或已停用时返回空串
     */
    String getParamValue(String paramKey);
}













