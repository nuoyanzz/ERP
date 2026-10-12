package com.erp.system.domain.vo;

import lombok.Data;

import java.util.List;

@Data
public class MenuTreeVO {

    private Long id;
    private Long parentId;
    private String menuName;
    private Integer menuType;
    private String perms;
    private String path;
    private String component;
    private String icon;
    private Integer sortNo;
    private Integer visible;
    private List<MenuTreeVO> children;
}








