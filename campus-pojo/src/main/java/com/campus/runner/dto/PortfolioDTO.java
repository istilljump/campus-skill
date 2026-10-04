package com.campus.runner.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;

/**
 * 技能者端-作品集上传
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel("作品集上传模型")
public class PortfolioDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    //作品标题
    @ApiModelProperty(value = "作品标题", required = true)
    @NotBlank(message = "作品标题不能为空")
    private String title;

    //所属技能类目id
    @ApiModelProperty("所属技能类目id")
    private Long categoryId;

    //封面图
    @ApiModelProperty("封面图")
    private String coverUrl;

    //作品文件URL列表，逗号分隔
    @ApiModelProperty("作品文件URL列表，逗号分隔")
    private String workUrls;

    //作品说明
    @ApiModelProperty("作品说明")
    private String description;
}
