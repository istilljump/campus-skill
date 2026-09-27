package com.campus.runner.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 管理端-统计查询（日期区间）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StatisticsQueryDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    //开始日期
    private String begin;

    //结束日期
    private String end;
}
