package com.campus.runner.controller.admin;

import com.campus.runner.result.Result;
import com.campus.runner.service.CreditService;
import com.campus.runner.vo.CreditRankVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 管理端-技能者信用分排行
 */
@RestController
@Slf4j
@Api(tags = "管理端-信用分接口")
@RequestMapping("/admin/credit")
public class CreditController {

    @Autowired
    private CreditService creditService;

    @GetMapping("/profile/export")
    @ApiOperation("导出指定技能者信用档案（Excel：概要 + 流水溯源）")
    public void profileExport(@RequestParam Long skillerId, javax.servlet.http.HttpServletResponse response) throws java.io.IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment;filename=" +
                java.net.URLEncoder.encode("信用档案-" + skillerId + ".xlsx", "UTF-8"));
        creditService.exportProfile(skillerId, response.getOutputStream());
    }

    @GetMapping("/rank")
    @ApiOperation("技能者信用分排行榜（默认前10）")
    public Result<List<CreditRankVO>> rank(@RequestParam(defaultValue = "10") Integer limit) {
        return Result.success(creditService.getRank(limit));
    }
}
