package com.erp.md.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.erp.common.api.PageResult;
import com.erp.common.exception.BizException;
import com.erp.md.domain.dto.WarehouseQueryRequest;
import com.erp.md.domain.dto.WarehouseSaveRequest;
import com.erp.md.domain.entity.MdWarehouse;
import com.erp.md.domain.vo.WarehouseVO;
import com.erp.md.mapper.MdWarehouseMapper;
import com.erp.md.service.MdWarehouseService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MdWarehouseServiceImpl extends ServiceImpl<MdWarehouseMapper, MdWarehouse> implements MdWarehouseService {

    @Override
    public PageResult<WarehouseVO> pageWarehouse(WarehouseQueryRequest request) {
        Page<MdWarehouse> page = this.page(request.toPage(), Wrappers.<MdWarehouse>lambdaQuery()
                .like(StringUtils.hasText(request.getWarehouseCode()), MdWarehouse::getWarehouseCode, request.getWarehouseCode())
                .like(StringUtils.hasText(request.getWarehouseName()), MdWarehouse::getWarehouseName, request.getWarehouseName())
                .eq(request.getWhType() != null, MdWarehouse::getWhType, request.getWhType())
                .eq(request.getStatus() != null, MdWarehouse::getStatus, request.getStatus())
                .orderByDesc(MdWarehouse::getCreateTime));
        List<WarehouseVO> records = page.getRecords().stream().map(this::toVO).collect(Collectors.toList());
        Page<WarehouseVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        voPage.setRecords(records);
        return PageResult.of(voPage);
    }

    @Override
    public WarehouseVO getWarehouseDetail(Long id) {
        MdWarehouse warehouse = this.getById(id);
        if (warehouse == null) {
            throw new BizException("仓库不存在");
        }
        return toVO(warehouse);
    }

    @Override
    public Long createWarehouse(WarehouseSaveRequest request) {
        checkCodeUnique(request.getWarehouseCode(), null);
        MdWarehouse warehouse = new MdWarehouse();
        BeanUtils.copyProperties(request, warehouse);
        if (warehouse.getWhType() == null) {
            warehouse.setWhType(1);
        }
        this.save(warehouse);
        return warehouse.getId();
    }

    @Override
    public void updateWarehouse(WarehouseSaveRequest request) {
        if (request.getId() == null) {
            throw new BizException("id不能为空");
        }
        MdWarehouse exist = this.getById(request.getId());
        if (exist == null) {
            throw new BizException("仓库不存在");
        }
        if (!exist.getWarehouseCode().equals(request.getWarehouseCode())) {
            checkCodeUnique(request.getWarehouseCode(), request.getId());
        }
        MdWarehouse warehouse = new MdWarehouse();
        BeanUtils.copyProperties(request, warehouse);
        this.updateById(warehouse);
    }

    @Override
    public void deleteWarehouse(Long id) {
        MdWarehouse exist = this.getById(id);
        if (exist == null) {
            throw new BizException("仓库不存在");
        }
        this.removeById(id);
    }

    private void checkCodeUnique(String warehouseCode, Long excludeId) {
        Long count = this.count(Wrappers.<MdWarehouse>lambdaQuery()
                .eq(MdWarehouse::getWarehouseCode, warehouseCode)
                .ne(excludeId != null, MdWarehouse::getId, excludeId));
        if (count != null && count > 0) {
            throw new BizException("仓库编码已存在");
        }
    }

    private WarehouseVO toVO(MdWarehouse warehouse) {
        WarehouseVO vo = new WarehouseVO();
        BeanUtils.copyProperties(warehouse, vo);
        return vo;
    }
}








