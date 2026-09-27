package com.campus.runner.mapper;

import com.campus.runner.entity.Message;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface MessageMapper {

    @Insert("insert into message (recipient_id, title, content, order_id, is_read, create_time) " +
            "values (#{recipientId}, #{title}, #{content}, #{orderId}, 0, #{createTime})")
    void insert(Message message);

    @Select("select * from message where recipient_id = #{userId} order by id desc limit #{limit}")
    List<Message> listLatest(@Param("userId") Long userId, @Param("limit") int limit);

    @Select("select count(*) from message where recipient_id = #{userId} and is_read = 0")
    int countUnread(@Param("userId") Long userId);

    @Update("update message set is_read = 1 where recipient_id = #{userId} and is_read = 0")
    void markAllRead(@Param("userId") Long userId);
}
