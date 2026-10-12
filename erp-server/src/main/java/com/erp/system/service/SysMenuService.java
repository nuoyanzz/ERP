package com.erp.system.service;

import com.erp.system.domain.dto.MenuSaveRequest;
import com.erp.system.domain.vo.MenuTreeVO;

import java.util.List;

public interface SysMenuService {

    List<MenuTreeVO> treeMenus();

    List<MenuTreeVO> myMenus();

    List<String> myPerms();

    MenuTreeVO getMenuDetail(Long id);

    Long createMenu(MenuSaveRequest request);

    void updateMenu(MenuSaveRequest request);

    void deleteMenu(Long id);
}








