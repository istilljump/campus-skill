package com.campus.runner.mapper;

import com.campus.runner.entity.Booking;
import com.campus.runner.vo.BookingVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface BookingMapper {

    void insert(Booking booking);

    Booking getById(Long id);

    @Select("select * from booking where order_id = #{orderId} limit 1")
    Booking getByOrderId(Long orderId);

    List<BookingVO> listByUser(Long userId);

    List<BookingVO> listBySkiller(@Param("skillerId") Long skillerId, @Param("status") Integer status);

    @Update("update booking set status = #{status}, update_time = now() where id = #{id}")
    void updateStatus(@Param("id") Long id, @Param("status") Integer status);

    @Update("update booking set order_id = #{orderId}, update_time = now() where id = #{id}")
    void updateOrderId(@Param("id") Long id, @Param("orderId") Long orderId);
}
