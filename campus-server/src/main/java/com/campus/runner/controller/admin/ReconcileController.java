package com.campus.runner.controller.admin;

import com.campus.runner.entity.ReconcileRecord;
import com.campus.runner.mapper.ReconcileRecordMapper;
import com.campus.runner.result.PageResult;
import com.campus.runner.result.Result;
import com.campus.runner.service.ReconciliationService;
import com.campus.runner.utils.ExcelWriterUtil;
import com.campus.runner.vo.ReconcileSummaryVO;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.poi.xssf.streaming.SXSSFWorkbook;

/**
 * 管理端-资金对账中心
 */
@RestController
@Slf4j
@Api(tags = "管理端-资金对账接口")
@RequestMapping("/admin/reconcile")
public class ReconcileController {

    @Autowired
    private ReconciliationService reconciliationService;

    @Autowired
    private ReconcileRecordMapper reconcileRecordMapper;

    @Autowired
    private com.campus.runner.mapper.ReconcileMapper reconcileMapper;

    @PostMapping("/run")
    @ApiOperation("手动触发一轮全量对账（定时任务每小时自动执行）")
    public Result<ReconcileSummaryVO> run() {
        return Result.success(reconciliationService.reconcileAll());
    }

    @GetMapping("/page")
    @ApiOperation("对账差异分页（可按批次过滤）")
    public Result<PageResult<ReconcileRecord>> page(@RequestParam(defaultValue = "1") Integer page,
                                                    @RequestParam(defaultValue = "10") Integer pageSize,
                                                    String batchNo) {
        PageHelper.startPage(page, pageSize);
        Page<ReconcileRecord> records = reconcileRecordMapper.page(batchNo, page, pageSize);
        return Result.success(new PageResult<>(records.getTotal(), records.getResult(), records.getPageSize(), records.getPageNum()));
    }

    @GetMapping("/summary")
    @ApiOperation("最近一次对账批次汇总（批次表口径，0 差异轮也保留）")
    public Result<ReconcileSummaryVO> summary() {
        long wallets = reconcileMapper.countWallets();
        Map<String, Object> batch = reconcileRecordMapper.latestBatch();
        if (batch == null) {
            return Result.success(ReconcileSummaryVO.builder()
                    .checkedWallets(wallets).mismatchCount(0L).cleanCount(wallets).allMatched(true).build());
        }
        long mismatch = batch.get("mismatch_count") != null ? ((Number) batch.get("mismatch_count")).longValue() : 0;
        long checked = batch.get("checked_wallets") != null ? ((Number) batch.get("checked_wallets")).longValue() : wallets;
        return Result.success(ReconcileSummaryVO.builder()
                .batchNo(String.valueOf(batch.get("batch_no")))
                .checkedWallets(checked)
                .mismatchCount(mismatch)
                .cleanCount(checked - mismatch)
                .allMatched(mismatch == 0)
                .build());
    }

    @GetMapping("/export")
    @ApiOperation("导出指定批次对账报告（Excel）")
    public void export(@RequestParam String batchNo, HttpServletResponse response) throws IOException {
        List<ReconcileRecord> records = reconcileRecordMapper.listByBatch(batchNo);
        List<List<Object>> rows = records.stream()
                .map(r -> Arrays.<Object>asList(r.getBatchNo(), r.getWalletId(), r.getUserId(),
                        r.getBalance(), r.getExpectedBalance(), r.getDiffAmount(), r.getTxCount(), r.getCreateTime()))
                .collect(Collectors.toList());
        SXSSFWorkbook workbook = new SXSSFWorkbook();
        ExcelWriterUtil.writeSheet(workbook, "对账差异明细",
                Arrays.asList("批次号", "钱包ID", "用户ID", "当前余额", "期望余额", "差异金额", "流水笔数", "对账时间"), rows);
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment;filename=" +
                URLEncoder.encode("对账报告-" + batchNo + ".xlsx", "UTF-8"));
        workbook.write(response.getOutputStream());
        workbook.dispose();
    }
}
