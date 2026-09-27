package com.campus.runner.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 数据大屏-名称计数项（类型TOP / 校区分布等通用）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class NameCountVO implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 名称（类型名/校区名） */
    private String name;
    /** 数量 */
    private Integer count;
}
