package com.campus.runner.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户（发单用户，认证后可成为技能者）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    //微信用户唯一标识
    private String openid;

    //登录账号（账号密码登录，唯一；为空表示尚未开通账号密码登录）
    private String username;

    //登录密码（MD5(salt + 明文)，任何接口不出参）
    @JsonIgnore
    private String password;

    //密码盐值
    @JsonIgnore
    private String salt;

    //姓名
    private String name;

    //手机号
    private String phone;

    //性别 0 女 1 男
    private String sex;

    //身份证号
    private String idNumber;

    //头像
    private String avatar;

    //学号
    private String studentNo;

    //校区
    private String campus;

    //信用分
    private Integer creditScore;

    //累计发单数
    private Integer publishOrderCount;

    //注册时间
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
