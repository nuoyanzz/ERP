package com.erp.hr.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.erp.common.api.PageResult;
import com.erp.common.exception.BizException;
import com.erp.hr.domain.dto.AttendanceQueryRequest;
import com.erp.hr.domain.dto.AttendanceSaveRequest;
import com.erp.hr.domain.entity.HrAttendance;
import com.erp.hr.domain.entity.HrEmployee;
import com.erp.hr.domain.vo.AttendanceVO;
import com.erp.hr.mapper.HrAttendanceMapper;
import com.erp.hr.mapper.HrEmployeeMapper;
import com.erp.hr.service.HrAttendanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class HrAttendanceServiceImpl extends ServiceImpl<HrAttendanceMapper, HrAttendance>
        implements HrAttendanceService {

    private final HrEmployeeMapper employeeMapper;

    @Override
    public PageResult<AttendanceVO> pageAttendance(AttendanceQueryRequest request) {
        Page<HrAttendance> page = this.page(request.toPage(),
                Wrappers.<HrAttendance>lambdaQuery()
                        .eq(request.getEmployeeId() != null, HrAttendance::getEmployeeId,
                                request.getEmployeeId())
                        .ge(request.getStartDate() != null, HrAttendance::getWorkDate,
                                request.getStartDate())
                        .le(request.getEndDate() != null, HrAttendance::getWorkDate, request.getEndDate())
                        .eq(request.getStatus() != null, HrAttendance::getStatus, request.getStatus())
                        .orderByDesc(HrAttendance::getWorkDate)
                        .orderByAsc(HrAttendance::getEmployeeId));

        List<Long> employeeIds = page.getRecords().stream()
                .map(HrAttendance::getEmployeeId)
                .distinct()
                .collect(Collectors.toList());
        Map<Long, HrEmployee> employeeMap = employeeIds.isEmpty()
                ? Map.of()
                : employeeMapper.selectBatchIds(employeeIds).stream()
                .collect(Collectors.toMap(HrEmployee::getId, Function.identity(), (a, b) -> a));

        List<AttendanceVO> records = page.getRecords().stream()
                .map(attendance -> toVO(attendance, employeeMap.get(attendance.getEmployeeId())))
                .collect(Collectors.toList());
        Page<AttendanceVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        voPage.setRecords(records);
        return PageResult.of(voPage);
    }

    @Override
    public AttendanceVO getAttendanceDetail(Long id) {
        HrAttendance attendance = this.getById(id);
        if (attendance == null) {
            throw new BizException("考勤记录不存在");
        }
        return toVO(attendance, employeeMapper.selectById(attendance.getEmployeeId()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long saveAttendance(AttendanceSaveRequest request) {
        HrEmployee employee = employeeMapper.selectById(request.getEmployeeId());
        if (employee == null) {
            throw new BizException("员工不存在");
        }
        // 同一员工同一天只允许一条考勤，避免重复录入导致工时翻倍
        Long duplicated = this.count(Wrappers.<HrAttendance>lambdaQuery()
                .eq(HrAttendance::getEmployeeId, request.getEmployeeId())
                .eq(HrAttendance::getWorkDate, request.getWorkDate())
                .ne(request.getId() != null, HrAttendance::getId, request.getId()));
        if (duplicated != null && duplicated > 0) {
            throw new BizException("该员工当天已有考勤记录");
        }
        // 未填工时时按打卡时间自动计算
        if ((request.getWorkHours() == null || request.getWorkHours().compareTo(BigDecimal.ZERO) == 0)
                && request.getClockIn() != null && request.getClockOut() != null) {
            long minutes = java.time.Duration.between(request.getClockIn(), request.getClockOut()).toMinutes();
            if (minutes < 0) {
                throw new BizException("下班时间不能早于上班时间");
            }
            request.setWorkHours(BigDecimal.valueOf(minutes).divide(BigDecimal.valueOf(60), 2,
                    java.math.RoundingMode.HALF_UP));
        }
        HrAttendance attendance = new HrAttendance();
        BeanUtils.copyProperties(request, attendance);
        if (attendance.getWorkHours() == null) {
            attendance.setWorkHours(BigDecimal.ZERO);
        }
        if (attendance.getOvertimeHours() == null) {
            attendance.setOvertimeHours(BigDecimal.ZERO);
        }
        if (attendance.getLeaveHours() == null) {
            attendance.setLeaveHours(BigDecimal.ZERO);
        }
        if (attendance.getStatus() == null) {
            attendance.setStatus(HrAttendance.STATUS_NORMAL);
        }
        if (request.getId() == null) {
            this.save(attendance);
        } else {
            this.updateById(attendance);
        }
        return attendance.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeAttendance(Long id) {
        this.removeById(id);
    }

    private AttendanceVO toVO(HrAttendance attendance, HrEmployee employee) {
        AttendanceVO vo = new AttendanceVO();
        BeanUtils.copyProperties(attendance, vo);
        if (employee != null) {
            vo.setEmployeeNo(employee.getEmployeeNo());
            vo.setEmployeeName(employee.getEmployeeName());
        }
        return vo;
    }
}
