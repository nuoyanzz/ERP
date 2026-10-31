package com.erp.md.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.erp.common.api.PageResult;
import com.erp.common.exception.BizException;
import com.erp.md.domain.dto.MdUnitConvertQueryRequest;
import com.erp.md.domain.dto.MdUnitConvertSaveRequest;
import com.erp.md.domain.entity.MdMaterial;
import com.erp.md.domain.entity.MdUnitConvert;
import com.erp.md.domain.vo.MdUnitConvertVO;
import com.erp.md.mapper.MdMaterialMapper;
import com.erp.md.mapper.MdUnitConvertMapper;
import com.erp.md.service.MdUnitConvertService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MdUnitConvertServiceImpl extends ServiceImpl<MdUnitConvertMapper, MdUnitConvert>
        implements MdUnitConvertService {

    /** 未配置换算关系时按 1:1 换算 */
    public static final BigDecimal DEFAULT_CONVERT_RATE = BigDecimal.ONE;

    private final MdMaterialMapper materialMapper;

    @Override
    public PageResult<MdUnitConvertVO> pageConvert(MdUnitConvertQueryRequest request) {
        Page<MdUnitConvert> page = this.page(request.toPage(), Wrappers.<MdUnitConvert>lambdaQuery()
                .eq(request.getMaterialId() != null, MdUnitConvert::getMaterialId, request.getMaterialId())
                .eq(StringUtils.hasText(request.getFromUnit()), MdUnitConvert::getFromUnit, request.getFromUnit())
                .eq(StringUtils.hasText(request.getToUnit()), MdUnitConvert::getToUnit, request.getToUnit())
                .orderByDesc(MdUnitConvert::getCreateTime));
        List<MdUnitConvertVO> records = toVOList(page.getRecords());
        Page<MdUnitConvertVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        voPage.setRecords(records);
        return PageResult.of(voPage);
    }

    @Override
    public MdUnitConvertVO getConvertDetail(Long id) {
        MdUnitConvert convert = this.getById(id);
        if (convert == null) {
            throw new BizException("单位换算不存在");
        }
        return toVOList(List.of(convert)).get(0);
    }

    @Override
    public Long createConvert(MdUnitConvertSaveRequest request) {
        validate(request);
        checkDuplicate(request.getMaterialId(), request.getFromUnit(), request.getToUnit(), null);
        MdUnitConvert convert = new MdUnitConvert();
        BeanUtils.copyProperties(request, convert);
        this.save(convert);
        return convert.getId();
    }

    @Override
    public void updateConvert(MdUnitConvertSaveRequest request) {
        if (request.getId() == null) {
            throw new BizException("id不能为空");
        }
        MdUnitConvert exist = this.getById(request.getId());
        if (exist == null) {
            throw new BizException("单位换算不存在");
        }
        validate(request);
        // 换算来源单位或目标单位变化时，仍需校验是否与其它记录冲突
        boolean pairChanged = !Objects.equals(exist.getFromUnit(), request.getFromUnit())
                || !Objects.equals(exist.getToUnit(), request.getToUnit());
        if (pairChanged) {
            checkDuplicate(request.getMaterialId(), request.getFromUnit(), request.getToUnit(), request.getId());
        }
        MdUnitConvert convert = new MdUnitConvert();
        BeanUtils.copyProperties(request, convert);
        this.updateById(convert);
    }

    @Override
    public void deleteConvert(Long id) {
        MdUnitConvert exist = this.getById(id);
        if (exist == null) {
            throw new BizException("单位换算不存在");
        }
        this.removeById(id);
    }

    @Override
    public BigDecimal getConvertRate(Long materialId, String unit) {
        if (materialId == null || !StringUtils.hasText(unit)) {
            return DEFAULT_CONVERT_RATE;
        }
        MdUnitConvert convert = this.getOne(Wrappers.<MdUnitConvert>lambdaQuery()
                .eq(MdUnitConvert::getMaterialId, materialId)
                .eq(MdUnitConvert::getFromUnit, unit)
                .last("limit 1"), false);
        if (convert == null || convert.getConvertRate() == null) {
            return DEFAULT_CONVERT_RATE;
        }
        return convert.getConvertRate();
    }

    /**
     * 换算率必须大于 0，来源单位与目标单位不能相同。
     */
    private void validate(MdUnitConvertSaveRequest request) {
        BigDecimal rate = request.getConvertRate();
        if (rate == null) {
            throw new BizException("换算率不能为空");
        }
        if (rate.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BizException("换算率必须大于0");
        }
        if (Objects.equals(request.getFromUnit(), request.getToUnit())) {
            throw new BizException("来源单位与目标单位不能相同");
        }
    }

    /**
     * 同一物料下，同一来源单位只允许存在一条换算关系；
     * 若已存在相同「来源单位 + 目标单位」的组合，同样视为重复。
     */
    private void checkDuplicate(Long materialId, String fromUnit, String toUnit, Long excludeId) {
        // 先判完全相同的组合，给出更精确的提示
        Long samePairCount = this.count(Wrappers.<MdUnitConvert>lambdaQuery()
                .eq(MdUnitConvert::getMaterialId, materialId)
                .eq(MdUnitConvert::getFromUnit, fromUnit)
                .eq(MdUnitConvert::getToUnit, toUnit)
                .ne(excludeId != null, MdUnitConvert::getId, excludeId));
        if (samePairCount != null && samePairCount > 0) {
            throw new BizException("该物料的单位换算关系已存在");
        }
        // 再判来源单位是否已被占用，换成其它目标单位同样不允许
        Long sameFromCount = this.count(Wrappers.<MdUnitConvert>lambdaQuery()
                .eq(MdUnitConvert::getMaterialId, materialId)
                .eq(MdUnitConvert::getFromUnit, fromUnit)
                .ne(excludeId != null, MdUnitConvert::getId, excludeId));
        if (sameFromCount != null && sameFromCount > 0) {
            throw new BizException("该物料的来源单位已存在换算关系");
        }
    }

    private List<MdUnitConvertVO> toVOList(List<MdUnitConvert> converts) {
        if (converts.isEmpty()) {
            return List.of();
        }
        List<Long> materialIds = converts.stream().map(MdUnitConvert::getMaterialId)
                .filter(Objects::nonNull).distinct().collect(Collectors.toList());
        final Map<Long, MdMaterial> materialMap = materialIds.isEmpty()
                ? Map.of()
                : materialMapper.selectBatchIds(materialIds).stream()
                        .collect(Collectors.toMap(MdMaterial::getId, Function.identity(), (a, b) -> a));
        return converts.stream().map(convert -> {
            MdUnitConvertVO vo = new MdUnitConvertVO();
            BeanUtils.copyProperties(convert, vo);
            MdMaterial material = materialMap.get(convert.getMaterialId());
            if (material != null) {
                vo.setMaterialCode(material.getMaterialCode());
                vo.setMaterialName(material.getMaterialName());
            }
            return vo;
        }).collect(Collectors.toList());
    }
}
