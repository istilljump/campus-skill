package com.campus.runner.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.io.Serializable;

/**
 * 管理端-系统公告发布
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel("系统公告发布模型")
public class AnnouncementDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "公告标题", required = true)
    @NotBlank(message = "公告标题不能为空")
    @Size(max = 64, message = "公告标题不能超过64字")
    private String title;

    @ApiModelProperty(value = "公告内容")
    @Size(max = 500, message = "公告内容不能超过500字")
    private String content;
}
