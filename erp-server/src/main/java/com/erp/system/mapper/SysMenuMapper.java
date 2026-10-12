package com.erp.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.erp.system.domain.entity.SysMenu;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SysMenuMapper extends BaseMapper<SysMenu> {

    /**
     * 查询用户拥有的权限码。
     * <p>
     * 菜单（menu_type=2）与按钮（menu_type=3）都可能声明权限码，
     * 例如「我的待办」菜单本身就带 {@code wf:task:todo}，
     * 只取按钮会漏掉这类菜单级权限。
     */
    @Select("""
            SELECT DISTINCT m.perms FROM sys_menu m
            INNER JOIN sys_role_menu rm ON rm.menu_id = m.id
            INNER JOIN sys_user_role ur ON ur.role_id = rm.role_id
            WHERE ur.user_id = #{userId} AND m.deleted = 0 AND m.menu_type IN (2, 3)
              AND m.perms IS NOT NULL AND m.perms <> ''
            """)
    List<String> selectPermsByUserId(Long userId);

    @Select("""
            SELECT DISTINCT m.* FROM sys_menu m
            INNER JOIN sys_role_menu rm ON rm.menu_id = m.id
            INNER JOIN sys_user_role ur ON ur.role_id = rm.role_id
            WHERE ur.user_id = #{userId} AND m.deleted = 0 AND m.menu_type IN (1, 2)
            ORDER BY m.sort_no
            """)
    List<SysMenu> selectMenusByUserId(Long userId);

    @Select("""
            SELECT rm.menu_id FROM sys_role_menu rm
            INNER JOIN sys_menu m ON m.id = rm.menu_id
            WHERE rm.role_id = #{roleId} AND m.deleted = 0
            """)
    List<Long> selectMenuIdsByRoleId(Long roleId);
}








