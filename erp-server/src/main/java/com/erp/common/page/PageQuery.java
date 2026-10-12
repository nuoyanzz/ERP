package com.erp.common.page;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.Data;

import java.io.Serializable;

@Data
public class PageQuery implements Serializable {

    private long pageNum = 1;
    private long pageSize = 10;

    public <T> Page<T> toPage() {
        return new Page<>(pageNum, pageSize);
    }
}








