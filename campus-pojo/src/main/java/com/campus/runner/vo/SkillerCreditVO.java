package com.campus.runner.vo;

import io.swagger.annotations.ApiModel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * 技能者信用档案（当前分 + 等级 + 流水明细）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel("技能者信用档案")
public class SkillerCreditVO implements Serializable {

    private static final long serialVersionUID = 1L;

    //当前信用分
    private Integer creditScore;

    //信用等级 C1/C2/C3
    private String level;

    //等级说明
    private String levelDesc;

    //距下一等级还差多少分（C3 为 null）
    private Integer toNextLevel;

    //信用流水（最近 50 条）
    private List<CreditLogVO> logs;
}
