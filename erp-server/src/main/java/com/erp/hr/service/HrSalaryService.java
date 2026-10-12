package com.erp.hr.service;

import com.erp.common.api.PageResult;
import com.erp.hr.domain.dto.HrSalaryQueryRequest;
import com.erp.hr.domain.dto.HrSalarySaveRequest;
import com.erp.hr.domain.vo.HrSalaryVO;

public interface HrSalaryService {

    PageResult<HrSalaryVO> pageSalary(HrSalaryQueryRequest request);

    HrSalaryVO getSalaryDetail(Long id);

    Long createSalary(HrSalarySaveRequest request);

    void updateSalary(HrSalarySaveRequest request);

    void deleteSalary(Long id);

    void auditSalary(Long id);
}








