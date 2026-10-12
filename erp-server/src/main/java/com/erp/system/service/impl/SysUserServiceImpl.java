package com.erp.system.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.erp.common.api.PageResult;
import com.erp.common.exception.BizException;
import com.erp.security.SecurityUtils;
import com.erp.security.service.PasswordPolicy;
import com.erp.system.domain.dto.AssignRoleRequest;
import com.erp.system.domain.dto.PasswordUpdateRequest;
import com.erp.system.domain.dto.UserQueryRequest;
import com.erp.system.domain.dto.UserSaveRequest;
import com.erp.system.domain.entity.SysRole;
import com.erp.system.domain.entity.SysUser;
import com.erp.system.domain.entity.SysUserRole;
import com.erp.system.domain.vo.UserVO;
import com.erp.system.mapper.SysRoleMapper;
import com.erp.system.mapper.SysUserMapper;
import com.erp.system.mapper.SysUserRoleMapper;
import com.erp.system.service.SysUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SysUserServiceImpl extends ServiceImpl<SysUserMapper, SysUser> implements SysUserService {

    private static final int STATUS_DISABLED = 1;
    private static final String DEFAULT_PASSWORD = "123456";

    private final SysUserRoleMapper userRoleMapper;
    private final SysRoleMapper roleMapper;
    private final PasswordEncoder passwordEncoder;
    private final PasswordPolicy passwordPolicy;

    @Override
    public PageResult<UserVO> pageUser(UserQueryRequest request) {
        IPage<SysUser> page = this.page(request.toPage(), Wrappers.<SysUser>lambdaQuery()
                .like(StringUtils.hasText(request.getUsername()), SysUser::getUsername, request.getUsername())
                .like(StringUtils.hasText(request.getNickname()), SysUser::getNickname, request.getNickname())
                .eq(request.getStatus() != null, SysUser::getStatus, request.getStatus())
                .orderByDesc(SysUser::getCreateTime));
        Map<Long, List<Long>> roleMap = selectRoleMap(page.getRecords());
        List<UserVO> records = page.getRecords().stream()
                .map(user -> toVO(user, roleMap.getOrDefault(user.getId(), Collections.emptyList())))
                .collect(Collectors.toList());
        Page<UserVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        voPage.setRecords(records);
        return PageResult.of(voPage);
    }

    @Override
    public UserVO getUserDetail(Long id) {
        SysUser user = this.getById(id);
        if (user == null) {
            throw new BizException("用户不存在");
        }
        return toVO(user, selectRoleIds(id));
    }

    @Override
    public Long createUser(UserSaveRequest request) {
        Long count = this.count(Wrappers.<SysUser>lambdaQuery().eq(SysUser::getUsername, request.getUsername()));
        if (count != null && count > 0) {
            throw new BizException("用户名已存在");
        }
        SysUser user = new SysUser();
        BeanUtils.copyProperties(request, user);
        String rawPassword = StringUtils.hasText(request.getPassword()) ? request.getPassword() : DEFAULT_PASSWORD;
        if (StringUtils.hasText(request.getPassword())) {
            passwordPolicy.validate(rawPassword);
        }
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setPwdUpdateTime(LocalDateTime.now());
        this.save(user);
        return user.getId();
    }

    @Override
    public void updateUser(UserSaveRequest request) {
        if (request.getId() == null) {
            throw new BizException("id不能为空");
        }
        SysUser exist = this.getById(request.getId());
        if (exist == null) {
            throw new BizException("用户不存在");
        }
        if (!exist.getUsername().equals(request.getUsername())) {
            Long count = this.count(Wrappers.<SysUser>lambdaQuery()
                    .eq(SysUser::getUsername, request.getUsername())
                    .ne(SysUser::getId, request.getId()));
            if (count != null && count > 0) {
                throw new BizException("用户名已存在");
            }
        }
        if (Objects.equals(STATUS_DISABLED, request.getStatus())
                && Objects.equals(SecurityUtils.getCurrentUserId(), request.getId())) {
            throw new BizException("不能停用当前登录用户");
        }
        SysUser user = new SysUser();
        BeanUtils.copyProperties(request, user);
        user.setPassword(null);
        this.updateById(user);
    }

    @Override
    public void deleteUsers(List<Long> ids) {
        if (CollectionUtils.isEmpty(ids)) {
            throw new BizException("请选择要删除的用户");
        }
        Long currentUserId = SecurityUtils.getCurrentUserId();
        if (currentUserId != null && ids.contains(currentUserId)) {
            throw new BizException("不能删除当前登录用户");
        }
        this.removeByIds(ids);
        userRoleMapper.delete(Wrappers.<SysUserRole>lambdaQuery().in(SysUserRole::getUserId, ids));
    }

    @Override
    public void updatePassword(Long id, PasswordUpdateRequest request) {
        SysUser exist = this.getById(id);
        if (exist == null) {
            throw new BizException("用户不存在");
        }
        if (!passwordEncoder.matches(request.getOldPassword(), exist.getPassword())) {
            throw new BizException("原密码错误");
        }
        passwordPolicy.validate(request.getNewPassword());
        this.update(Wrappers.<SysUser>lambdaUpdate()
                .set(SysUser::getPassword, passwordEncoder.encode(request.getNewPassword()))
                .set(SysUser::getPwdUpdateTime, LocalDateTime.now())
                .set(SysUser::getUpdateTime, LocalDateTime.now())
                .set(SysUser::getUpdateBy, SecurityUtils.getCurrentUserId())
                .eq(SysUser::getId, id));
    }

    @Override
    public void resetPassword(Long id) {
        SysUser exist = this.getById(id);
        if (exist == null) {
            throw new BizException("用户不存在");
        }
        this.update(Wrappers.<SysUser>lambdaUpdate()
                .set(SysUser::getPassword, passwordEncoder.encode(DEFAULT_PASSWORD))
                .set(SysUser::getUpdateTime, LocalDateTime.now())
                .set(SysUser::getUpdateBy, SecurityUtils.getCurrentUserId())
                .eq(SysUser::getId, id));
    }

    @Override
    public void assignRoles(Long id, AssignRoleRequest request) {
        SysUser exist = this.getById(id);
        if (exist == null) {
            throw new BizException("用户不存在");
        }
        List<Long> roleIds = request.getRoleIds() == null ? Collections.emptyList() : request.getRoleIds();
        if (!roleIds.isEmpty()) {
            Long count = roleMapper.selectCount(Wrappers.<SysRole>lambdaQuery().in(SysRole::getId, roleIds));
            if (count == null || count.intValue() != roleIds.size()) {
                throw new BizException("存在无效的角色");
            }
        }
        userRoleMapper.delete(Wrappers.<SysUserRole>lambdaQuery().eq(SysUserRole::getUserId, id));
        for (Long roleId : roleIds) {
            SysUserRole userRole = new SysUserRole();
            userRole.setUserId(id);
            userRole.setRoleId(roleId);
            userRoleMapper.insert(userRole);
        }
    }

    private UserVO toVO(SysUser user, List<Long> roleIds) {
        UserVO vo = new UserVO();
        BeanUtils.copyProperties(user, vo);
        vo.setRoleIds(roleIds);
        return vo;
    }

    private Map<Long, List<Long>> selectRoleMap(List<SysUser> users) {
        if (CollectionUtils.isEmpty(users)) {
            return Collections.emptyMap();
        }
        List<Long> userIds = users.stream().map(SysUser::getId).collect(Collectors.toList());
        List<SysUserRole> list = userRoleMapper.selectList(Wrappers.<SysUserRole>lambdaQuery()
                .in(SysUserRole::getUserId, userIds));
        return list.stream().collect(Collectors.groupingBy(SysUserRole::getUserId,
                Collectors.mapping(SysUserRole::getRoleId, Collectors.toList())));
    }

    private List<Long> selectRoleIds(Long userId) {
        List<SysUserRole> list = userRoleMapper.selectList(Wrappers.<SysUserRole>lambdaQuery()
                .eq(SysUserRole::getUserId, userId));
        return list.stream().map(SysUserRole::getRoleId).collect(Collectors.toList());
    }
}








