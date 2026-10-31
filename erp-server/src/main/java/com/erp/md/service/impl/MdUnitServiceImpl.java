package com.erp.md.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.erp.common.api.PageResult;
import com.erp.common.exception.BizException;
import com.erp.md.domain.dto.MdUnitQueryRequest;
import com.erp.md.domain.dto.MdUnitSaveRequest;
import com.erp.md.domain.entity.MdUnit;
import com.erp.md.domain.vo.MdUnitVO;
import com.erp.md.mapper.MdUnitMapper;
import com.erp.md.service.MdUnitService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MdUnitServiceImpl extends ServiceImpl<MdUnitMapper, MdUnit> implements MdUnitService {

    @Override
    public PageResult<MdUnitVO> pageUnit(MdUnitQueryRequest request) {
        Page<MdUnit> page = this.page(request.toPage(), Wrappers.<MdUnit>lambdaQuery()
                .like(StringUtils.hasText(request.getUnitCode()), MdUnit::getUnitCode, request.getUnitCode())
                .like(StringUtils.hasText(request.getUnitName()), MdUnit::getUnitName, request.getUnitName())
                .eq(request.getStatus() != null, MdUnit::getStatus, request.getStatus())
                .orderByDesc(MdUnit::getCreateTime));
        List<MdUnitVO> records = page.getRecords().stream().map(this::toVO).collect(Collectors.toList());
        Page<MdUnitVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        voPage.setRecords(records);
        return PageResult.of(voPage);
    }

    @Override
    public MdUnitVO getUnitDetail(Long id) {
        MdUnit unit = this.getById(id);
        if (unit == null) {
            throw new BizException("计量单位不存在");
        }
        return toVO(unit);
    }

    @Override
    public Long createUnit(MdUnitSaveRequest request) {
        checkCodeUnique(request.getUnitCode(), null);
        MdUnit unit = new MdUnit();
        BeanUtils.copyProperties(request, unit);
        this.save(unit);
        return unit.getId();
    }

    @Override
    public void updateUnit(MdUnitSaveRequest request) {
        if (request.getId() == null) {
            throw new BizException("id不能为空");
        }
        MdUnit exist = this.getById(request.getId());
        if (exist == null) {
            throw new BizException("计量单位不存在");
        }
        if (!exist.getUnitCode().equals(request.getUnitCode())) {
            checkCodeUnique(request.getUnitCode(), request.getId());
        }
        MdUnit unit = new MdUnit();
        BeanUtils.copyProperties(request, unit);
        this.updateById(unit);
    }

    @Override
    public void deleteUnit(Long id) {
        MdUnit exist = this.getById(id);
        if (exist == null) {
            throw new BizException("计量单位不存在");
        }
        this.removeById(id);
    }

    private void checkCodeUnique(String unitCode, Long excludeId) {
        Long count = this.count(Wrappers.<MdUnit>lambdaQuery()
                .eq(MdUnit::getUnitCode, unitCode)
                .ne(excludeId != null, MdUnit::getId, excludeId));
        if (count != null && count > 0) {
            throw new BizException("计量单位编码已存在");
        }
    }

    private MdUnitVO toVO(MdUnit unit) {
        MdUnitVO vo = new MdUnitVO();
        BeanUtils.copyProperties(unit, vo);
        return vo;
    }
}
