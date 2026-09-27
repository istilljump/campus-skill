package com.campus.runner.service;

import com.campus.runner.dto.UserUpdateDTO;
import com.campus.runner.entity.User;
import com.campus.runner.result.PageResult;
import com.campus.runner.vo.UserLoginVO;

public interface UserService {

    UserLoginVO login(String code);

    /**
     * 管理端-用户列表分页
     */
    PageResult<User> page(Integer page, Integer pageSize, String name, String phone, String campus);

    /**
     * 用户端-个人信息（信用分/发单数/校区）
     */
    User getProfile(Long userId);

    /**
     * 用户端-编辑个人资料（昵称/手机号/学号/校区）
     */
    void updateProfile(Long userId, UserUpdateDTO userUpdateDTO);
}
