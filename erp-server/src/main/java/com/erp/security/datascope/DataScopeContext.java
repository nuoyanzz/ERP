package com.erp.security.datascope;

import java.util.ArrayList;
import java.util.List;

/**
 * 数据权限上下文。保存当前请求解析出的数据范围，供业务查询追加过滤条件。
 * <p>
 * 使用 ThreadLocal，请求结束时由 {@code DataScopeInterceptor} 清理。
 */
public final class DataScopeContext {

    private static final ThreadLocal<DataScopeInfo> HOLDER = new ThreadLocal<>();

    /**
     * 数据权限忽略标记。
     * <p>
     * 取业务单号（如查「本月最大单号序号」）属于系统级约束查询，
     * 单号要求全库唯一，不能被部门数据范围裁剪，否则不同部门会取到相同序号导致唯一键冲突。
     * 此处临时置位，让拦截器跳过改写。
     */
    private static final ThreadLocal<Boolean> IGNORED = ThreadLocal.withInitial(() -> Boolean.FALSE);

    private DataScopeContext() {
    }

    public static void set(DataScopeInfo info) {
        HOLDER.set(info);
    }

    public static DataScopeInfo get() {
        return HOLDER.get();
    }

    public static void clear() {
        HOLDER.remove();
        IGNORED.remove();
    }

    /**
     * 当前是否忽略数据权限。
     */
    public static boolean isIgnored() {
        return Boolean.TRUE.equals(IGNORED.get());
    }

    /**
     * 在忽略数据权限的前提下执行一段逻辑。
     *
     * @param action 待执行逻辑
     * @return 逻辑的返回值
     */
    public static <T> T ignoringScope(java.util.function.Supplier<T> action) {
        boolean previous = isIgnored();
        IGNORED.set(Boolean.TRUE);
        try {
            return action.get();
        } finally {
            IGNORED.set(previous);
        }
    }

    /**
     * 解析出的数据范围。
     */
    public static class DataScopeInfo {

        private DataScope scope;
        private Long userId;
        private Long deptId;
        private List<Long> deptIds = new ArrayList<>();

        public DataScope getScope() {
            return scope;
        }

        public void setScope(DataScope scope) {
            this.scope = scope;
        }

        public Long getUserId() {
            return userId;
        }

        public void setUserId(Long userId) {
            this.userId = userId;
        }

        public Long getDeptId() {
            return deptId;
        }

        public void setDeptId(Long deptId) {
            this.deptId = deptId;
        }

        public List<Long> getDeptIds() {
            return deptIds;
        }

        public void setDeptIds(List<Long> deptIds) {
            this.deptIds = deptIds;
        }

        /**
         * 是否限制部门维度。
         */
        public boolean hasDeptRestriction() {
            return scope != DataScope.ALL && scope != DataScope.SELF && deptIds != null && !deptIds.isEmpty();
        }

        /**
         * 是否限制创建人维度。
         */
        public boolean hasSelfRestriction() {
            return scope == DataScope.SELF;
        }
    }
}








