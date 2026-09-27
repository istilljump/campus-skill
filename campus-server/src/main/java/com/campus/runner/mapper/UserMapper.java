package com.campus.runner.mapper;

import com.campus.runner.entity.User;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface UserMapper {

    @Select("select * from user where openid = #{openid}")
    User getUserByOpenid(String openid);

    @Select("select * from user where id = #{id}")
    User getById(Long id);

    @Insert("insert into user (openid, name, phone, sex, id_number, avatar, create_time) " +
            "values (#{openid}, #{name}, #{phone}, #{sex}, #{idNumber}, #{avatar}, #{createTime})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void saveUser(User user);

    int update(User user);

    List<User> page(@Param("name") String name, @Param("phone") String phone, @Param("campus") String campus);

    @Update("update user set publish_order_count = publish_order_count + 1 where id = #{id}")
    void incrementPublishOrderCount(Long id);

    @Update("update user set credit_score = greatest(0, credit_score + #{delta}) where id = #{id}")
    void adjustCreditScore(@org.apache.ibatis.annotations.Param("id") Long id, @org.apache.ibatis.annotations.Param("delta") Integer delta);
}
