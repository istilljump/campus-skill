package com.campus.runner.mapper;

import com.campus.runner.entity.Announcement;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface AnnouncementMapper {

    @Insert("insert into announcement (title, content, recipient_count, admin_id, admin_name, create_time) " +
            "values (#{title}, #{content}, #{recipientCount}, #{adminId}, #{adminName}, #{createTime})")
    void insert(Announcement announcement);

    @Select("select * from announcement order by id desc")
    List<Announcement> listLatest();
}
