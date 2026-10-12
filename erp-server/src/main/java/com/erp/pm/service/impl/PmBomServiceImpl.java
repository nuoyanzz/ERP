package com.erp.pm.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.erp.common.api.PageResult;
import com.erp.common.exception.BizException;
import com.erp.md.domain.entity.MdMaterial;
import com.erp.md.mapper.MdMaterialMapper;
import com.erp.pm.domain.dto.PmBomEntryRequest;
import com.erp.pm.domain.dto.PmBomSaveRequest;
import com.erp.pm.domain.entity.PmBom;
import com.erp.pm.domain.entity.PmBomEntry;
import com.erp.pm.domain.entity.PmMo;
import com.erp.pm.domain.vo.PmBomEntryVO;
import com.erp.pm.domain.vo.PmBomVO;
import com.erp.pm.mapper.PmBomEntryMapper;
import com.erp.pm.mapper.PmBomMapper;
import com.erp.pm.mapper.PmMoMapper;
import com.erp.pm.service.PmBomService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PmBomServiceImpl extends ServiceImpl<PmBomMapper, PmBom> implements PmBomService {

    private static final String NO_PREFIX = "BOM-";
    private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyyMM");

    private final PmBomEntryMapper bomEntryMapper;
    private final MdMaterialMapper materialMapper;
    private final PmMoMapper moMapper;

    @Override
    public PageResult<PmBomVO> pageBom(int pageNum, int pageSize, Long materialId, Integer status) {
        Page<PmBom> page = this.page(new Page<>(pageNum, pageSize), Wrappers.<PmBom>lambdaQuery()
                .eq(materialId != null, PmBom::getMaterialId, materialId)
                .eq(status != null, PmBom::getStatus, status)
                .orderByDesc(PmBom::getId));
        List<PmBomVO> records = page.getRecords().stream().map(this::toVO).collect(Collectors.toList());
        Page<PmBomVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        voPage.setRecords(records);
        return PageResult.of(voPage);
    }

    @Override
    public PmBomVO getBomDetail(Long id) {
        PmBom bom = this.getById(id);
        if (bom == null) {
            throw new BizException("BOM 不存在");
        }
        PmBomVO vo = toVO(bom);
        vo.setEntries(loadEntries(id));
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createBom(PmBomSaveRequest request) {
        validateEntries(request.getEntries());
        String version = StringUtils.hasText(request.getVersion()) ? request.getVersion() : nextVersion(request.getMaterialId());
        // 同一物料同一版本号只能有一个 BOM
        if (existsByMaterialAndVersion(request.getMaterialId(), version, null)) {
            throw new BizException("该物料已存在版本号为 " + version + " 的 BOM");
        }
        validateDates(request.getEffectiveDate(), request.getExpiryDate());

        PmBom bom = new PmBom();
        bom.setBomNo(nextNo());
        bom.setMaterialId(request.getMaterialId());
        bom.setVersion(version);
        bom.setStatus(PmBom.STATUS_ENABLED);
        bom.setBaseQty(request.getBaseQty() == null ? BigDecimal.ONE : request.getBaseQty());
        bom.setEffectiveDate(request.getEffectiveDate());
        bom.setExpiryDate(request.getExpiryDate());
        boolean wantDefault = request.getIsDefault() == null || request.getIsDefault() == 1;
        bom.setIsDefault(wantDefault ? 1 : 0);
        bom.setRemark(request.getRemark());
        this.save(bom);
        if (wantDefault) {
            // 同一物料只允许一个默认版本，新建默认版本时要把旧版本降级
            clearOtherDefaults(bom.getMaterialId(), bom.getId());
        }
        saveEntries(bom.getId(), request.getEntries());
        return bom.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateBom(PmBomSaveRequest request) {
        if (request.getId() == null) {
            throw new BizException("id不能为空");
        }
        PmBom exist = this.getById(request.getId());
        if (exist == null) {
            throw new BizException("BOM 不存在");
        }
        // 已被已下达（审核/生产中）的生产订单引用的 BOM，不能再改明细，
        // 否则在制工单的领料依据会被篡改。只能新建版本。
        assertNotReferencedByReleasedMo(exist.getId());
        validateEntries(request.getEntries());
        validateDates(request.getEffectiveDate(), request.getExpiryDate());
        String version = StringUtils.hasText(request.getVersion()) ? request.getVersion() : exist.getVersion();
        if (existsByMaterialAndVersion(request.getMaterialId(), version, exist.getId())) {
            throw new BizException("该物料已存在版本号为 " + version + " 的 BOM");
        }
        PmBom bom = new PmBom();
        bom.setId(request.getId());
        bom.setMaterialId(request.getMaterialId());
        bom.setVersion(version);
        bom.setBaseQty(request.getBaseQty() == null
                ? (exist.getBaseQty() == null ? BigDecimal.ONE : exist.getBaseQty()) : request.getBaseQty());
        bom.setEffectiveDate(request.getEffectiveDate());
        bom.setExpiryDate(request.getExpiryDate());
        bom.setRemark(request.getRemark());
        this.updateById(bom);
        boolean wantDefault = request.getIsDefault() == null || request.getIsDefault() == 1;
        if (wantDefault) {
            clearOtherDefaults(bom.getMaterialId(), bom.getId());
        }
        bomEntryMapper.delete(Wrappers.<PmBomEntry>lambdaQuery().eq(PmBomEntry::getBomId, request.getId()));
        saveEntries(request.getId(), request.getEntries());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteBom(Long id) {
        PmBom exist = this.getById(id);
        if (exist == null) {
            throw new BizException("BOM 不存在");
        }
        assertNotReferencedByReleasedMo(id);
        this.removeById(id);
        bomEntryMapper.delete(Wrappers.<PmBomEntry>lambdaQuery().eq(PmBomEntry::getBomId, id));
    }

    /**
     * 已被已下达生产订单引用的 BOM 不允许修改或删除。
     * <p>
     * 已下达 = 状态为已审核 / 生产中 / 已完工的生产订单。
     */
    private void assertNotReferencedByReleasedMo(Long bomId) {
        Long count = moMapper.selectCount(Wrappers.<PmMo>lambdaQuery()
                .eq(PmMo::getBomId, bomId)
                .in(PmMo::getStatus, PmMo.STATUS_AUDITED, PmMo.STATUS_PROCESSING, PmMo.STATUS_FINISHED));
        if (count != null && count > 0) {
            throw new BizException("该 BOM 已被已下达的生产订单引用，不能修改或删除，请新建版本");
        }
    }

    private void validateEntries(List<PmBomEntryRequest> entryRequests) {
        List<Long> materialIds = entryRequests.stream()
                .map(PmBomEntryRequest::getMaterialId)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        if (materialIds.isEmpty()) {
            throw new BizException("BOM 明细不能为空");
        }
        Long count = materialMapper.selectCount(Wrappers.<MdMaterial>lambdaQuery().in(MdMaterial::getId, materialIds));
        if (count == null || count.intValue() != materialIds.size()) {
            throw new BizException("存在无效的子项物料");
        }
        // 替代料也必须是有效物料
        List<Long> substituteIds = entryRequests.stream()
                .filter(entry -> entry.getSubstituteFlag() != null && entry.getSubstituteFlag() == 1)
                .map(PmBomEntryRequest::getSubstituteMaterialId)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        if (!substituteIds.isEmpty()) {
            Long subCount = materialMapper.selectCount(Wrappers.<MdMaterial>lambdaQuery()
                    .in(MdMaterial::getId, substituteIds));
            if (subCount == null || subCount.intValue() != substituteIds.size()) {
                throw new BizException("存在无效的替代料物料");
            }
        }
    }

    private void validateDates(LocalDate effectiveDate, LocalDate expiryDate) {
        if (effectiveDate != null && expiryDate != null && expiryDate.isBefore(effectiveDate)) {
            throw new BizException("失效日期不能早于生效日期");
        }
    }

    private boolean existsByMaterialAndVersion(Long materialId, String version, Long excludeId) {
        Long count = this.count(Wrappers.<PmBom>lambdaQuery()
                .eq(PmBom::getMaterialId, materialId)
                .eq(PmBom::getVersion, version)
                .ne(excludeId != null, PmBom::getId, excludeId));
        return count != null && count > 0;
    }

    /**
     * 把同一物料下其它版本的默认标记清掉，保证同一物料只有一个 is_default = 1。
     */
    private void clearOtherDefaults(Long materialId, Long keepId) {
        List<PmBom> others = this.list(Wrappers.<PmBom>lambdaQuery()
                .eq(PmBom::getMaterialId, materialId)
                .ne(PmBom::getId, keepId)
                .eq(PmBom::getIsDefault, 1));
        for (PmBom other : others) {
            PmBom update = new PmBom();
            update.setId(other.getId());
            update.setIsDefault(0);
            this.updateById(update);
        }
    }

    /**
     * 自动生成下一个版本号：V1、V2、V3...
     */
    private String nextVersion(Long materialId) {
        List<PmBom> boms = this.list(Wrappers.<PmBom>lambdaQuery().eq(PmBom::getMaterialId, materialId));
        int max = 0;
        for (PmBom bom : boms) {
            String version = bom.getVersion();
            if (version == null || !version.startsWith("V")) {
                continue;
            }
            try {
                max = Math.max(max, Integer.parseInt(version.substring(1)));
            } catch (NumberFormatException ignored) {
                // 非 V+数字 的版本号不参与自动编号
            }
        }
        return "V" + (max + 1);
    }

    private void saveEntries(Long bomId, List<PmBomEntryRequest> entryRequests) {
        int position = 0;
        for (PmBomEntryRequest entryRequest : entryRequests) {
            position++;
            PmBomEntry entry = new PmBomEntry();
            entry.setBomId(bomId);
            entry.setMaterialId(entryRequest.getMaterialId());
            entry.setQty(entryRequest.getQty());
            entry.setLossRate(entryRequest.getLossRate() == null ? BigDecimal.ZERO : entryRequest.getLossRate());
            entry.setSubstituteFlag(entryRequest.getSubstituteFlag() == null ? 0 : entryRequest.getSubstituteFlag());
            entry.setSubstituteMaterialId(entryRequest.getSubstituteMaterialId());
            entry.setPositionNo(entryRequest.getPositionNo() == null ? position : entryRequest.getPositionNo());
            entry.setRemark(entryRequest.getRemark());
            bomEntryMapper.insert(entry);
        }
    }

    private List<PmBomEntryVO> loadEntries(Long bomId) {
        List<PmBomEntry> entries = bomEntryMapper.selectList(Wrappers.<PmBomEntry>lambdaQuery()
                .eq(PmBomEntry::getBomId, bomId)
                .orderByAsc(PmBomEntry::getPositionNo)
                .orderByAsc(PmBomEntry::getId));
        if (entries.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> materialIds = new ArrayList<>(entries.stream()
                .map(PmBomEntry::getMaterialId)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .collect(Collectors.toList()));
        entries.stream()
                .map(PmBomEntry::getSubstituteMaterialId)
                .filter(java.util.Objects::nonNull)
                .forEach(materialIds::add);
        Map<Long, MdMaterial> materialMap = materialMapper.selectList(Wrappers.<MdMaterial>lambdaQuery()
                        .in(MdMaterial::getId, materialIds.stream().distinct().collect(Collectors.toList())))
                .stream()
                .collect(Collectors.toMap(MdMaterial::getId, Function.identity()));
        return entries.stream().map(entry -> {
            PmBomEntryVO vo = new PmBomEntryVO();
            BeanUtils.copyProperties(entry, vo);
            MdMaterial material = materialMap.get(entry.getMaterialId());
            if (material != null) {
                vo.setMaterialCode(material.getMaterialCode());
                vo.setMaterialName(material.getMaterialName());
            }
            MdMaterial substitute = materialMap.get(entry.getSubstituteMaterialId());
            if (substitute != null) {
                vo.setSubstituteMaterialCode(substitute.getMaterialCode());
                vo.setSubstituteMaterialName(substitute.getMaterialName());
            }
            return vo;
        }).collect(Collectors.toList());
    }

    private PmBomVO toVO(PmBom bom) {
        PmBomVO vo = new PmBomVO();
        BeanUtils.copyProperties(bom, vo);
        MdMaterial material = materialMapper.selectById(bom.getMaterialId());
        if (material != null) {
            vo.setMaterialCode(material.getMaterialCode());
            vo.setMaterialName(material.getMaterialName());
        }
        return vo;
    }

    private String nextNo() {
        String prefix = NO_PREFIX + LocalDate.now().format(MONTH_FORMATTER) + "-";
        return prefix + nextSeq(baseMapper.selectLastBomNo(prefix));
    }

    private String nextSeq(String lastNo) {
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
        return String.format("%04d", next);
    }
}








