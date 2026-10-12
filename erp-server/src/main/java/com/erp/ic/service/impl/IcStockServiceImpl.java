package com.erp.ic.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.erp.common.api.PageResult;
import com.erp.common.exception.BizException;
import com.erp.ic.domain.dto.StockQueryRequest;
import com.erp.ic.domain.dto.TransactionQueryRequest;
import com.erp.ic.domain.entity.IcStock;
import com.erp.ic.domain.entity.IcTransaction;
import com.erp.ic.domain.vo.IcStockVO;
import com.erp.ic.domain.vo.IcTransactionVO;
import com.erp.ic.mapper.IcStockMapper;
import com.erp.ic.mapper.IcTransactionMapper;
import com.erp.ic.service.IcStockService;
import com.erp.md.domain.entity.MdMaterial;
import com.erp.md.domain.entity.MdWarehouse;
import com.erp.md.mapper.MdMaterialMapper;
import com.erp.md.mapper.MdWarehouseMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class IcStockServiceImpl extends ServiceImpl<IcStockMapper, IcStock> implements IcStockService {

    private final IcTransactionMapper transactionMapper;
    private final MdMaterialMapper materialMapper;
    private final MdWarehouseMapper warehouseMapper;

    @Override
    public PageResult<IcStockVO> pageStock(StockQueryRequest request) {
        List<Long> materialIds = resolveMaterialIds(request.getMaterialCode(), request.getMaterialName());
        if (materialIds != null && materialIds.isEmpty()) {
            return PageResult.of(new Page<>(request.getPageNum(), request.getPageSize(), 0));
        }
        Page<IcStock> page = this.page(request.toPage(), Wrappers.<IcStock>lambdaQuery()
                .in(materialIds != null, IcStock::getMaterialId, materialIds)
                .eq(request.getWarehouseId() != null, IcStock::getWarehouseId, request.getWarehouseId())
                .orderByDesc(IcStock::getUpdateTime));
        Map<Long, MdMaterial> materialMap = loadMaterials(page.getRecords());
        Map<Long, MdWarehouse> warehouseMap = loadWarehouses(page.getRecords());
        List<IcStockVO> records = page.getRecords().stream()
                .map(stock -> toStockVO(stock, materialMap, warehouseMap))
                .collect(Collectors.toList());
        Page<IcStockVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        voPage.setRecords(records);
        return PageResult.of(voPage);
    }

    @Override
    public PageResult<IcTransactionVO> pageTransaction(TransactionQueryRequest request) {
        Page<IcTransaction> page = transactionMapper.selectPage(request.toPage(), Wrappers.<IcTransaction>lambdaQuery()
                .eq(request.getMaterialId() != null, IcTransaction::getMaterialId, request.getMaterialId())
                .eq(StringUtils.hasText(request.getTranType()), IcTransaction::getTranType, request.getTranType())
                .eq(StringUtils.hasText(request.getBizNo()), IcTransaction::getBizNo, request.getBizNo())
                .eq(request.getDirection() != null, IcTransaction::getDirection, request.getDirection())
                .ge(request.getStartDate() != null, IcTransaction::getBizDate, request.getStartDate())
                .le(request.getEndDate() != null, IcTransaction::getBizDate, request.getEndDate())
                .orderByDesc(IcTransaction::getBizDate)
                .orderByDesc(IcTransaction::getId));
        List<Long> materialIds = page.getRecords().stream()
                .map(IcTransaction::getMaterialId)
                .distinct()
                .collect(Collectors.toList());
        List<Long> warehouseIds = page.getRecords().stream()
                .map(IcTransaction::getWarehouseId)
                .distinct()
                .collect(Collectors.toList());
        Map<Long, MdMaterial> materialMap = loadMaterialsByIds(materialIds);
        Map<Long, MdWarehouse> warehouseMap = loadWarehousesByIds(warehouseIds);
        List<IcTransactionVO> records = page.getRecords().stream()
                .map(tran -> toTransactionVO(tran, materialMap, warehouseMap))
                .collect(Collectors.toList());
        Page<IcTransactionVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        voPage.setRecords(records);
        return PageResult.of(voPage);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void increaseStock(Long materialId, Long warehouseId, String batchNo, BigDecimal qty,
                              String tranType, Long bizId, String bizNo, LocalDate bizDate, String remark) {
        if (qty == null || qty.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BizException("数量必须大于0");
        }
        IcStock stock = baseMapper.selectForUpdate(materialId, warehouseId, normalizeBatchNo(batchNo));
        if (stock == null) {
            stock = new IcStock();
            stock.setMaterialId(materialId);
            stock.setWarehouseId(warehouseId);
            stock.setBatchNo(normalizeBatchNo(batchNo));
            stock.setQty(qty);
            stock.setLockedQty(BigDecimal.ZERO);
            stock.setAvailableQty(qty);
            this.save(stock);
        } else {
            stock.setQty(stock.getQty().add(qty));
            stock.setAvailableQty(stock.getQty().subtract(stock.getLockedQty()));
            this.updateById(stock);
        }
        recordTransaction(materialId, warehouseId, batchNo, tranType, IcTransaction.DIRECTION_IN, qty, bizId, bizNo, bizDate, remark);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void decreaseStock(Long materialId, Long warehouseId, String batchNo, BigDecimal qty,
                              String tranType, Long bizId, String bizNo, LocalDate bizDate, String remark) {
        if (qty == null || qty.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BizException("数量必须大于0");
        }
        IcStock stock = baseMapper.selectForUpdate(materialId, warehouseId, normalizeBatchNo(batchNo));
        if (stock == null || stock.getAvailableQty().compareTo(qty) < 0) {
            throw new BizException("库存可用量不足");
        }
        stock.setQty(stock.getQty().subtract(qty));
        stock.setAvailableQty(stock.getQty().subtract(stock.getLockedQty()));
        this.updateById(stock);
        recordTransaction(materialId, warehouseId, batchNo, tranType, IcTransaction.DIRECTION_OUT, qty, bizId, bizNo, bizDate, remark);
    }

    private void recordTransaction(Long materialId, Long warehouseId, String batchNo, String tranType,
                                   Integer direction, BigDecimal qty, Long bizId, String bizNo,
                                   LocalDate bizDate, String remark) {
        IcTransaction transaction = new IcTransaction();
        transaction.setMaterialId(materialId);
        transaction.setWarehouseId(warehouseId);
        transaction.setBatchNo(normalizeBatchNo(batchNo));
        transaction.setTranType(tranType);
        transaction.setDirection(direction);
        transaction.setQty(qty);
        transaction.setBizId(bizId);
        transaction.setBizNo(bizNo);
        transaction.setBizDate(bizDate);
        transaction.setRemark(remark);
        transactionMapper.insert(transaction);
    }

    private String normalizeBatchNo(String batchNo) {
        return StringUtils.hasText(batchNo) ? batchNo : "";
    }

    private List<Long> resolveMaterialIds(String materialCode, String materialName) {
        if (!StringUtils.hasText(materialCode) && !StringUtils.hasText(materialName)) {
            return null;
        }
        List<MdMaterial> materials = materialMapper.selectList(Wrappers.<MdMaterial>lambdaQuery()
                .like(StringUtils.hasText(materialCode), MdMaterial::getMaterialCode, materialCode)
                .like(StringUtils.hasText(materialName), MdMaterial::getMaterialName, materialName));
        return materials.stream().map(MdMaterial::getId).collect(Collectors.toList());
    }

    private Map<Long, MdMaterial> loadMaterials(List<IcStock> stocks) {
        List<Long> materialIds = stocks.stream().map(IcStock::getMaterialId).distinct().collect(Collectors.toList());
        return loadMaterialsByIds(materialIds);
    }

    private Map<Long, MdMaterial> loadMaterialsByIds(List<Long> materialIds) {
        if (materialIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return materialMapper.selectList(Wrappers.<MdMaterial>lambdaQuery().in(MdMaterial::getId, materialIds))
                .stream()
                .collect(Collectors.toMap(MdMaterial::getId, Function.identity()));
    }

    private Map<Long, MdWarehouse> loadWarehouses(List<IcStock> stocks) {
        List<Long> warehouseIds = stocks.stream().map(IcStock::getWarehouseId).distinct().collect(Collectors.toList());
        return loadWarehousesByIds(warehouseIds);
    }

    private Map<Long, MdWarehouse> loadWarehousesByIds(List<Long> warehouseIds) {
        if (warehouseIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return warehouseMapper.selectList(Wrappers.<MdWarehouse>lambdaQuery().in(MdWarehouse::getId, warehouseIds))
                .stream()
                .collect(Collectors.toMap(MdWarehouse::getId, Function.identity()));
    }

    private IcStockVO toStockVO(IcStock stock, Map<Long, MdMaterial> materialMap, Map<Long, MdWarehouse> warehouseMap) {
        IcStockVO vo = new IcStockVO();
        BeanUtils.copyProperties(stock, vo);
        MdMaterial material = materialMap.get(stock.getMaterialId());
        if (material != null) {
            vo.setMaterialCode(material.getMaterialCode());
            vo.setMaterialName(material.getMaterialName());
            vo.setSpec(material.getSpec());
            vo.setUnit(material.getUnit());
        }
        MdWarehouse warehouse = warehouseMap.get(stock.getWarehouseId());
        if (warehouse != null) {
            vo.setWarehouseCode(warehouse.getWarehouseCode());
            vo.setWarehouseName(warehouse.getWarehouseName());
        }
        return vo;
    }

    private IcTransactionVO toTransactionVO(IcTransaction transaction, Map<Long, MdMaterial> materialMap, Map<Long, MdWarehouse> warehouseMap) {
        IcTransactionVO vo = new IcTransactionVO();
        BeanUtils.copyProperties(transaction, vo);
        MdMaterial material = materialMap.get(transaction.getMaterialId());
        if (material != null) {
            vo.setMaterialCode(material.getMaterialCode());
            vo.setMaterialName(material.getMaterialName());
        }
        MdWarehouse warehouse = warehouseMap.get(transaction.getWarehouseId());
        if (warehouse != null) {
            vo.setWarehouseName(warehouse.getWarehouseName());
        }
        return vo;
    }
}








