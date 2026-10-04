package com.campus.runner.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户端-服务预约申请
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel("服务预约申请模型")
public class BookingApplyDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    //服务id
    @ApiModelProperty(value = "服务id", required = true)
    @NotNull(message = "服务id不能为空")
    private Long serviceItemId;

    //期望开始时间（支持 2026-10-06 14:00 或 2026-10-06 14:00:00）
    @ApiModelProperty("期望开始时间，如 2026-10-06 14:00")
    private String expectTime;

    //需求备注
    @ApiModelProperty("需求备注")
    @Size(max = 500, message = "备注不能超过500字")
    private String remark;
}
