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

    @GetMapping("/rank")
    @ApiOperation("技能者信用分排行榜（默认前10）")
    public Result<List<CreditRankVO>> rank(@RequestParam(defaultValue = "10") Integer limit) {
        return Result.success(creditService.getRank(limit));
    }
}
