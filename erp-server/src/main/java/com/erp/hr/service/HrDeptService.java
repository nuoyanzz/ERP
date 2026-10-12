package com.erp.hr.service;

import com.erp.hr.domain.dto.HrDeptSaveRequest;
import com.erp.hr.domain.vo.HrDeptTreeVO;

import java.util.List;

public interface HrDeptService {

    List<HrDeptTreeVO> treeDepts();

    HrDeptTreeVO getDeptDetail(Long id);

    Long createDept(HrDeptSaveRequest request);

    void updateDept(HrDeptSaveRequest request);

    void deleteDept(Long id);
}








