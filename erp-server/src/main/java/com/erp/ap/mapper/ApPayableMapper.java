package com.erp.ap.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.erp.ap.domain.entity.ApPayable;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ApPayableMapper extends BaseMapper<ApPayable> {

    @Select("""
            SELECT payable_no FROM ap_payable
            WHERE payable_no LIKE CONCAT(#{prefix}, '%') AND deleted = 0
            ORDER BY payable_no DESC LIMIT 1
            """)
    String selectLastPayableNo(@Param("prefix") String prefix);
}








