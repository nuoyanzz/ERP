package com.erp.pm.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.erp.pm.domain.entity.PmWorkCenter;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface PmWorkCenterMapper extends BaseMapper<PmWorkCenter> {

    @Select("""
            SELECT center_code FROM pm_work_center
            WHERE center_code LIKE CONCAT(#{prefix}, '%') AND deleted = 0
            ORDER BY center_code DESC LIMIT 1
            """)
    String selectLastCenterCode(@Param("prefix") String prefix);
}








