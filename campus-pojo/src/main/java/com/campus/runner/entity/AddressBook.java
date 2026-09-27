package com.campus.runner.entity;

import com.campus.runner.valid.groups.Update;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.Range;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import java.io.Serializable;

/**
 * 地址簿（校园收货地址）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel("收货地址")
public class AddressBook implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty("地址ID")
    @NotNull(groups = Update.class, message = "地址ID不能为空")
    private Long id;

    //用户id
    @ApiModelProperty(value = "用户ID", hidden = true)
    private Long userId;

    //收货人
    @ApiModelProperty(value = "收货人姓名", required = true)
    @NotBlank(message = "收货人姓名不能为空")
    private String consignee;

    //手机号
    @ApiModelProperty(value = "手机号", required = true)
    @NotBlank(message = "手机号不能为空")
    private String phone;

    //性别 0 女 1 男
    @ApiModelProperty(value = "性别")
    @Pattern(regexp = "[0-1]", message = "性别不合法")
    private String sex = "1";

    //校区
    @ApiModelProperty(value = "校区", required = true)
    @NotBlank(message = "校区不能为空")
    private String campus;

    //楼栋
    @ApiModelProperty(value = "楼栋", required = true)
    @NotBlank(message = "楼栋不能为空")
    private String building;

    //房间号
    @ApiModelProperty(value = "房间号")
    private String room;

    //详细地址
    @ApiModelProperty(value = "详细地址")
    private String detail;

    //标签
    @ApiModelProperty("地址标签")
    private String label;

    //是否默认 0否 1是
    @ApiModelProperty(value = "是否为默认地址", required = true)
    @Range(max = 1L, message = "isDefault不合法")
    private Integer isDefault = 0;

    public String detailedAddress() {
        StringBuilder sb = new StringBuilder();
        if (campus != null) {
            sb.append(campus);
        }
        if (building != null) {
            sb.append(building);
        }
        if (room != null) {
            sb.append(room);
        }
        if (detail != null) {
            sb.append(detail);
        }
        return sb.toString();
    }
}
