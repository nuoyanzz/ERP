package com.erp.hr.service;

import com.erp.common.api.PageResult;
import com.erp.hr.domain.dto.HrOrgSaveRequest;
import com.erp.hr.domain.vo.HrOrgVO;

import java.util.List;

public interface HrOrgService {

    PageResult<HrOrgVO> pageOrg(int pageNum, int pageSize, String orgName);

    List<HrOrgVO> listOrgs();

    HrOrgVO getOrgDetail(Long id);

    Long createOrg(HrOrgSaveRequest request);

    void updateOrg(HrOrgSaveRequest request);

    void deleteOrg(Long id);
}








