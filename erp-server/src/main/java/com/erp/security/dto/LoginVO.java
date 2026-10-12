package com.erp.security.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginVO {

    private String token;
    /** 刷新令牌，用于换取新的访问令牌 */
    private String refreshToken;
    private Long userId;
    private String username;
    private String nickname;
    private List<String> perms;
    /** 密码是否已过期，前端应引导用户修改密码 */
    private boolean passwordExpired;
}








