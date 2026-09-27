package com.campus.runner.mapper;

import com.campus.runner.entity.RunnerAudit;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface RunnerAuditMapper {

    @Insert("insert into runner_audit (runner_id, real_name, student_no, campus, college, id_card, student_card_img, " +
            "status, apply_time) values (#{runnerId}, #{realName}, #{studentNo}, #{campus}, #{college}, #{idCard}, " +
            "#{studentCardImg}, #{status}, #{applyTime})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(RunnerAudit runnerAudit);

    @Select("select * from runner_audit where id = #{id}")
    RunnerAudit getById(Long id);

    @Select("select * from runner_audit where runner_id = #{runnerId} order by apply_time desc limit 1")
    RunnerAudit getLatestByRunnerId(Long runnerId);

    /**
     * 管理端-审核记录列表
     */
    List<RunnerAudit> page(@Param("status") Integer status, @Param("studentNo") String studentNo);

    int update(RunnerAudit runnerAudit);
}
