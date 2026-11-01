package com.erp.md.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.erp.common.api.PageResult;
import com.erp.common.exception.BizException;
import com.erp.md.domain.dto.MaterialQueryRequest;
import com.erp.md.domain.dto.MaterialSaveRequest;
import com.erp.md.domain.entity.MdMaterial;
import com.erp.md.domain.vo.MaterialVO;
import com.erp.md.mapper.MdMaterialMapper;
import com.erp.md.service.MdMaterialService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MdMaterialServiceImpl extends ServiceImpl<MdMaterialMapper, MdMaterial> implements MdMaterialService {

    @Override
    public PageResult<MaterialVO> pageMaterial(MaterialQueryRequest request) {
        Page<MdMaterial> page = this.page(request.toPage(), Wrappers.<MdMaterial>lambdaQuery()
                .like(StringUtils.hasText(request.getMaterialCode()), MdMaterial::getMaterialCode, request.getMaterialCode())
                .like(StringUtils.hasText(request.getMaterialName()), MdMaterial::getMaterialName, request.getMaterialName())
                .eq(request.getCategoryId() != null, MdMaterial::getCategoryId, request.getCategoryId())
                .eq(request.getStatus() != null, MdMaterial::getStatus, request.getStatus())
                .orderByDesc(MdMaterial::getCreateTime));
        List<MaterialVO> records = page.getRecords().stream().map(this::toVO).collect(Collectors.toList());
        Page<MaterialVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        voPage.setRecords(records);
        return PageResult.of(voPage);
    }

    @Override
    public List<MaterialVO> listMdMaterial() {
        return this.list(Wrappers.<MdMaterial>lambdaQuery()
                .eq(MdMaterial::getStatus, 0)
                .orderByAsc(MdMaterial::getId))
                .stream().map(this::toVO).collect(Collectors.toList());
    }

    @Override
    public MaterialVO getMaterialDetail(Long id) {
        MdMaterial material = this.getById(id);
        if (material == null) {
            throw new BizException("物料不存在");
        }
        return toVO(material);
    }

    @Override
    public Long createMaterial(MaterialSaveRequest request) {
        checkCodeUnique(request.getMaterialCode(), null);
        MdMaterial material = new MdMaterial();
        BeanUtils.copyProperties(request, material);
        this.save(material);
        return material.getId();
    }

    @Override
    public void updateMaterial(MaterialSaveRequest request) {
        if (request.getId() == null) {
            throw new BizException("id不能为空");
        }
        MdMaterial exist = this.getById(request.getId());
        if (exist == null) {
            throw new BizException("物料不存在");
        }
        if (!exist.getMaterialCode().equals(request.getMaterialCode())) {
            checkCodeUnique(request.getMaterialCode(), request.getId());
        }
        MdMaterial material = new MdMaterial();
        BeanUtils.copyProperties(request, material);
        this.updateById(material);
    }

    @Override
    public void deleteMaterial(Long id) {
        MdMaterial exist = this.getById(id);
        if (exist == null) {
            throw new BizException("物料不存在");
        }
        this.removeById(id);
    }

    private void checkCodeUnique(String materialCode, Long excludeId) {
        Long count = this.count(Wrappers.<MdMaterial>lambdaQuery()
                .eq(MdMaterial::getMaterialCode, materialCode)
                .ne(excludeId != null, MdMaterial::getId, excludeId));
        if (count != null && count > 0) {
            throw new BizException("物料编码已存在");
        }
    }

    private MaterialVO toVO(MdMaterial material) {
        MaterialVO vo = new MaterialVO();
        BeanUtils.copyProperties(material, vo);
        return vo;
    }
}








