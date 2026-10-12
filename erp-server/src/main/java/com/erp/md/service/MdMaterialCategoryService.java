package com.erp.md.service;

import com.erp.md.domain.dto.MaterialCategorySaveRequest;
import com.erp.md.domain.vo.MaterialCategoryTreeVO;

import java.util.List;

public interface MdMaterialCategoryService {

    List<MaterialCategoryTreeVO> treeCategories();

    MaterialCategoryTreeVO getCategoryDetail(Long id);

    Long createCategory(MaterialCategorySaveRequest request);

    void updateCategory(MaterialCategorySaveRequest request);

    void deleteCategory(Long id);
}








