package com.campus.runner.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * 用户端-订单评价提交
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel("订单评价提交模型")
public class ReviewSubmitDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    //订单id
    @ApiModelProperty(value = "订单id", required = true)
    @NotNull(message = "订单id不能为空")
    private Long orderId;

    //评分 1-5
    @ApiModelProperty(value = "评分 1-5", required = true)
    @NotNull(message = "评分不能为空")
    @Min(value = 1, message = "评分最低1分")
    @Max(value = 5, message = "评分最高5分")
    private Integer score;

    //评价内容
    @ApiModelProperty("评价内容")
    private String content;

    //评价标签，逗号分隔
    @ApiModelProperty("评价标签")
    private String tags;

    //是否匿名 0否 1是
    @ApiModelProperty("是否匿名 0否 1是")
    private Integer isAnonymous;
}
