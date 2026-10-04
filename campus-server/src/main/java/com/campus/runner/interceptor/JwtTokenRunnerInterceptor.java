package com.campus.runner.interceptor;

import com.campus.runner.constant.JwtClaimsConstant;
import com.campus.runner.constant.MessageConstant;
import com.campus.runner.context.BaseContext;
import com.campus.runner.exception.BusinessException;
import com.campus.runner.properties.JwtProperties;
import com.campus.runner.utils.JwtUtil;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 技能者端 JWT 拦截器：校验 token 合法性，并确认请求者为技能者身份
 */
@Component
@Slf4j
public class JwtTokenRunnerInterceptor implements HandlerInterceptor {

    @Autowired
    private JwtProperties jwtProperties;

    @Override
    @SneakyThrows()
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }

        String authentication = request.getHeader(jwtProperties.getUserTokenName());

        try {
            log.info("技能者端jwt校验:{}", authentication);
            Claims claims = JwtUtil.parseJWT(jwtProperties.getUserSecretKey(), authentication);
            Long runnerId = claims.get(JwtClaimsConstant.RUNNER_ID, Long.class);
            if (runnerId == null) {
                //token 中无技能者身份声明，拒绝访问
                log.warn("token中缺少技能者身份声明，拒绝访问");
                throw new BusinessException(MessageConstant.RUNNER_NOT_CERTIFIED);
            }
            Long userId = claims.get(JwtClaimsConstant.USER_ID, Long.class);
            log.info("当前技能者ID：{}，关联用户ID：{}", runnerId, userId);
            BaseContext.setCurrentId(runnerId);
            return true;
        } catch (ExpiredJwtException e) {
            throw new BusinessException("token已过期");
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException("token不合法");
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        BaseContext.removeCurrentId();
    }
}
