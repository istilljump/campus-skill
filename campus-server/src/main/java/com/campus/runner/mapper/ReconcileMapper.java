package com.campus.runner.mapper;

import com.campus.runner.vo.ReconcileRowVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ReconcileMapper {

    /**
     * 全量扫描：每个钱包的当前余额 + 按流水重算的期望余额。
     * 期望余额恒等式：收入类流水（1技能收入/3充值/5退款）记正，支出类（2支付/4提现/6违约金）记负。
     * 流水与余额全部经 changeBalance/deductBestEffort 写入，二者必须严格一致。
     */
    @Select("select w.id as walletId, w.user_id as userId, w.balance as balance, " +
            "ifnull(t.expected, 0) as expected, ifnull(t.cnt, 0) as txCount " +
            "from wallet_account w " +
            "left join (select wallet_id, " +
            "  sum(case when type in (1, 3, 5) then amount else -amount end) as expected, " +
            "  count(*) as cnt " +
            "  from wallet_transaction group by wallet_id) t on t.wallet_id = w.id")
    List<ReconcileRowVO> scanAll();

    /**
     * 钱包总数（对账汇总展示用）
     */
    @Select("select count(*) from wallet_account")
    long countWallets();
}
