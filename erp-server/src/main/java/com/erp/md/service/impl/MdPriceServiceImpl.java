package com.erp.md.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.erp.common.api.PageResult;
import com.erp.common.exception.BizException;
import com.erp.md.domain.dto.MdPriceQueryRequest;
import com.erp.md.domain.dto.MdPriceSaveRequest;
import com.erp.md.domain.entity.MdCustomer;
import com.erp.md.domain.entity.MdMaterial;
import com.erp.md.domain.entity.MdPrice;
import com.erp.md.domain.entity.MdSupplier;
import com.erp.md.domain.vo.MdPriceVO;
import com.erp.md.mapper.MdCustomerMapper;
import com.erp.md.mapper.MdMaterialMapper;
import com.erp.md.mapper.MdPriceMapper;
import com.erp.md.mapper.MdSupplierMapper;
import com.erp.md.service.MdPriceService;
import com.erp.md.service.MdPriceValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MdPriceServiceImpl extends ServiceImpl<MdPriceMapper, MdPrice> implements MdPriceService {

    private final MdCustomerMapper customerMapper;
    private final MdSupplierMapper supplierMapper;
    private final MdMaterialMapper materialMapper;

    @Override
    public PageResult<MdPriceVO> pagePrice(MdPriceQueryRequest request) {
        Page<MdPrice> page = this.page(request.toPage(), Wrappers.<MdPrice>lambdaQuery()
                .like(StringUtils.hasText(request.getPriceNo()), MdPrice::getPriceNo, request.getPriceNo())
                .like(StringUtils.hasText(request.getPriceName()), MdPrice::getPriceName, request.getPriceName())
                .eq(request.getCustomerId() != null, MdPrice::getCustomerId, request.getCustomerId())
                .eq(request.getSupplierId() != null, MdPrice::getSupplierId, request.getSupplierId())
                .eq(request.getMaterialId() != null, MdPrice::getMaterialId, request.getMaterialId())
                .eq(StringUtils.hasText(request.getCurrency()), MdPrice::getCurrency, request.getCurrency())
                .ge(request.getEffectiveDate() != null, MdPrice::getEffectiveDate, request.getEffectiveDate())
                .le(request.getExpiryDate() != null, MdPrice::getExpiryDate, request.getExpiryDate())
                .eq(request.getStatus() != null, MdPrice::getStatus, request.getStatus())
                .orderByDesc(MdPrice::getCreateTime));

        List<MdPriceVO> records = toVOList(page.getRecords());
        Page<MdPriceVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        voPage.setRecords(records);
        return PageResult.of(voPage);
    }

    @Override
    public MdPriceVO getPriceDetail(Long id) {
        MdPrice price = this.getById(id);
        if (price == null) {
            throw new BizException("价格记录不存在");
        }
        return toVOList(List.of(price)).get(0);
    }

    @Override
    public Long createPrice(MdPriceSaveRequest request) {
        MdPriceValidator.validate(request);
        checkPriceNoUnique(request.getPriceNo(), null);
        MdPrice price = new MdPrice();
        BeanUtils.copyProperties(request, price);
        fillDefaults(price);
        this.save(price);
        return price.getId();
    }

    @Override
    public void updatePrice(MdPriceSaveRequest request) {
        if (request.getId() == null) {
            throw new BizException("id不能为空");
        }
        MdPrice exist = this.getById(request.getId());
        if (exist == null) {
            throw new BizException("价格记录不存在");
        }
        MdPriceValidator.validate(request);
        if (!Objects.equals(exist.getPriceNo(), request.getPriceNo())) {
            checkPriceNoUnique(request.getPriceNo(), request.getId());
        }
        MdPrice price = new MdPrice();
        BeanUtils.copyProperties(request, price);
        fillDefaults(price);
        this.updateById(price);
    }

    @Override
    public void deletePrice(Long id) {
        MdPrice exist = this.getById(id);
        if (exist == null) {
            throw new BizException("价格记录不存在");
        }
        this.removeById(id);
    }

    private void checkPriceNoUnique(String priceNo, Long excludeId) {
        Long count = this.count(Wrappers.<MdPrice>lambdaQuery()
                .eq(MdPrice::getPriceNo, priceNo)
                .ne(excludeId != null, MdPrice::getId, excludeId));
        if (count != null && count > 0) {
            throw new BizException("价格单号已存在");
        }
    }

    /**
     * 补齐数据库侧的默认值，避免前端不传时写出 null 覆盖列默认值。
     */
    private void fillDefaults(MdPrice price) {
        if (price.getTaxRate() == null) {
            price.setTaxRate(BigDecimal.ZERO);
        }
        if (price.getStatus() == null) {
            price.setStatus(0);
        }
    }

    /**
     * 批量转换 VO，并把客户名、供应商名、物料编码与名称一次性带出。
     * <p>
     * 关联主数据统一批量查询后回填，避免逐行查询造成 N+1。
     */
    private List<MdPriceVO> toVOList(List<MdPrice> prices) {
        if (prices.isEmpty()) {
            return List.of();
        }
        List<Long> customerIds = prices.stream().map(MdPrice::getCustomerId)
                .filter(Objects::nonNull).distinct().collect(Collectors.toList());
        List<Long> supplierIds = prices.stream().map(MdPrice::getSupplierId)
                .filter(Objects::nonNull).distinct().collect(Collectors.toList());
        List<Long> materialIds = prices.stream().map(MdPrice::getMaterialId)
                .filter(Objects::nonNull).distinct().collect(Collectors.toList());

        Map<Long, MdCustomer> customerMap = loadCustomers(customerIds);
        Map<Long, MdSupplier> supplierMap = loadSuppliers(supplierIds);
        Map<Long, MdMaterial> materialMap = loadMaterials(materialIds);

        return prices.stream().map(price -> {
            MdPriceVO vo = new MdPriceVO();
            BeanUtils.copyProperties(price, vo);
            MdCustomer customer = customerMap.get(price.getCustomerId());
            if (customer != null) {
                vo.setCustomerName(customer.getCustomerName());
            }
            MdSupplier supplier = supplierMap.get(price.getSupplierId());
            if (supplier != null) {
                vo.setSupplierName(supplier.getSupplierName());
            }
            MdMaterial material = materialMap.get(price.getMaterialId());
            if (material != null) {
                vo.setMaterialCode(material.getMaterialCode());
                vo.setMaterialName(material.getMaterialName());
            }
            return vo;
        }).collect(Collectors.toList());
    }

    private Map<Long, MdCustomer> loadCustomers(List<Long> ids) {
        if (ids.isEmpty()) {
            return Map.of();
        }
        return customerMapper.selectBatchIds(ids).stream()
                .collect(Collectors.toMap(MdCustomer::getId, Function.identity(), (a, b) -> a));
    }

    private Map<Long, MdSupplier> loadSuppliers(List<Long> ids) {
        if (ids.isEmpty()) {
            return Map.of();
        }
        return supplierMapper.selectBatchIds(ids).stream()
                .collect(Collectors.toMap(MdSupplier::getId, Function.identity(), (a, b) -> a));
    }

    private Map<Long, MdMaterial> loadMaterials(List<Long> ids) {
        if (ids.isEmpty()) {
            return Map.of();
        }
        return materialMapper.selectBatchIds(ids).stream()
                .collect(Collectors.toMap(MdMaterial::getId, Function.identity(), (a, b) -> a));
    }
}
