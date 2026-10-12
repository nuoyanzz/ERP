package com.erp.hr.service;

import com.erp.common.api.PageResult;
import com.erp.hr.domain.dto.HrEmployeeQueryRequest;
import com.erp.hr.domain.dto.HrEmployeeSaveRequest;
import com.erp.hr.domain.vo.HrEmployeeVO;

public interface HrEmployeeService {

    PageResult<HrEmployeeVO> pageEmployee(HrEmployeeQueryRequest request);

    HrEmployeeVO getEmployeeDetail(Long id);

    Long createEmployee(HrEmployeeSaveRequest request);

    void updateEmployee(HrEmployeeSaveRequest request);

    void deleteEmployee(Long id);
}








