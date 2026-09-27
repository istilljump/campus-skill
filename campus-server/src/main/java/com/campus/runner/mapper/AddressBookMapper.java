package com.campus.runner.mapper;

import com.campus.runner.entity.AddressBook;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface AddressBookMapper {

    @Insert("insert into address_book (user_id, consignee, sex, phone, campus, building, room, detail, label, is_default) " +
            "values (#{userId}, #{consignee}, #{sex}, #{phone}, #{campus}, #{building}, #{room}, #{detail}, #{label}, #{isDefault})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int saveAddress(AddressBook addressBook);

    @Select("select * from address_book where user_id = #{userId} order by is_default desc, id desc")
    List<AddressBook> getAllAddress(Long userId);

    @Select("select * from address_book where user_id = #{userId} and is_default = 1")
    AddressBook getDefaultAddress(Long userId);

    @Select("select * from address_book where user_id = #{userId} and id = #{id}")
    AddressBook getAddressById(Long userId, Long id);

    @Select("select * from address_book where id = #{id}")
    AddressBook getById(Long id);

    @Delete("delete from address_book where user_id = #{userId} and id = #{id}")
    int deleteAddressById(Long userId, Long id);

    int updateAddress(AddressBook address);
}
