package com.erp.hr.service.impl;

import com.erp.common.util.SerialNoGenerator;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.erp.common.api.PageResult;
import com.erp.common.exception.BizException;
import com.erp.hr.domain.dto.HrEmployeeQueryRequest;
import com.erp.hr.domain.dto.HrEmployeeSaveRequest;
import com.erp.hr.domain.entity.HrDept;
import com.erp.hr.domain.entity.HrEmployee;
import com.erp.hr.domain.vo.HrEmployeeVO;
import com.erp.hr.mapper.HrDeptMapper;
import com.erp.hr.mapper.HrEmployeeMapper;
import com.erp.hr.service.HrEmployeeService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class HrEmployeeServiceImpl extends ServiceImpl<HrEmployeeMapper, HrEmployee> implements HrEmployeeService {

    private static final String NO_PREFIX = "YG-";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final HrDeptMapper deptMapper;
    /** 业务单号生成器（原子自增，避免并发撞号） */
    private final SerialNoGenerator serialNoGenerator;

    @Override
    public PageResult<HrEmployeeVO> pageEmployee(HrEmployeeQueryRequest request) {
        Page<HrEmployee> page = this.page(request.toPage(), Wrappers.<HrEmployee>lambdaQuery()
                .like(StringUtils.hasText(request.getEmployeeNo()), HrEmployee::getEmployeeNo, request.getEmployeeNo())
                .like(StringUtils.hasText(request.getEmployeeName()), HrEmployee::getEmployeeName, request.getEmployeeName())
                .eq(request.getDeptId() != null, HrEmployee::getDeptId, request.getDeptId())
                .eq(request.getStatus() != null, HrEmployee::getStatus, request.getStatus())
                .orderByDesc(HrEmployee::getId));
        List<HrEmployeeVO> records = page.getRecords().stream().map(this::toVO).collect(Collectors.toList());
        Page<HrEmployeeVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        voPage.setRecords(records);
        return PageResult.of(voPage);
    }

    @Override
    public HrEmployeeVO getEmployeeDetail(Long id) {
        HrEmployee employee = this.getById(id);
        if (employee == null) {
            throw new BizException("员工不存在");
        }
        return toVO(employee);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createEmployee(HrEmployeeSaveRequest request) {
        HrDept dept = deptMapper.selectById(request.getDeptId());
        if (dept == null) {
            throw new BizException("部门不存在");
        }
        HrEmployee employee = new HrEmployee();
        BeanUtils.copyProperties(request, employee);
        employee.setEmployeeNo(nextNo());
        if (employee.getGender() == null) {
            employee.setGender(1);
        }
        if (employee.getStatus() == null) {
            employee.setStatus(HrEmployee.STATUS_ACTIVE);
        }
        this.save(employee);
        return employee.getId();
    }

    @Override
    public void updateEmployee(HrEmployeeSaveRequest request) {
        if (request.getId() == null) {
            throw new BizException("id不能为空");
        }
        HrEmployee exist = this.getById(request.getId());
        if (exist == null) {
            throw new BizException("员工不存在");
        }
        HrDept dept = deptMapper.selectById(request.getDeptId());
        if (dept == null) {
            throw new BizException("部门不存在");
        }
        HrEmployee employee = new HrEmployee();
        BeanUtils.copyProperties(request, employee);
        this.updateById(employee);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteEmployee(Long id) {
        HrEmployee exist = this.getById(id);
        if (exist == null) {
            throw new BizException("员工不存在");
        }
        this.removeById(id);
    }

    private HrEmployeeVO toVO(HrEmployee employee) {
        HrEmployeeVO vo = new HrEmployeeVO();
        BeanUtils.copyProperties(employee, vo);
        if (employee.getDeptId() != null) {
            HrDept dept = deptMapper.selectById(employee.getDeptId());
            if (dept != null) {
                vo.setDeptName(dept.getDeptName());
            }
        }
        return vo;
    }

    private String nextNo() {
        // 序号取自流水表原子自增：并发不撞号，软删除不重用旧号
        return serialNoGenerator.next("YG", LocalDate.now().format(DATE_FORMATTER));
    }

}








