package com.erp.security.datascope;

import com.erp.security.LoginUser;
import com.erp.security.SecurityUtils;
import com.erp.system.domain.entity.SysUser;
import com.erp.system.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 数据权限辅助。
 * <p>
 * 单据落库时确定所属部门，供数据权限按部门过滤：
 * 请求指定的部门优先，其次继承来源单据的部门，最后回落到当前用户所在部门。
 * 部门为空时该单据仅本人可见（数据权限退化为 create_by 匹配）。
 */
@Component
@RequiredArgsConstructor
public class DataScopeHelper {

    private final SysUserMapper userMapper;

    /**
     * 解析单据所属部门。
     *
     * @param requestedDeptId 请求中指定的部门
     * @param sourceDeptId    来源单据的部门
     * @return 所属部门 ID；无法确定时返回 null
     */
    public Long resolveDeptId(Long requestedDeptId, Long sourceDeptId) {
        if (requestedDeptId != null) {
            return requestedDeptId;
        }
        if (sourceDeptId != null) {
            return sourceDeptId;
        }
        return currentUserDeptId();
    }

    /**
     * 当前登录用户所在部门。
     */
    public Long currentUserDeptId() {
        LoginUser loginUser = SecurityUtils.getCurrentUser();
        if (loginUser == null || loginUser.getUserId() == null) {
            return null;
        }
        SysUser user = userMapper.selectById(loginUser.getUserId());
        return user == null ? null : user.getDeptId();
    }
}
