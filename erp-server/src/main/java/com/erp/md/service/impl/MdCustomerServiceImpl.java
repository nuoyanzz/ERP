package com.erp.md.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.erp.common.api.PageResult;
import com.erp.common.exception.BizException;
import com.erp.md.domain.dto.CustomerQueryRequest;
import com.erp.md.domain.dto.CustomerSaveRequest;
import com.erp.md.domain.entity.MdCustomer;
import com.erp.md.domain.vo.CustomerVO;
import com.erp.md.mapper.MdCustomerMapper;
import com.erp.md.service.MdCustomerService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MdCustomerServiceImpl extends ServiceImpl<MdCustomerMapper, MdCustomer> implements MdCustomerService {

    @Override
    public PageResult<CustomerVO> pageCustomer(CustomerQueryRequest request) {
        Page<MdCustomer> page = this.page(request.toPage(), Wrappers.<MdCustomer>lambdaQuery()
                .like(StringUtils.hasText(request.getCustomerCode()), MdCustomer::getCustomerCode, request.getCustomerCode())
                .like(StringUtils.hasText(request.getCustomerName()), MdCustomer::getCustomerName, request.getCustomerName())
                .eq(request.getStatus() != null, MdCustomer::getStatus, request.getStatus())
                .orderByDesc(MdCustomer::getCreateTime));
        List<CustomerVO> records = page.getRecords().stream().map(this::toVO).collect(Collectors.toList());
        Page<CustomerVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        voPage.setRecords(records);
        return PageResult.of(voPage);
    }

    @Override
    public List<CustomerVO> listMdCustomer() {
        return this.list(Wrappers.<MdCustomer>lambdaQuery()
                .eq(MdCustomer::getStatus, 0)
                .orderByAsc(MdCustomer::getId))
                .stream().map(this::toVO).collect(Collectors.toList());
    }

    @Override
    public CustomerVO getCustomerDetail(Long id) {
        MdCustomer customer = this.getById(id);
        if (customer == null) {
            throw new BizException("客户不存在");
        }
        return toVO(customer);
    }

    @Override
    public Long createCustomer(CustomerSaveRequest request) {
        checkCodeUnique(request.getCustomerCode(), null);
        MdCustomer customer = new MdCustomer();
        BeanUtils.copyProperties(request, customer);
        this.save(customer);
        return customer.getId();
    }

    @Override
    public void updateCustomer(CustomerSaveRequest request) {
        if (request.getId() == null) {
            throw new BizException("id不能为空");
        }
        MdCustomer exist = this.getById(request.getId());
        if (exist == null) {
            throw new BizException("客户不存在");
        }
        if (!exist.getCustomerCode().equals(request.getCustomerCode())) {
            checkCodeUnique(request.getCustomerCode(), request.getId());
        }
        MdCustomer customer = new MdCustomer();
        BeanUtils.copyProperties(request, customer);
        this.updateById(customer);
    }

    @Override
    public void deleteCustomer(Long id) {
        MdCustomer exist = this.getById(id);
        if (exist == null) {
            throw new BizException("客户不存在");
        }
        this.removeById(id);
    }

    private void checkCodeUnique(String customerCode, Long excludeId) {
        Long count = this.count(Wrappers.<MdCustomer>lambdaQuery()
                .eq(MdCustomer::getCustomerCode, customerCode)
                .ne(excludeId != null, MdCustomer::getId, excludeId));
        if (count != null && count > 0) {
            throw new BizException("客户编码已存在");
        }
    }

    private CustomerVO toVO(MdCustomer customer) {
        CustomerVO vo = new CustomerVO();
        BeanUtils.copyProperties(customer, vo);
        return vo;
    }
}








