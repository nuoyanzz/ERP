package com.erp.hr.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.erp.hr.domain.entity.HrSalary;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface HrSalaryMapper extends BaseMapper<HrSalary> {

    @Select("""
            SELECT salary_no FROM hr_salary
            WHERE salary_no LIKE CONCAT(#{prefix}, '%') AND deleted = 0
            ORDER BY salary_no DESC LIMIT 1
            """)
    String selectLastSalaryNo(@Param("prefix") String prefix);
}








