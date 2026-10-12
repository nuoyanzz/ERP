package com.erp.md.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.erp.common.api.PageResult;
import com.erp.common.exception.BizException;
import com.erp.md.domain.dto.SupplierQueryRequest;
import com.erp.md.domain.dto.SupplierSaveRequest;
import com.erp.md.domain.entity.MdSupplier;
import com.erp.md.domain.vo.SupplierVO;
import com.erp.md.mapper.MdSupplierMapper;
import com.erp.md.service.MdSupplierService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MdSupplierServiceImpl extends ServiceImpl<MdSupplierMapper, MdSupplier> implements MdSupplierService {

    @Override
    public PageResult<SupplierVO> pageSupplier(SupplierQueryRequest request) {
        Page<MdSupplier> page = this.page(request.toPage(), Wrappers.<MdSupplier>lambdaQuery()
                .like(StringUtils.hasText(request.getSupplierCode()), MdSupplier::getSupplierCode, request.getSupplierCode())
                .like(StringUtils.hasText(request.getSupplierName()), MdSupplier::getSupplierName, request.getSupplierName())
                .eq(request.getStatus() != null, MdSupplier::getStatus, request.getStatus())
                .orderByDesc(MdSupplier::getCreateTime));
        List<SupplierVO> records = page.getRecords().stream().map(this::toVO).collect(Collectors.toList());
        Page<SupplierVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        voPage.setRecords(records);
        return PageResult.of(voPage);
    }

    @Override
    public SupplierVO getSupplierDetail(Long id) {
        MdSupplier supplier = this.getById(id);
        if (supplier == null) {
            throw new BizException("供应商不存在");
        }
        return toVO(supplier);
    }

    @Override
    public Long createSupplier(SupplierSaveRequest request) {
        checkCodeUnique(request.getSupplierCode(), null);
        MdSupplier supplier = new MdSupplier();
        BeanUtils.copyProperties(request, supplier);
        if (supplier.getLevel() == null) {
            supplier.setLevel(3);
        }
        this.save(supplier);
        return supplier.getId();
    }

    @Override
    public void updateSupplier(SupplierSaveRequest request) {
        if (request.getId() == null) {
            throw new BizException("id不能为空");
        }
        MdSupplier exist = this.getById(request.getId());
        if (exist == null) {
            throw new BizException("供应商不存在");
        }
        if (!exist.getSupplierCode().equals(request.getSupplierCode())) {
            checkCodeUnique(request.getSupplierCode(), request.getId());
        }
        MdSupplier supplier = new MdSupplier();
        BeanUtils.copyProperties(request, supplier);
        this.updateById(supplier);
    }

    @Override
    public void deleteSupplier(Long id) {
        MdSupplier exist = this.getById(id);
        if (exist == null) {
            throw new BizException("供应商不存在");
        }
        this.removeById(id);
    }

    private void checkCodeUnique(String supplierCode, Long excludeId) {
        Long count = this.count(Wrappers.<MdSupplier>lambdaQuery()
                .eq(MdSupplier::getSupplierCode, supplierCode)
                .ne(excludeId != null, MdSupplier::getId, excludeId));
        if (count != null && count > 0) {
            throw new BizException("供应商编码已存在");
        }
    }

    private SupplierVO toVO(MdSupplier supplier) {
        SupplierVO vo = new SupplierVO();
        BeanUtils.copyProperties(supplier, vo);
        return vo;
    }
}








