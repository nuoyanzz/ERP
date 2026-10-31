package com.erp.security.datascope;

/**
 * 数据权限范围。
 */
public enum DataScope {

    /** 全部数据 */
    ALL(1, "全部数据"),

    /** 本部门数据 */
    DEPT(2, "本部门数据"),

    /** 本部门及以下数据 */
    DEPT_AND_CHILD(3, "本部门及以下数据"),

    /** 仅本人数据 */
    SELF(4, "仅本人数据"),

    /** 自定义部门 */
    CUSTOM(5, "自定义部门");

    private final int code;
    private final String label;

    DataScope(int code, String label) {
        this.code = code;
        this.label = label;
    }

    public int getCode() {
        return code;
    }

    public String getLabel() {
        return label;
    }

    public static DataScope of(Integer code) {
        if (code == null) {
            return ALL;
        }
        for (DataScope scope : values()) {
            if (scope.code == code) {
                return scope;
            }
        }
        return ALL;
    }
}








