package com.erp.ic.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.erp.wf.integration.ApprovalGate;
import com.erp.wf.integration.ICTransferApprovalListener;
import com.erp.common.api.PageResult;
import com.erp.common.exception.BizException;
import com.erp.ic.domain.dto.IcTransferEntryRequest;
import com.erp.ic.domain.dto.IcTransferSaveRequest;
import com.erp.ic.domain.entity.IcTransfer;
import com.erp.ic.domain.entity.IcTransferEntry;
import com.erp.ic.domain.vo.IcTransferDetailVO;
import com.erp.ic.domain.vo.IcTransferEntryVO;
import com.erp.ic.domain.vo.IcTransferListVO;
import com.erp.ic.mapper.IcTransferEntryMapper;
import com.erp.ic.mapper.IcTransferMapper;
import com.erp.ic.service.IcStockService;
import com.erp.ic.service.IcTransferService;
import com.erp.md.domain.entity.MdMaterial;
import com.erp.md.domain.entity.MdWarehouse;
import com.erp.md.mapper.MdMaterialMapper;
import com.erp.md.mapper.MdWarehouseMapper;
import com.erp.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class IcTransferServiceImpl implements IcTransferService {

    private static final String NO_PREFIX = "DB-";
    private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyyMM");

    private final IcTransferMapper transferMapper;
    private final IcTransferEntryMapper transferEntryMapper;
    private final MdWarehouseMapper warehouseMapper;
    private final MdMaterialMapper materialMapper;
    private final IcStockService stockService;

    private final ApprovalGate approvalGate;

    @Override
    public PageResult<IcTransferListVO> pageTransfer(int pageNum, int pageSize, Long fromWarehouseId, Long toWarehouseId, Integer status) {
        Page<IcTransfer> page = transferMapper.selectPage(new Page<>(pageNum, pageSize), Wrappers.<IcTransfer>lambdaQuery()
                .eq(fromWarehouseId != null, IcTransfer::getFromWarehouseId, fromWarehouseId)
                .eq(toWarehouseId != null, IcTransfer::getToWarehouseId, toWarehouseId)
                .eq(status != null, IcTransfer::getStatus, status)
                .orderByDesc(IcTransfer::getTransferDate)
                .orderByDesc(IcTransfer::getId));
        List<IcTransferListVO> records = page.getRecords().stream().map(this::toListVO).collect(Collectors.toList());
        Page<IcTransferListVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        voPage.setRecords(records);
        return PageResult.of(voPage);
    }

    @Override
    public IcTransferDetailVO getTransferDetail(Long id) {
        IcTransfer transfer = transferMapper.selectById(id);
        if (transfer == null) {
            throw new BizException("调拨单不存在");
        }
        IcTransferDetailVO detail = new IcTransferDetailVO();
        BeanUtils.copyProperties(transfer, detail);
        detail.setEntries(loadEntries(id));
        return detail;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createTransfer(IcTransferSaveRequest request) {
        validateWarehouses(request);
        validateEntries(request.getEntries());
        IcTransfer transfer = new IcTransfer();
        transfer.setTransferNo(nextNo(request.getTransferDate()));
        transfer.setFromWarehouseId(request.getFromWarehouseId());
        transfer.setToWarehouseId(request.getToWarehouseId());
        transfer.setTransferDate(request.getTransferDate());
        transfer.setStatus(IcTransfer.STATUS_DRAFT);
        transfer.setRemark(request.getRemark());
        transferMapper.insert(transfer);
        saveEntries(transfer.getId(), request.getEntries());
        return transfer.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateTransfer(IcTransferSaveRequest request) {
        if (request.getId() == null) {
            throw new BizException("id不能为空");
        }
        IcTransfer exist = transferMapper.selectById(request.getId());
        if (exist == null) {
            throw new BizException("调拨单不存在");
        }
        if (exist.getStatus() == null || exist.getStatus() != IcTransfer.STATUS_DRAFT) {
            throw new BizException("只有暂存状态的调拨单才能修改");
        }
        validateWarehouses(request);
        validateEntries(request.getEntries());
        IcTransfer transfer = new IcTransfer();
        transfer.setId(request.getId());
        transfer.setFromWarehouseId(request.getFromWarehouseId());
        transfer.setToWarehouseId(request.getToWarehouseId());
        transfer.setTransferDate(request.getTransferDate());
        transfer.setRemark(request.getRemark());
        transferMapper.updateById(transfer);
        transferEntryMapper.delete(Wrappers.<IcTransferEntry>lambdaQuery().eq(IcTransferEntry::getTransferId, request.getId()));
        saveEntries(request.getId(), request.getEntries());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteTransfer(Long id) {
        IcTransfer exist = transferMapper.selectById(id);
        if (exist == null) {
            throw new BizException("调拨单不存在");
        }
        if (exist.getStatus() == null || exist.getStatus() != IcTransfer.STATUS_DRAFT) {
            throw new BizException("只有暂存状态的调拨单才能删除");
        }
        transferMapper.deleteById(id);
        transferEntryMapper.delete(Wrappers.<IcTransferEntry>lambdaQuery().eq(IcTransferEntry::getTransferId, id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void auditTransfer(Long id) {
        IcTransfer transfer = transferMapper.selectById(id);
        if (transfer == null) {
            throw new BizException("调拨单不存在");
        }
        if (transfer.getStatus() == null || transfer.getStatus() != IcTransfer.STATUS_DRAFT) {
            throw new BizException("只有暂存状态的调拨单才能审核");
        }

        // 若该业务类型配置了审批流，审核动作改由审批通过后回调触发
        if (approvalGate.tryStart(ICTransferApprovalListener.BIZ_TYPE, id,
                "库存调拨" + transfer.getTransferNo(), null, null)) {
            return;
        }
        List<IcTransferEntry> entries = transferEntryMapper.selectList(Wrappers.<IcTransferEntry>lambdaQuery()
                .eq(IcTransferEntry::getTransferId, id)
                .orderByAsc(IcTransferEntry::getId));
        if (entries.isEmpty()) {
            throw new BizException("调拨单明细为空");
        }
        Map<Long, MdMaterial> materialMap = loadMaterials(entries);
        for (IcTransferEntry entry : entries) {
            MdMaterial material = materialMap.get(entry.getMaterialId());
            String batchNo = normalizeBatchNo(entry.getBatchNo());
            if (material != null && material.getTrackBatch() != null && material.getTrackBatch() == 1 && !StringUtils.hasText(batchNo)) {
                throw new BizException("批次管理物料必须填写批次号：" + material.getMaterialCode());
            }
            stockService.decreaseStock(entry.getMaterialId(), transfer.getFromWarehouseId(), batchNo,
                    entry.getQty(), "IC_TRANSFER_OUT", transfer.getId(), transfer.getTransferNo(), transfer.getTransferDate(),
                    "调拨出库");
            stockService.increaseStock(entry.getMaterialId(), transfer.getToWarehouseId(), batchNo,
                    entry.getQty(), "IC_TRANSFER_IN", transfer.getId(), transfer.getTransferNo(), transfer.getTransferDate(),
                    "调拨入库");
        }
        IcTransfer update = new IcTransfer();
        update.setId(id);
        update.setStatus(IcTransfer.STATUS_AUDITED);
        update.setAuditBy(SecurityUtils.getCurrentUserId());
        update.setAuditTime(LocalDateTime.now());
        transferMapper.updateById(update);
    }

    private void validateWarehouses(IcTransferSaveRequest request) {
        if (request.getFromWarehouseId().equals(request.getToWarehouseId())) {
            throw new BizException("调出仓库与调入仓库不能相同");
        }
        MdWarehouse from = warehouseMapper.selectById(request.getFromWarehouseId());
        if (from == null) {
            throw new BizException("调出仓库不存在");
        }
        MdWarehouse to = warehouseMapper.selectById(request.getToWarehouseId());
        if (to == null) {
            throw new BizException("调入仓库不存在");
        }
    }

    private void validateEntries(List<IcTransferEntryRequest> entryRequests) {
        if (entryRequests == null || entryRequests.isEmpty()) {
            throw new BizException("调拨明细不能为空");
        }
        List<Long> materialIds = entryRequests.stream()
                .map(IcTransferEntryRequest::getMaterialId)
                .distinct()
                .collect(Collectors.toList());
        Map<Long, MdMaterial> materialMap = materialMapper.selectList(Wrappers.<MdMaterial>lambdaQuery()
                        .in(MdMaterial::getId, materialIds))
                .stream()
                .collect(Collectors.toMap(MdMaterial::getId, Function.identity()));
        for (IcTransferEntryRequest entryRequest : entryRequests) {
            MdMaterial material = materialMap.get(entryRequest.getMaterialId());
            if (material == null) {
                throw new BizException("物料不存在：" + entryRequest.getMaterialId());
            }
            if (material.getStatus() != null && material.getStatus() != 0) {
                throw new BizException("物料已停用：" + material.getMaterialCode());
            }
        }
    }

    private void saveEntries(Long transferId, List<IcTransferEntryRequest> entryRequests) {
        for (IcTransferEntryRequest entryRequest : entryRequests) {
            IcTransferEntry entry = new IcTransferEntry();
            entry.setTransferId(transferId);
            entry.setMaterialId(entryRequest.getMaterialId());
            entry.setQty(entryRequest.getQty());
            entry.setBatchNo(StringUtils.hasText(entryRequest.getBatchNo()) ? entryRequest.getBatchNo() : "");
            entry.setRemark(entryRequest.getRemark());
            transferEntryMapper.insert(entry);
        }
    }

    private Map<Long, MdMaterial> loadMaterials(List<IcTransferEntry> entries) {
        List<Long> materialIds = entries.stream().map(IcTransferEntry::getMaterialId).distinct().collect(Collectors.toList());
        return materialMapper.selectList(Wrappers.<MdMaterial>lambdaQuery().in(MdMaterial::getId, materialIds))
                .stream()
                .collect(Collectors.toMap(MdMaterial::getId, Function.identity()));
    }

    private List<IcTransferEntryVO> loadEntries(Long transferId) {
        List<IcTransferEntry> entries = transferEntryMapper.selectList(Wrappers.<IcTransferEntry>lambdaQuery()
                .eq(IcTransferEntry::getTransferId, transferId)
                .orderByAsc(IcTransferEntry::getId));
        if (entries.isEmpty()) {
            return Collections.emptyList();
        }
        Map<Long, MdMaterial> materialMap = loadMaterials(entries);
        return entries.stream().map(entry -> {
            IcTransferEntryVO vo = new IcTransferEntryVO();
            BeanUtils.copyProperties(entry, vo);
            MdMaterial material = materialMap.get(entry.getMaterialId());
            if (material != null) {
                vo.setMaterialCode(material.getMaterialCode());
                vo.setMaterialName(material.getMaterialName());
            }
            return vo;
        }).collect(Collectors.toList());
    }

    private String nextNo(LocalDate transferDate) {
        String prefix = NO_PREFIX + transferDate.format(MONTH_FORMATTER) + "-";
        String lastNo = transferMapper.selectLastTransferNo(prefix);
        int next = 1;
        if (StringUtils.hasText(lastNo)) {
            int idx = lastNo.lastIndexOf('-');
            if (idx >= 0 && idx < lastNo.length() - 1) {
                try {
                    next = Integer.parseInt(lastNo.substring(idx + 1)) + 1;
                } catch (NumberFormatException ignored) {
                    next = 1;
                }
            }
        }
        return prefix + String.format("%04d", next);
    }

    private String normalizeBatchNo(String batchNo) {
        return StringUtils.hasText(batchNo) ? batchNo : "";
    }

    private IcTransferListVO toListVO(IcTransfer transfer) {
        IcTransferListVO vo = new IcTransferListVO();
        BeanUtils.copyProperties(transfer, vo);
        MdWarehouse from = warehouseMapper.selectById(transfer.getFromWarehouseId());
        if (from != null) {
            vo.setFromWarehouseName(from.getWarehouseName());
        }
        MdWarehouse to = warehouseMapper.selectById(transfer.getToWarehouseId());
        if (to != null) {
            vo.setToWarehouseName(to.getWarehouseName());
        }
        return vo;
    }
}








