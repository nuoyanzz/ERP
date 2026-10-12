package com.erp.system.domain.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class MenuSaveRequest {

    private Long id;

    @NotNull(message = "上级菜单不能为空")
    private Long parentId;

    @NotBlank(message = "菜单名称不能为空")
    @Size(max = 50, message = "菜单名称长度不能超过50")
    private String menuName;

    @NotNull(message = "菜单类型不能为空")
    @Min(value = 1, message = "菜单类型：1目录 2菜单 3按钮")
    @Max(value = 3, message = "菜单类型：1目录 2菜单 3按钮")
    private Integer menuType;

    @Size(max = 100, message = "权限标识长度不能超过100")
    private String perms;

    @Size(max = 200, message = "路由地址长度不能超过200")
    private String path;

    @Size(max = 200, message = "组件路径长度不能超过200")
    private String component;

    @Size(max = 100, message = "图标长度不能超过100")
    private String icon;

    @Min(value = 0, message = "排序不能小于0")
    private Integer sortNo;

    @Min(value = 0, message = "显示状态取值0-1")
    @Max(value = 1, message = "显示状态取值0-1")
    private Integer visible;
}








