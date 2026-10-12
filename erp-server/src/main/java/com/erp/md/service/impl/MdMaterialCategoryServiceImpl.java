package com.erp.md.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.erp.common.exception.BizException;
import com.erp.common.util.TreeUtils;
import com.erp.md.domain.dto.MaterialCategorySaveRequest;
import com.erp.md.domain.entity.MdMaterial;
import com.erp.md.domain.entity.MdMaterialCategory;
import com.erp.md.domain.vo.MaterialCategoryTreeVO;
import com.erp.md.mapper.MdMaterialCategoryMapper;
import com.erp.md.mapper.MdMaterialMapper;
import com.erp.md.service.MdMaterialCategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MdMaterialCategoryServiceImpl extends ServiceImpl<MdMaterialCategoryMapper, MdMaterialCategory> implements MdMaterialCategoryService {

    private final MdMaterialMapper materialMapper;

    @Override
    public List<MaterialCategoryTreeVO> treeCategories() {
        List<MdMaterialCategory> categories = this.list(Wrappers.<MdMaterialCategory>lambdaQuery()
                .orderByAsc(MdMaterialCategory::getSortNo)
                .orderByAsc(MdMaterialCategory::getId));
        List<MaterialCategoryTreeVO> vos = new ArrayList<>(categories.size());
        for (MdMaterialCategory category : categories) {
            MaterialCategoryTreeVO vo = new MaterialCategoryTreeVO();
            BeanUtils.copyProperties(category, vo);
            vos.add(vo);
        }
        return TreeUtils.buildTree(vos, MaterialCategoryTreeVO::getId, MaterialCategoryTreeVO::getParentId, MaterialCategoryTreeVO::setChildren);
    }

    @Override
    public MaterialCategoryTreeVO getCategoryDetail(Long id) {
        MdMaterialCategory category = this.getById(id);
        if (category == null) {
            throw new BizException("物料分类不存在");
        }
        MaterialCategoryTreeVO vo = new MaterialCategoryTreeVO();
        BeanUtils.copyProperties(category, vo);
        return vo;
    }

    @Override
    public Long createCategory(MaterialCategorySaveRequest request) {
        checkCodeUnique(request.getCategoryCode(), null);
        MdMaterialCategory category = new MdMaterialCategory();
        BeanUtils.copyProperties(request, category);
        if (category.getParentId() == null) {
            category.setParentId(0L);
        }
        this.save(category);
        return category.getId();
    }

    @Override
    public void updateCategory(MaterialCategorySaveRequest request) {
        if (request.getId() == null) {
            throw new BizException("id不能为空");
        }
        MdMaterialCategory exist = this.getById(request.getId());
        if (exist == null) {
            throw new BizException("物料分类不存在");
        }
        if (request.getId().equals(request.getParentId())) {
            throw new BizException("上级分类不能是自身");
        }
        if (!exist.getCategoryCode().equals(request.getCategoryCode())) {
            checkCodeUnique(request.getCategoryCode(), request.getId());
        }
        MdMaterialCategory category = new MdMaterialCategory();
        BeanUtils.copyProperties(request, category);
        this.updateById(category);
    }

    @Override
    public void deleteCategory(Long id) {
        Long childCount = this.count(Wrappers.<MdMaterialCategory>lambdaQuery().eq(MdMaterialCategory::getParentId, id));
        if (childCount != null && childCount > 0) {
            throw new BizException("存在子分类，无法删除");
        }
        Long materialCount = materialMapper.selectCount(Wrappers.<MdMaterial>lambdaQuery().eq(MdMaterial::getCategoryId, id));
        if (materialCount != null && materialCount > 0) {
            throw new BizException("该分类下存在物料，无法删除");
        }
        this.removeById(id);
    }

    private void checkCodeUnique(String categoryCode, Long excludeId) {
        Long count = this.count(Wrappers.<MdMaterialCategory>lambdaQuery()
                .eq(MdMaterialCategory::getCategoryCode, categoryCode)
                .ne(excludeId != null, MdMaterialCategory::getId, excludeId));
        if (count != null && count > 0) {
            throw new BizException("分类编码已存在");
        }
    }
}








