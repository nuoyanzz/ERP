package com.erp.hr.service;

import com.erp.common.api.PageResult;
import com.erp.hr.domain.dto.AttendanceQueryRequest;
import com.erp.hr.domain.dto.AttendanceSaveRequest;
import com.erp.hr.domain.vo.AttendanceVO;

/**
 * 考勤管理服务。
 */
public interface HrAttendanceService {

    PageResult<AttendanceVO> pageAttendance(AttendanceQueryRequest request);

    AttendanceVO getAttendanceDetail(Long id);

    Long saveAttendance(AttendanceSaveRequest request);

    void removeAttendance(Long id);
}
