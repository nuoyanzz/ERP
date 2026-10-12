package com.erp.pm.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.erp.wf.integration.ApprovalGate;
import com.erp.wf.integration.PMMoApprovalListener;
import com.erp.common.api.PageResult;
import com.erp.common.exception.BizException;
import com.erp.md.domain.entity.MdMaterial;
import com.erp.md.domain.entity.MdWarehouse;
import com.erp.md.mapper.MdMaterialMapper;
import com.erp.md.mapper.MdWarehouseMapper;
import com.erp.pm.domain.dto.PmMoSaveRequest;
import com.erp.pm.domain.entity.PmBom;
import com.erp.pm.domain.entity.PmBomEntry;
import com.erp.pm.domain.entity.PmMo;
import com.erp.pm.domain.entity.PmMoEntry;
import com.erp.pm.domain.vo.PmMoEntryVO;
import com.erp.pm.domain.vo.PmMoVO;
import com.erp.pm.mapper.PmBomEntryMapper;
import com.erp.pm.mapper.PmBomMapper;
import com.erp.pm.mapper.PmMoEntryMapper;
import com.erp.pm.mapper.PmMoMapper;
import com.erp.pm.service.PmMoService;
import com.erp.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
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
public class PmMoServiceImpl extends ServiceImpl<PmMoMapper, PmMo> implements PmMoService {

    private static final String NO_PREFIX = "MO-";
    private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyyMM");
    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");

    private final PmBomMapper bomMapper;
    private final PmBomEntryMapper bomEntryMapper;
    private final PmMoEntryMapper moEntryMapper;
    private final MdMaterialMapper materialMapper;
    private final MdWarehouseMapper warehouseMapper;

    private final ApprovalGate approvalGate;

    @Override
    public PageResult<PmMoVO> pageMo(int pageNum, int pageSize, Long materialId, Integer status) {
        Page<PmMo> page = this.page(new Page<>(pageNum, pageSize), Wrappers.<PmMo>lambdaQuery()
                .eq(materialId != null, PmMo::getMaterialId, materialId)
                .eq(status != null, PmMo::getStatus, status)
                .orderByDesc(PmMo::getId));
        List<PmMoVO> records = page.getRecords().stream().map(this::toVO).collect(Collectors.toList());
        Page<PmMoVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        voPage.setRecords(records);
        return PageResult.of(voPage);
    }

    @Override
    public PmMoVO getMoDetail(Long id) {
        PmMo mo = this.getById(id);
        if (mo == null) {
            throw new BizException("生产订单不存在");
        }
        PmMoVO vo = toVO(mo);
        vo.setEntries(loadEntries(id));
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createMo(PmMoSaveRequest request) {
        validate(request);
        PmMo mo = new PmMo();
        mo.setMoNo(nextNo(request.getPlanStartDate()));
        mo.setMaterialId(request.getMaterialId());
        mo.setBomId(request.getBomId());
        mo.setQty(request.getQty());
        mo.setWarehouseId(request.getWarehouseId());
        mo.setPlanStartDate(request.getPlanStartDate());
        mo.setPlanEndDate(request.getPlanEndDate());
        mo.setStatus(PmMo.STATUS_DRAFT);
        mo.setRemark(request.getRemark());
        this.save(mo);
        return mo.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateMo(PmMoSaveRequest request) {
        if (request.getId() == null) {
            throw new BizException("id不能为空");
        }
        PmMo exist = this.getById(request.getId());
        if (exist == null) {
            throw new BizException("生产订单不存在");
        }
        if (exist.getStatus() == null || exist.getStatus() != PmMo.STATUS_DRAFT) {
            throw new BizException("只有暂存状态的生产订单才能修改");
        }
        validate(request);
        PmMo mo = new PmMo();
        mo.setId(request.getId());
        mo.setMaterialId(request.getMaterialId());
        mo.setBomId(request.getBomId());
        mo.setQty(request.getQty());
        mo.setWarehouseId(request.getWarehouseId());
        mo.setPlanStartDate(request.getPlanStartDate());
        mo.setPlanEndDate(request.getPlanEndDate());
        mo.setRemark(request.getRemark());
        this.updateById(mo);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteMo(Long id) {
        PmMo exist = this.getById(id);
        if (exist == null) {
            throw new BizException("生产订单不存在");
        }
        if (exist.getStatus() == null || exist.getStatus() != PmMo.STATUS_DRAFT) {
            throw new BizException("只有暂存状态的生产订单才能删除");
        }
        this.removeById(id);
        moEntryMapper.delete(Wrappers.<PmMoEntry>lambdaQuery().eq(PmMoEntry::getMoId, id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void auditMo(Long id) {
        PmMo mo = this.getById(id);
        if (mo == null) {
            throw new BizException("生产订单不存在");
        }
        if (mo.getStatus() == null || mo.getStatus() != PmMo.STATUS_DRAFT) {
            throw new BizException("只有暂存状态的生产订单才能审核");
        }

        // 若该业务类型配置了审批流，审核动作改由审批通过后回调触发
        if (approvalGate.tryStart(PMMoApprovalListener.BIZ_TYPE, id,
                "生产订单" + mo.getMoNo(), mo.getQty(), null)) {
            return;
        }
        PmBom bom = bomMapper.selectById(mo.getBomId());
        if (bom == null || bom.getStatus() == null || bom.getStatus() != PmBom.STATUS_ENABLED) {
            throw new BizException("BOM 不存在或未启用");
        }
        if (!bom.getMaterialId().equals(mo.getMaterialId())) {
            throw new BizException("BOM 父项物料与生产物料不一致");
        }
        List<PmBomEntry> bomEntries = bomEntryMapper.selectList(Wrappers.<PmBomEntry>lambdaQuery()
                .eq(PmBomEntry::getBomId, bom.getId())
                .orderByAsc(PmBomEntry::getId));
        if (bomEntries.isEmpty()) {
            throw new BizException("BOM 明细为空");
        }
        moEntryMapper.delete(Wrappers.<PmMoEntry>lambdaQuery().eq(PmMoEntry::getMoId, id));
        // BOM 用量是「每 baseQty 个父件」的消耗量，必须先按基准数量换算，否则 baseQty != 1 时需求数量会错
        BigDecimal baseQty = bom.getBaseQty() == null || bom.getBaseQty().compareTo(BigDecimal.ZERO) <= 0
                ? BigDecimal.ONE : bom.getBaseQty();
        for (PmBomEntry bomEntry : bomEntries) {
            BigDecimal lossRate = bomEntry.getLossRate() == null ? BigDecimal.ZERO : bomEntry.getLossRate();
            BigDecimal requireQty = mo.getQty()
                    .divide(baseQty, 10, RoundingMode.HALF_UP)
                    .multiply(bomEntry.getQty())
                    .multiply(BigDecimal.ONE.add(lossRate.divide(ONE_HUNDRED, 6, RoundingMode.HALF_UP)))
                    .setScale(4, RoundingMode.HALF_UP);
            PmMoEntry entry = new PmMoEntry();
            entry.setMoId(id);
            entry.setBomEntryId(bomEntry.getId());
            entry.setMaterialId(bomEntry.getMaterialId());
            entry.setRequireQty(requireQty);
            entry.setPickedQty(BigDecimal.ZERO);
            entry.setRemark(bomEntry.getRemark());
            moEntryMapper.insert(entry);
        }
        PmMo update = new PmMo();
        update.setId(id);
        update.setStatus(PmMo.STATUS_AUDITED);
        update.setAuditBy(SecurityUtils.getCurrentUserId());
        update.setAuditTime(LocalDateTime.now());
        this.updateById(update);
    }

    private void validate(PmMoSaveRequest request) {
        MdMaterial material = materialMapper.selectById(request.getMaterialId());
        if (material == null) {
            throw new BizException("生产物料不存在");
        }
        MdWarehouse warehouse = warehouseMapper.selectById(request.getWarehouseId());
        if (warehouse == null) {
            throw new BizException("成品仓库不存在");
        }
        PmBom bom = bomMapper.selectById(request.getBomId());
        if (bom == null) {
            throw new BizException("BOM 不存在");
        }
        if (!bom.getMaterialId().equals(request.getMaterialId())) {
            throw new BizException("BOM 父项物料与生产物料不一致");
        }
        if (request.getQty() == null || request.getQty().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BizException("生产数量必须大于0");
        }
    }

    private List<PmMoEntryVO> loadEntries(Long moId) {
        List<PmMoEntry> entries = moEntryMapper.selectList(Wrappers.<PmMoEntry>lambdaQuery()
                .eq(PmMoEntry::getMoId, moId)
                .orderByAsc(PmMoEntry::getId));
        if (entries.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> materialIds = entries.stream().map(PmMoEntry::getMaterialId).distinct().collect(Collectors.toList());
        Map<Long, MdMaterial> materialMap = materialMapper.selectList(Wrappers.<MdMaterial>lambdaQuery()
                        .in(MdMaterial::getId, materialIds))
                .stream()
                .collect(Collectors.toMap(MdMaterial::getId, Function.identity()));
        return entries.stream().map(entry -> {
            PmMoEntryVO vo = new PmMoEntryVO();
            BeanUtils.copyProperties(entry, vo);
            MdMaterial material = materialMap.get(entry.getMaterialId());
            if (material != null) {
                vo.setMaterialCode(material.getMaterialCode());
                vo.setMaterialName(material.getMaterialName());
            }
            return vo;
        }).collect(Collectors.toList());
    }

    private PmMoVO toVO(PmMo mo) {
        PmMoVO vo = new PmMoVO();
        BeanUtils.copyProperties(mo, vo);
        MdMaterial material = materialMapper.selectById(mo.getMaterialId());
        if (material != null) {
            vo.setMaterialCode(material.getMaterialCode());
            vo.setMaterialName(material.getMaterialName());
        }
        PmBom bom = bomMapper.selectById(mo.getBomId());
        if (bom != null) {
            vo.setBomNo(bom.getBomNo());
        }
        MdWarehouse warehouse = warehouseMapper.selectById(mo.getWarehouseId());
        if (warehouse != null) {
            vo.setWarehouseName(warehouse.getWarehouseName());
        }
        return vo;
    }

    private String nextNo(LocalDate date) {
        String prefix = NO_PREFIX + date.format(MONTH_FORMATTER) + "-";
        return prefix + nextSeq(baseMapper.selectLastMoNo(prefix));
    }

    private String nextSeq(String lastNo) {
        int next = 1;
        if (lastNo != null) {
            int idx = lastNo.lastIndexOf('-');
            if (idx >= 0 && idx < lastNo.length() - 1) {
                try {
                    next = Integer.parseInt(lastNo.substring(idx + 1)) + 1;
                } catch (NumberFormatException ignored) {
                    next = 1;
                }
            }
        }
        return String.format("%04d", next);
    }
}








