package com.erp.pm.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.erp.pm.domain.entity.PmRouting;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface PmRoutingMapper extends BaseMapper<PmRouting> {

    @Select("""
            SELECT routing_no FROM pm_routing
            WHERE routing_no LIKE CONCAT(#{prefix}, '%') AND deleted = 0
            ORDER BY routing_no DESC LIMIT 1
            """)
    String selectLastRoutingNo(@Param("prefix") String prefix);
}








