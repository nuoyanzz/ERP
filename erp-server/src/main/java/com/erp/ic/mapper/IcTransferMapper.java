package com.erp.ic.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.erp.ic.domain.entity.IcTransfer;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface IcTransferMapper extends BaseMapper<IcTransfer> {

    @Select("""
            SELECT transfer_no FROM ic_transfer
            WHERE transfer_no LIKE CONCAT(#{prefix}, '%') AND deleted = 0
            ORDER BY transfer_no DESC LIMIT 1
            """)
    String selectLastTransferNo(@Param("prefix") String prefix);
}








