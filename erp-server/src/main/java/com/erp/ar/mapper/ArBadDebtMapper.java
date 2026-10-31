package com.erp.ar.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.erp.ar.domain.entity.ArBadDebt;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ArBadDebtMapper extends BaseMapper<ArBadDebt> {

    /**
     * 取指定前缀下的最后一个单据号，用于生成下一个序号。
     */
}













