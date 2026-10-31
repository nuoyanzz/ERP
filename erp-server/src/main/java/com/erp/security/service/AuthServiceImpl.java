package com.erp.security.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.erp.common.exception.BizException;
import com.erp.security.JwtProperties;
import com.erp.security.JwtUtil;
import com.erp.security.dto.LoginRequest;
import com.erp.security.dto.LoginVO;
import com.erp.security.dto.RefreshTokenRequest;
import com.erp.system.domain.entity.SysLoginLog;
import com.erp.system.domain.entity.SysUser;
import com.erp.system.mapper.SysLoginLogMapper;
import com.erp.system.mapper.SysMenuMapper;
import com.erp.system.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private static final int STATUS_DISABLED = 1;
    /** 密码至少包含字母与数字 */

    private final SysUserMapper userMapper;
    private final SysMenuMapper menuMapper;
    private final SysLoginLogMapper loginLogMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final JwtProperties jwtProperties;
    private final PasswordPolicy passwordPolicy;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LoginVO login(LoginRequest request) {
        SysUser user = userMapper.selectOne(Wrappers.<SysUser>lambdaQuery()
                .eq(SysUser::getUsername, request.getUsername())
                .last("limit 1"));
        if (user == null) {
            recordLogin(request.getUsername(), 0, "用户名或密码错误");
            throw new BizException("用户名或密码错误");
        }
        if (user.getStatus() != null && STATUS_DISABLED == user.getStatus()) {
            recordLogin(request.getUsername(), 0, "用户已停用");
            throw new BizException("用户已停用，请联系管理员");
        }

        // 账号锁定检查
        if (user.getLockUntil() != null && user.getLockUntil().isAfter(LocalDateTime.now())) {
            throw new BizException("账号已锁定，请于 " + user.getLockUntil() + " 后重试");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            int failCount = handleLoginFailure(user);
            recordLogin(request.getUsername(), 0, "密码错误");
            if (failCount >= jwtProperties.getMaxLoginFailCount()) {
                throw new BizException("密码连续错误 " + failCount + " 次，账号已锁定 "
                        + jwtProperties.getLockMinutes() + " 分钟");
            }
            throw new BizException("用户名或密码错误");
        }

        // 登录成功：清零失败计数，记录登录信息
        SysUser successUpdate = new SysUser();
        successUpdate.setId(user.getId());
        successUpdate.setFailCount(0);
        successUpdate.setLockUntil(null);
        successUpdate.setLastLoginTime(LocalDateTime.now());
        successUpdate.setLastLoginIp(currentIp());
        userMapper.updateById(successUpdate);
        recordLogin(request.getUsername(), 1, null);

        List<String> perms = menuMapper.selectPermsByUserId(user.getId());
        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getNickname(), perms);
        String refreshToken = jwtUtil.generateRefreshToken(user.getId(), user.getUsername());

        return LoginVO.builder()
                .token(token)
                .refreshToken(refreshToken)
                .userId(user.getId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .perms(perms)
                .passwordExpired(isPasswordExpired(user))
                .build();
    }

    @Override
    public LoginVO refresh(RefreshTokenRequest request) {
        if (!jwtUtil.isRefreshToken(request.getRefreshToken())) {
            throw new BizException("刷新令牌无效");
        }
        String username;
        Long userId;
        try {
            var claims = jwtUtil.parseToken(request.getRefreshToken());
            username = claims.getSubject();
            Object id = claims.get(JwtUtil.CLAIM_USER_ID);
            userId = id == null ? null : Long.valueOf(String.valueOf(id));
        } catch (Exception e) {
            throw new BizException("刷新令牌已过期，请重新登录");
        }
        SysUser user = userId == null ? null : userMapper.selectById(userId);
        if (user == null || !user.getUsername().equals(username)) {
            throw new BizException("用户不存在");
        }
        if (user.getStatus() != null && STATUS_DISABLED == user.getStatus()) {
            throw new BizException("用户已停用");
        }
        List<String> perms = menuMapper.selectPermsByUserId(user.getId());
        return LoginVO.builder()
                .token(jwtUtil.generateToken(user.getId(), user.getUsername(), user.getNickname(), perms))
                .refreshToken(jwtUtil.generateRefreshToken(user.getId(), user.getUsername()))
                .userId(user.getId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .perms(perms)
                .passwordExpired(isPasswordExpired(user))
                .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void changePassword(Long userId, String oldPassword, String newPassword) {
        SysUser user = userMapper.selectById(userId);
        if (user == null) {
            throw new BizException("用户不存在");
        }
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new BizException("原密码不正确");
        }
        validatePasswordStrength(newPassword);
        if (passwordEncoder.matches(newPassword, user.getPassword())) {
            throw new BizException("新密码不能与原密码相同");
        }
        SysUser update = new SysUser();
        update.setId(userId);
        update.setPassword(passwordEncoder.encode(newPassword));
        update.setPwdUpdateTime(LocalDateTime.now());
        userMapper.updateById(update);
    }

    /**
     * 处理登录失败：累加失败次数，达到阈值后锁定账号。
     */
    private int handleLoginFailure(SysUser user) {
        int failCount = (user.getFailCount() == null ? 0 : user.getFailCount()) + 1;
        SysUser update = new SysUser();
        update.setId(user.getId());
        update.setFailCount(failCount);
        if (failCount >= jwtProperties.getMaxLoginFailCount()) {
            update.setLockUntil(LocalDateTime.now().plusMinutes(jwtProperties.getLockMinutes()));
        }
        userMapper.updateById(update);
        return failCount;
    }

    /**
     * 密码强度校验。
     */
    public void validatePasswordStrength(String password) {
        passwordPolicy.validate(password);
    }

    /**
     * 判断密码是否已过期。
     */
    private boolean isPasswordExpired(SysUser user) {
        int expireDays = jwtProperties.getPasswordExpireDays();
        if (expireDays <= 0 || user.getPwdUpdateTime() == null) {
            return false;
        }
        return user.getPwdUpdateTime().plusDays(expireDays).isBefore(LocalDateTime.now());
    }

    private void recordLogin(String username, int success, String errorMsg) {
        SysLoginLog log = new SysLoginLog();
        log.setUsername(username);
        log.setLoginIp(currentIp());
        log.setStatus(success);
        log.setErrorMsg(errorMsg);
        loginLogMapper.insert(log);
    }

    private String currentIp() {
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return null;
        }
        String forwarded = attributes.getRequest().getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return attributes.getRequest().getRemoteAddr();
    }
}








