package com.campus.runner.mapper;

import com.campus.runner.entity.ErrandType;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface ErrandTypeMapper {

    @Select("select * from errand_type where status = 1 order by sort")
    List<ErrandType> listEnabled();

    @Select("select * from errand_type order by sort")
    List<ErrandType> listAll();

    @Select("select * from errand_type where id = #{id}")
    ErrandType getById(Long id);

    @Insert("insert into errand_type (name, icon, description, fee_rate, sort, status, create_time, update_time) " +
            "values (#{name}, #{icon}, #{description}, #{feeRate}, #{sort}, #{status}, #{createTime}, #{updateTime})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(ErrandType errandType);

    int update(ErrandType errandType);

    @Update("update errand_type set status = #{status}, update_time = now() where id = #{id}")
    void updateStatus(@org.apache.ibatis.annotations.Param("id") Long id, @org.apache.ibatis.annotations.Param("status") Integer status);
}
