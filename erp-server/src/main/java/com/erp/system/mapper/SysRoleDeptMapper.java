package com.erp.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.erp.system.domain.entity.SysRoleDept;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 角色与部门的关联（自定义数据范围）。
 */
@Mapper
public interface SysRoleDeptMapper extends BaseMapper<SysRoleDept> {

    @Select("""
            <script>
            SELECT DISTINCT dept_id FROM sys_role_dept
            WHERE role_id IN
            <foreach collection="roleIds" item="roleId" open="(" separator="," close=")">
                #{roleId}
            </foreach>
            </script>
            """)
    List<Long> selectDeptIdsByRoleIds(@Param("roleIds") List<Long> roleIds);
}








