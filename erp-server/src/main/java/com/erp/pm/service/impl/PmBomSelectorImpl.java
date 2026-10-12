package com.erp.pm.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.erp.pm.domain.entity.PmBom;
import com.erp.pm.mapper.PmBomMapper;
import com.erp.pm.service.PmBomSelector;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class PmBomSelectorImpl implements PmBomSelector {

    private final PmBomMapper bomMapper;

    @Override
    public PmBom select(Long materialId, LocalDate demandDate) {
        List<PmBom> candidates = bomMapper.selectList(Wrappers.<PmBom>lambdaQuery()
                .eq(PmBom::getMaterialId, materialId)
                .eq(PmBom::getStatus, PmBom.STATUS_ENABLED));
        return pick(candidates, demandDate);
    }

    @Override
    public Map<Long, PmBom> selectAll(List<Long> materialIds, LocalDate demandDate) {
        if (materialIds == null || materialIds.isEmpty()) {
            return new HashMap<>();
        }
        List<PmBom> all = bomMapper.selectList(Wrappers.<PmBom>lambdaQuery()
                .in(PmBom::getMaterialId, materialIds)
                .eq(PmBom::getStatus, PmBom.STATUS_ENABLED));
        // 按物料分组后再逐个挑选，避免同一个物料的多版本互相覆盖
        Map<Long, List<PmBom>> byMaterial = all.stream()
                .collect(Collectors.groupingBy(PmBom::getMaterialId));
        Map<Long, PmBom> result = new HashMap<>();
        for (Map.Entry<Long, List<PmBom>> entry : byMaterial.entrySet()) {
            PmBom bom = pick(entry.getValue(), demandDate);
            if (bom != null) {
                result.put(entry.getKey(), bom);
            }
        }
        return result;
    }

    /**
     * 从候选 BOM 中挑出唯一可用版本。
     * <p>
     * 优先取「默认版本且在有效期内」；若默认版本不在有效期内，
     * 则取有效期内版本号最大的一个，保证 MRP 在任何需求日都能找到唯一 BOM。
     */
    private PmBom pick(List<PmBom> candidates, LocalDate demandDate) {
        if (candidates == null || candidates.isEmpty()) {
            return null;
        }
        LocalDate date = demandDate == null ? LocalDate.now() : demandDate;
        List<PmBom> effective = candidates.stream()
                .filter(bom -> isEffective(bom, date))
                .collect(Collectors.toList());
        if (effective.isEmpty()) {
            return null;
        }
        PmBom defaultBom = effective.stream()
                .filter(bom -> bom.getIsDefault() != null && bom.getIsDefault() == 1)
                .findFirst()
                .orElse(null);
        if (defaultBom != null) {
            return defaultBom;
        }
        // 没有可用默认版本时，取版本号最大的一个，保证结果唯一
        return effective.stream()
                .max(Comparator.comparing(bom -> bom.getVersion() == null ? "" : bom.getVersion()))
                .orElse(null);
    }

    /**
     * 生效日期 <= 需求日 且（失效日期为空 或 失效日期 >= 需求日）。
     */
    private boolean isEffective(PmBom bom, LocalDate demandDate) {
        if (bom.getEffectiveDate() != null && bom.getEffectiveDate().isAfter(demandDate)) {
            return false;
        }
        return bom.getExpiryDate() == null || !bom.getExpiryDate().isBefore(demandDate);
    }
}








