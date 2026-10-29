package com.campus.runner.mapper;

import com.campus.runner.entity.ReconcileRecord;
import com.github.pagehelper.Page;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ReconcileRecordMapper {

    @Insert("insert into reconcile_record (batch_no, wallet_id, user_id, balance, expected_balance, diff_amount, " +
            "tx_count, checked_wallets, create_time) " +
            "values (#{batchNo}, #{walletId}, #{userId}, #{balance}, #{expectedBalance}, #{diffAmount}, " +
            "#{txCount}, #{checkedWallets}, #{createTime})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(ReconcileRecord record);

    /**
     * 对账差异分页（可按批次过滤，最新批次在前）
     */
    Page<ReconcileRecord> page(@Param("batchNo") String batchNo, Integer page, Integer pageSize);

    /**
     * 最近一个批次号
     */
    @Select("select batch_no from reconcile_record order by id desc limit 1")
    String latestBatchNo();

    /**
     * 指定批次的差异条数
     */
    @Select("select count(*) from reconcile_record where batch_no = #{batchNo}")
    long countByBatch(@Param("batchNo") String batchNo);

    /**
     * 指定批次全部差异行（导出用）
     */
    @Select("select * from reconcile_record where batch_no = #{batchNo} order by diff_amount desc")
    java.util.List<ReconcileRecord> listByBatch(@Param("batchNo") String batchNo);

    // ---------- 批次汇总（每轮对账必落一行，0 差异轮也落，保证"最近批次"语义准确） ----------

    @Insert("insert into reconcile_batch (batch_no, checked_wallets, mismatch_count, all_matched, create_time) " +
            "values (#{batchNo}, #{checkedWallets}, #{mismatchCount}, #{allMatched}, #{createTime}) " +
            "on duplicate key update checked_wallets = values(checked_wallets), mismatch_count = values(mismatch_count), " +
            "all_matched = values(all_matched), create_time = values(create_time)")
    void insertBatch(@Param("batchNo") String batchNo, @Param("checkedWallets") int checkedWallets,
                     @Param("mismatchCount") int mismatchCount, @Param("allMatched") boolean allMatched,
                     @Param("createTime") java.time.LocalDateTime createTime);

    /**
     * 最近一次对账批次（含 0 差异轮），无记录返回 null
     */
    @Select("select batch_no, checked_wallets, mismatch_count, all_matched, create_time from reconcile_batch " +
            "order by create_time desc limit 1")
    java.util.Map<String, Object> latestBatch();
}
