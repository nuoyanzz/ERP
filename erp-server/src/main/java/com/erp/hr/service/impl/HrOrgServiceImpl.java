package com.erp.hr.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.erp.common.api.PageResult;
import com.erp.common.exception.BizException;
import com.erp.hr.domain.dto.HrOrgSaveRequest;
import com.erp.hr.domain.entity.HrOrg;
import com.erp.hr.domain.vo.HrOrgVO;
import com.erp.hr.mapper.HrOrgMapper;
import com.erp.hr.service.HrOrgService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class HrOrgServiceImpl extends ServiceImpl<HrOrgMapper, HrOrg> implements HrOrgService {

    @Override
    public PageResult<HrOrgVO> pageOrg(int pageNum, int pageSize, String orgName) {
        Page<HrOrg> page = this.page(new Page<>(pageNum, pageSize), Wrappers.<HrOrg>lambdaQuery()
                .like(StringUtils.hasText(orgName), HrOrg::getOrgName, orgName)
                .orderByAsc(HrOrg::getOrgCode));
        List<HrOrgVO> records = page.getRecords().stream().map(this::toVO).collect(Collectors.toList());
        Page<HrOrgVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        voPage.setRecords(records);
        return PageResult.of(voPage);
    }

    @Override
    public List<HrOrgVO> listOrgs() {
        return this.list(Wrappers.<HrOrg>lambdaQuery().orderByAsc(HrOrg::getOrgCode))
                .stream()
                .map(this::toVO)
                .collect(Collectors.toList());
    }

    @Override
    public HrOrgVO getOrgDetail(Long id) {
        HrOrg org = this.getById(id);
        if (org == null) {
            throw new BizException("组织不存在");
        }
        return toVO(org);
    }

    @Override
    public Long createOrg(HrOrgSaveRequest request) {
        Long count = this.count(Wrappers.<HrOrg>lambdaQuery().eq(HrOrg::getOrgCode, request.getOrgCode()));
        if (count != null && count > 0) {
            throw new BizException("组织编码已存在");
        }
        HrOrg org = new HrOrg();
        BeanUtils.copyProperties(request, org);
        this.save(org);
        return org.getId();
    }

    @Override
    public void updateOrg(HrOrgSaveRequest request) {
        if (request.getId() == null) {
            throw new BizException("id不能为空");
        }
        HrOrg exist = this.getById(request.getId());
        if (exist == null) {
            throw new BizException("组织不存在");
        }
        Long count = this.count(Wrappers.<HrOrg>lambdaQuery()
                .eq(HrOrg::getOrgCode, request.getOrgCode())
                .ne(HrOrg::getId, request.getId()));
        if (count != null && count > 0) {
            throw new BizException("组织编码已存在");
        }
        HrOrg org = new HrOrg();
        BeanUtils.copyProperties(request, org);
        this.updateById(org);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteOrg(Long id) {
        HrOrg exist = this.getById(id);
        if (exist == null) {
            throw new BizException("组织不存在");
        }
        this.removeById(id);
    }

    private HrOrgVO toVO(HrOrg org) {
        HrOrgVO vo = new HrOrgVO();
        BeanUtils.copyProperties(org, vo);
        return vo;
    }
}








