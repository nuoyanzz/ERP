package com.erp.hr.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.erp.common.exception.BizException;
import com.erp.common.util.TreeUtils;
import com.erp.hr.domain.dto.HrDeptSaveRequest;
import com.erp.hr.domain.entity.HrDept;
import com.erp.hr.domain.entity.HrEmployee;
import com.erp.hr.domain.vo.HrDeptTreeVO;
import com.erp.hr.mapper.HrDeptMapper;
import com.erp.hr.mapper.HrEmployeeMapper;
import com.erp.hr.service.HrDeptService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class HrDeptServiceImpl extends ServiceImpl<HrDeptMapper, HrDept> implements HrDeptService {

    private final HrEmployeeMapper employeeMapper;

    @Override
    public List<HrDeptTreeVO> treeDepts() {
        List<HrDept> depts = this.list(Wrappers.<HrDept>lambdaQuery().orderByAsc(HrDept::getDeptCode));
        List<HrDeptTreeVO> vos = new ArrayList<>(depts.size());
        for (HrDept dept : depts) {
            HrDeptTreeVO vo = new HrDeptTreeVO();
            BeanUtils.copyProperties(dept, vo);
            vos.add(vo);
        }
        return TreeUtils.buildTree(vos, HrDeptTreeVO::getId, HrDeptTreeVO::getParentId, HrDeptTreeVO::setChildren);
    }

    @Override
    public HrDeptTreeVO getDeptDetail(Long id) {
        HrDept dept = this.getById(id);
        if (dept == null) {
            throw new BizException("部门不存在");
        }
        HrDeptTreeVO vo = new HrDeptTreeVO();
        BeanUtils.copyProperties(dept, vo);
        return vo;
    }

    @Override
    public Long createDept(HrDeptSaveRequest request) {
        Long count = this.count(Wrappers.<HrDept>lambdaQuery().eq(HrDept::getDeptCode, request.getDeptCode()));
        if (count != null && count > 0) {
            throw new BizException("部门编码已存在");
        }
        HrDept dept = new HrDept();
        BeanUtils.copyProperties(request, dept);
        if (dept.getParentId() == null) {
            dept.setParentId(0L);
        }
        this.save(dept);
        return dept.getId();
    }

    @Override
    public void updateDept(HrDeptSaveRequest request) {
        if (request.getId() == null) {
            throw new BizException("id不能为空");
        }
        HrDept exist = this.getById(request.getId());
        if (exist == null) {
            throw new BizException("部门不存在");
        }
        if (request.getId().equals(request.getParentId())) {
            throw new BizException("上级部门不能是自身");
        }
        Long count = this.count(Wrappers.<HrDept>lambdaQuery()
                .eq(HrDept::getDeptCode, request.getDeptCode())
                .ne(HrDept::getId, request.getId()));
        if (count != null && count > 0) {
            throw new BizException("部门编码已存在");
        }
        HrDept dept = new HrDept();
        BeanUtils.copyProperties(request, dept);
        this.updateById(dept);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteDept(Long id) {
        HrDept exist = this.getById(id);
        if (exist == null) {
            throw new BizException("部门不存在");
        }
        Long childCount = this.count(Wrappers.<HrDept>lambdaQuery().eq(HrDept::getParentId, id));
        if (childCount != null && childCount > 0) {
            throw new BizException("存在子部门，无法删除");
        }
        Long employeeCount = employeeMapper.selectCount(Wrappers.<HrEmployee>lambdaQuery().eq(HrEmployee::getDeptId, id));
        if (employeeCount != null && employeeCount > 0) {
            throw new BizException("该部门下存在员工，无法删除");
        }
        this.removeById(id);
    }
}








