package com.erp.hr.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.erp.hr.domain.entity.HrEmployee;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface HrEmployeeMapper extends BaseMapper<HrEmployee> {

    @Select("""
            SELECT employee_no FROM hr_employee
            WHERE employee_no LIKE CONCAT(#{prefix}, '%') AND deleted = 0
            ORDER BY employee_no DESC LIMIT 1
            """)
    String selectLastEmployeeNo(@Param("prefix") String prefix);
}








