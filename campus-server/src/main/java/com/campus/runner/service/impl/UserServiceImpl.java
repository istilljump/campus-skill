package com.campus.runner.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.campus.runner.constant.MessageConstant;
import com.campus.runner.constant.WeChatConstant;
import com.campus.runner.dto.AccountLoginDTO;
import com.campus.runner.dto.AccountRegisterDTO;
import com.campus.runner.dto.PasswordChangeDTO;
import com.campus.runner.dto.UserUpdateDTO;
import com.campus.runner.entity.User;
import com.campus.runner.exception.BusinessException;
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
import org.springframework.util.DigestUtils;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

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
            // 本地联调模拟用户（campus_dev_{code}）默认昵称取 code，避免各端显示空白
            user = User.builder()
                    .openid(openid)
                    .name(openid != null && openid.startsWith("campus_dev_") ? openid.substring("campus_dev_".length()) : null)
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
    public void updateProfile(Long userId, com.campus.runner.dto.UserUpdateDTO dto) {
        User user = userMapper.getById(userId);
        if (user == null) {
            throw new com.campus.runner.exception.BusinessException(MessageConstant.ACCOUNT_NOT_FOUND);
        }
        User upd = new User();
        upd.setId(userId);
        upd.setName(dto.getName());
        upd.setPhone(dto.getPhone());
        upd.setStudentNo(dto.getStudentNo());
        upd.setCampus(dto.getCampus());
        userMapper.update(upd);
    }

    @Override
    public PageResult<User> page(Integer page, Integer pageSize, String name, String phone, String campus) {
        com.github.pagehelper.PageHelper.startPage(page, pageSize);
        com.github.pagehelper.Page<User> userPage = (com.github.pagehelper.Page<User>) userMapper.page(name, phone, campus);
        return new PageResult<>(userPage.getTotal(), userPage.getResult(), userPage.getPageSize(), userPage.getPageNum());
    }

    @Override
    public UserLoginVO register(AccountRegisterDTO dto) {
        String username = dto.getUsername().trim();
        if (userMapper.getByUsername(username) != null) {
            throw new BusinessException("该账号已被注册，请更换");
        }
        //BCrypt 存储（自带随机盐与慢哈希）；salt 列保留以兼容历史数据与提现密码口径
        String salt = UUID.randomUUID().toString().replace("-", "");
        String hashed = com.campus.runner.utils.PasswordUtil.encode(dto.getPassword());
        User user = User.builder()
                .openid("acct_" + salt.substring(0, 16))  //非微信注册用户，openid 用账号标识占位
                .username(username)
                .password(hashed)
                .salt(salt)
                .name(dto.getName() == null || dto.getName().isBlank() ? username : dto.getName().trim())
                .createTime(LocalDateTime.now())
                .build();
        try {
            userMapper.saveAccount(user);
        } catch (org.springframework.dao.DuplicateKeyException e) {
            //并发注册同名账号：唯一索引兜底，返回友好提示而非系统异常
            throw new BusinessException("该账号已被注册，请更换");
        }
        return buildToken(user, "acct_" + username);
    }

    @Override
    public UserLoginVO loginByAccount(AccountLoginDTO dto) {
        User user = userMapper.getByUsername(dto.getUsername().trim());
        //账号不存在与密码错误统一提示，避免账号枚举探测
        if (user == null || user.getPassword() == null) {
            throw new BusinessException("账号或密码不正确");
        }
        if (!com.campus.runner.utils.PasswordUtil.matches(dto.getPassword(), user.getSalt(), user.getPassword())) {
            throw new BusinessException("账号或密码不正确");
        }
        //旧 MD5 口径登录成功后自动升级为 BCrypt（渐进迁移，无需重置全部密码）
        if (com.campus.runner.utils.PasswordUtil.needsRehash(user.getPassword())) {
            userMapper.updatePassword(user.getId(), com.campus.runner.utils.PasswordUtil.encode(dto.getPassword()), user.getSalt());
        }
        return buildToken(user, "acct_" + user.getUsername());
    }

    @Override
    public void changePassword(Long userId, PasswordChangeDTO dto) {
        User user = userMapper.getById(userId);
        if (user == null) {
            throw new BusinessException(MessageConstant.ACCOUNT_NOT_FOUND);
        }
        if (user.getPassword() != null
                && !com.campus.runner.utils.PasswordUtil.matches(dto.getOldPassword(), user.getSalt(), user.getPassword())) {
            throw new BusinessException("原密码不正确");
        }
        String salt = UUID.randomUUID().toString().replace("-", "");
        String hashed = com.campus.runner.utils.PasswordUtil.encode(dto.getNewPassword());
        userMapper.updatePassword(userId, hashed, salt);
    }

    /**
     * 签发用户端 JWT（claims 与微信登录保持一致，token 三端隔离由拦截器保证）
     */
    private UserLoginVO buildToken(User user, String openidClaim) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getId());
        claims.put("openid", openidClaim);
        String token = JwtUtil.createJWT(jwtProperties.getUserSecretKey(), jwtProperties.getUserTtl(), claims);
        return UserLoginVO.builder()
                .id(user.getId())
                .openid(openidClaim)
                .token(token)
                .build();
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
