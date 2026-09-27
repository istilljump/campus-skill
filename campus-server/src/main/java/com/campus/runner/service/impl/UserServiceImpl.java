package com.campus.runner.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.campus.runner.constant.MessageConstant;
import com.campus.runner.constant.WeChatConstant;
import com.campus.runner.entity.User;
import com.campus.runner.exception.LoginFailedException;
import com.campus.runner.mapper.UserMapper;
import com.campus.runner.result.PageResult;
import com.campus.runner.properties.JwtProperties;
import com.campus.runner.properties.WeChatProperties;
import com.campus.runner.service.UserService;
import com.campus.runner.utils.HttpClientUtil;
import com.campus.runner.utils.JwtUtil;
import com.campus.runner.vo.UserLoginVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class UserServiceImpl implements UserService {

    @Autowired
    private WeChatProperties weChatProperties;

    @Autowired
    private JwtProperties jwtProperties;

    @Autowired
    private UserMapper userMapper;

    @Override
    public UserLoginVO login(String code) {
        String openid = resolveOpenid(code);

        // 查找数据库中是否存在对应openid的用户
        User user = userMapper.getUserByOpenid(openid);

        if (user == null) {
            // 数据库中不存在对应用户，默认新增对应openid的用户
            user = User.builder()
                    .openid(openid)
                    .createTime(LocalDateTime.now())
                    .build();
            userMapper.saveUser(user);
        }

        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getId());
        claims.put("openid", openid);
        String token = JwtUtil.createJWT(jwtProperties.getUserSecretKey(), jwtProperties.getUserTtl(), claims);

        return UserLoginVO.builder()
                .id(user.getId())
                .openid(openid)
                .token(token)
                .build();
    }

    @Override
    public User getProfile(Long userId) {
        User user = userMapper.getById(userId);
        if (user == null) {
            throw new com.campus.runner.exception.BusinessException(MessageConstant.ACCOUNT_NOT_FOUND);
        }
        return user;
    }

    @Override
    public PageResult<User> page(Integer page, Integer pageSize, String name, String phone, String campus) {
        com.github.pagehelper.PageHelper.startPage(page, pageSize);
        com.github.pagehelper.Page<User> userPage = (com.github.pagehelper.Page<User>) userMapper.page(name, phone, campus);
        return new PageResult<>(userPage.getTotal(), userPage.getResult(), userPage.getPageSize(), userPage.getPageNum());
    }

    /**
     * 通过微信 code 换取 openid；本地开发未配置微信 appid 时使用模拟 openid，便于联调
     */
    private String resolveOpenid(String code) {
        String appid = weChatProperties.getAppid();
        if (appid == null || appid.isBlank() || "placeholder".equals(appid)) {
            log.warn("未配置微信appid，使用本地模拟openid联调");
            return "campus_dev_" + code;
        }
        Map<String, String> queryParams = new HashMap<>();
        queryParams.put(WeChatConstant.PARAM_APPID, appid);
        queryParams.put(WeChatConstant.PARAM_SECRET, weChatProperties.getSecret());
        queryParams.put(WeChatConstant.PARAM_JS_CODE, code);
        queryParams.put(WeChatConstant.PARAM_GRANT_TYPE, WeChatConstant.GRANT_TYPE_AUTHORIZATION_CODE);
        String response = HttpClientUtil.doGet(WeChatConstant.WECHAT_SERVER_LOGIN_URL, queryParams);

        JSONObject jsonObject = JSON.parseObject(response);
        String openid = jsonObject.getString("openid");
        if (openid == null) {
            throw new LoginFailedException(MessageConstant.LOGIN_FAILED);
        }
        return openid;
    }
}
