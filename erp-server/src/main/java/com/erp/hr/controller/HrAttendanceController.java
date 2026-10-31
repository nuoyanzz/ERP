package com.erp.hr.controller;

import com.erp.common.api.PageResult;
import com.erp.common.api.Result;
import com.erp.common.exception.BizException;
import com.erp.hr.domain.dto.AttendanceQueryRequest;
import com.erp.hr.domain.dto.AttendanceSaveRequest;
import com.erp.hr.domain.vo.AttendanceVO;
import com.erp.hr.service.HrAttendanceService;
import com.erp.security.annotation.RequiresPermission;
import com.erp.system.annotation.OperLog;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 考勤管理。记录员工上下班打卡、加班与请假。
 */
@RestController
@RequestMapping("/hr/attendance")
@RequiredArgsConstructor
public class HrAttendanceController {

    private final HrAttendanceService attendanceService;

    @GetMapping("/page")
    @RequiresPermission("hr:attendance:query")
    @OperLog(module = "hr")
    public Result<PageResult<AttendanceVO>> page(AttendanceQueryRequest request) {
        return Result.ok(attendanceService.pageAttendance(request));
    }

    @GetMapping("/{id}")
    @RequiresPermission("hr:attendance:query")
    @OperLog(module = "hr")
    public Result<AttendanceVO> detail(@PathVariable Long id) {
        return Result.ok(attendanceService.getAttendanceDetail(id));
    }

    @PostMapping
    @RequiresPermission("hr:attendance:add")
    @OperLog(module = "hr", type = "CREATE", value = "录入考勤")
    public Result<Long> create(@Valid @RequestBody AttendanceSaveRequest request) {
        request.setId(null);
        return Result.ok(attendanceService.saveAttendance(request));
    }

    @PutMapping
    @RequiresPermission("hr:attendance:edit")
    @OperLog(module = "hr", type = "UPDATE", value = "修改考勤")
    public Result<Void> update(@Valid @RequestBody AttendanceSaveRequest request) {
        if (request.getId() == null) {
            throw new BizException("id不能为空");
        }
        attendanceService.saveAttendance(request);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    @RequiresPermission("hr:attendance:remove")
    @OperLog(module = "hr", type = "DELETE", value = "删除考勤")
    public Result<Void> remove(@PathVariable Long id) {
        attendanceService.removeAttendance(id);
        return Result.ok();
    }
}
