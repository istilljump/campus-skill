package com.campus.runner.service.impl;

import com.campus.runner.constant.RedisConstant;
import com.campus.runner.service.ShopService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class ShopServiceImpl implements ShopService {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Override
    public Integer getShopStatus() {
        Integer status = (Integer) redisTemplate.opsForValue().get(RedisConstant.SHOP_STATUS_KEY);
        //首次部署无缓存时默认营业中
        return status == null ? 1 : status;
    }

    @Override
    public void setShopStatus(Integer status) {
        redisTemplate.opsForValue().set(RedisConstant.SHOP_STATUS_KEY, status);
    }
}
