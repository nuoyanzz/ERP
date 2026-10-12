package com.erp.fi.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.erp.common.exception.BizException;
import com.erp.common.util.TreeUtils;
import com.erp.fi.domain.dto.FiAccountSaveRequest;
import com.erp.fi.domain.entity.FiAccount;
import com.erp.fi.domain.entity.FiVoucherEntry;
import com.erp.fi.domain.vo.FiAccountTreeVO;
import com.erp.fi.mapper.FiAccountMapper;
import com.erp.fi.mapper.FiVoucherEntryMapper;
import com.erp.fi.service.FiAccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FiAccountServiceImpl extends ServiceImpl<FiAccountMapper, FiAccount> implements FiAccountService {

    private final FiVoucherEntryMapper voucherEntryMapper;

    @Override
    public List<FiAccountTreeVO> treeAccounts() {
        List<FiAccount> accounts = this.list(Wrappers.<FiAccount>lambdaQuery()
                .orderByAsc(FiAccount::getSubjectCode));
        List<FiAccountTreeVO> vos = new ArrayList<>(accounts.size());
        for (FiAccount account : accounts) {
            FiAccountTreeVO vo = new FiAccountTreeVO();
            BeanUtils.copyProperties(account, vo);
            vos.add(vo);
        }
        return TreeUtils.buildTree(vos, FiAccountTreeVO::getId, FiAccountTreeVO::getParentId, FiAccountTreeVO::setChildren);
    }

    @Override
    public FiAccountTreeVO getAccountDetail(Long id) {
        FiAccount account = this.getById(id);
        if (account == null) {
            throw new BizException("会计科目不存在");
        }
        FiAccountTreeVO vo = new FiAccountTreeVO();
        BeanUtils.copyProperties(account, vo);
        return vo;
    }

    @Override
    public Long createAccount(FiAccountSaveRequest request) {
        checkCodeUnique(request.getSubjectCode(), null);
        checkParent(request.getParentId());
        FiAccount account = new FiAccount();
        BeanUtils.copyProperties(request, account);
        this.save(account);
        return account.getId();
    }

    @Override
    public void updateAccount(FiAccountSaveRequest request) {
        if (request.getId() == null) {
            throw new BizException("id不能为空");
        }
        FiAccount exist = this.getById(request.getId());
        if (exist == null) {
            throw new BizException("会计科目不存在");
        }
        if (!exist.getSubjectCode().equals(request.getSubjectCode())) {
            checkCodeUnique(request.getSubjectCode(), request.getId());
        }
        if (!request.getId().equals(request.getParentId())) {
            checkParent(request.getParentId());
        }
        FiAccount account = new FiAccount();
        BeanUtils.copyProperties(request, account);
        this.updateById(account);
    }

    @Override
    public void deleteAccount(Long id) {
        FiAccount exist = this.getById(id);
        if (exist == null) {
            throw new BizException("会计科目不存在");
        }
        Long childCount = this.count(Wrappers.<FiAccount>lambdaQuery().eq(FiAccount::getParentId, id));
        if (childCount != null && childCount > 0) {
            throw new BizException("存在下级科目，无法删除");
        }
        Long entryCount = voucherEntryMapper.selectCount(Wrappers.<FiVoucherEntry>lambdaQuery()
                .eq(FiVoucherEntry::getAccountId, id));
        if (entryCount != null && entryCount > 0) {
            throw new BizException("该科目已被凭证引用，无法删除");
        }
        this.removeById(id);
    }

    private void checkCodeUnique(String subjectCode, Long excludeId) {
        Long count = this.count(Wrappers.<FiAccount>lambdaQuery()
                .eq(FiAccount::getSubjectCode, subjectCode)
                .ne(excludeId != null, FiAccount::getId, excludeId));
        if (count != null && count > 0) {
            throw new BizException("科目编码已存在");
        }
    }

    private void checkParent(Long parentId) {
        if (parentId == null || parentId == 0L) {
            return;
        }
        FiAccount parent = this.getById(parentId);
        if (parent == null) {
            throw new BizException("上级科目不存在");
        }
        if (parent.getStatus() != null && parent.getStatus() != 0) {
            throw new BizException("上级科目已停用");
        }
    }
}








