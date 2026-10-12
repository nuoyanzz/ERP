package com.erp.system.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.erp.common.exception.BizException;
import com.erp.common.util.TreeUtils;
import com.erp.security.SecurityUtils;
import com.erp.system.domain.dto.MenuSaveRequest;
import com.erp.system.domain.entity.SysMenu;
import com.erp.system.domain.vo.MenuTreeVO;
import com.erp.system.mapper.SysMenuMapper;
import com.erp.system.service.SysMenuService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SysMenuServiceImpl extends ServiceImpl<SysMenuMapper, SysMenu> implements SysMenuService {

    @Override
    public List<MenuTreeVO> treeMenus() {
        List<SysMenu> menus = this.list(Wrappers.<SysMenu>lambdaQuery().orderByAsc(SysMenu::getSortNo));
        return buildTree(menus);
    }

    @Override
    public List<MenuTreeVO> myMenus() {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            throw new BizException("登录已失效，请重新登录");
        }
        return buildTree(baseMapper.selectMenusByUserId(userId));
    }

    @Override
    public List<String> myPerms() {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            throw new BizException("登录已失效，请重新登录");
        }
        return baseMapper.selectPermsByUserId(userId);
    }

    @Override
    public MenuTreeVO getMenuDetail(Long id) {
        SysMenu menu = this.getById(id);
        if (menu == null) {
            throw new BizException("菜单不存在");
        }
        MenuTreeVO vo = new MenuTreeVO();
        BeanUtils.copyProperties(menu, vo);
        return vo;
    }

    @Override
    public Long createMenu(MenuSaveRequest request) {
        SysMenu menu = new SysMenu();
        BeanUtils.copyProperties(request, menu);
        this.save(menu);
        return menu.getId();
    }

    @Override
    public void updateMenu(MenuSaveRequest request) {
        if (request.getId() == null) {
            throw new BizException("id不能为空");
        }
        SysMenu exist = this.getById(request.getId());
        if (exist == null) {
            throw new BizException("菜单不存在");
        }
        if (request.getId().equals(request.getParentId())) {
            throw new BizException("上级菜单不能是自身");
        }
        SysMenu menu = new SysMenu();
        BeanUtils.copyProperties(request, menu);
        this.updateById(menu);
    }

    @Override
    public void deleteMenu(Long id) {
        Long count = this.count(Wrappers.<SysMenu>lambdaQuery().eq(SysMenu::getParentId, id));
        if (count != null && count > 0) {
            throw new BizException("存在子菜单，无法删除");
        }
        this.removeById(id);
    }

    private List<MenuTreeVO> buildTree(List<SysMenu> menus) {
        List<MenuTreeVO> vos = new ArrayList<>(menus.size());
        for (SysMenu menu : menus) {
            MenuTreeVO vo = new MenuTreeVO();
            BeanUtils.copyProperties(menu, vo);
            vos.add(vo);
        }
        return TreeUtils.buildTree(vos, MenuTreeVO::getId, MenuTreeVO::getParentId, MenuTreeVO::setChildren);
    }
}








