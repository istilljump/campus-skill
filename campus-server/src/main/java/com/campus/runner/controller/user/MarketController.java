package com.campus.runner.controller.user;

import com.campus.runner.result.PageResult;
import com.campus.runner.result.Result;
import com.campus.runner.context.BaseContext;
import com.campus.runner.entity.UserBehavior;
import com.campus.runner.service.RecommendService;
import com.campus.runner.service.ServiceItemService;
import com.campus.runner.vo.ServiceItemVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 用户端-技能市场接口（浏览上架服务）
 */
@RestController
@Slf4j
@Api(tags = "用户端-技能市场接口")
@RequestMapping("/user/market")
public class MarketController {

    @Autowired
    private ServiceItemService serviceItemService;

    @Autowired
    private RecommendService recommendService;

    @GetMapping
    @ApiOperation("服务市场分页（仅上架，支持类目/关键词筛选）")
    public Result<PageResult<ServiceItemVO>> page(@RequestParam(defaultValue = "1") Integer page,
                                                  @RequestParam(defaultValue = "10") Integer pageSize,
                                                  Long categoryId, String keyword,
                                                  @RequestParam(defaultValue = "default") String sort) {
        return Result.success(serviceItemService.pageOnShelf(page, pageSize, categoryId, keyword, sort));
    }

    @GetMapping("/{id}")
    @ApiOperation("服务详情（src=行为来源：1市场列表 2猜你喜欢 3看了又看，推荐效果实验用）")
    public Result<ServiceItemVO> detail(@PathVariable Long id,
                                        @RequestParam(value = "src", required = false) Integer src) {
        ServiceItemVO vo = serviceItemService.getVOById(id);
        //行为埋点：浏览服务详情（推荐引擎数据源，旁路记录不影响主流程）
        recommendService.record(BaseContext.getCurrentId(), id, UserBehavior.TYPE_VIEW,
                src == null ? UserBehavior.SOURCE_OTHER : src);
        return Result.success(vo);
    }

    @GetMapping("/recommend")
    @ApiOperation("猜你喜欢：个性化推荐（冷启动回退热门精选，含推荐理由）")
    public Result<List<ServiceItemVO>> recommend(@RequestParam(defaultValue = "8") Integer limit) {
        return Result.success(recommendService.recommendForUser(BaseContext.getCurrentId(), limit));
    }

    @GetMapping("/{id}/similar")
    @ApiOperation("看了又看：与指定服务相似的其他服务（含推荐理由）")
    public Result<List<ServiceItemVO>> similar(@PathVariable Long id, @RequestParam(defaultValue = "4") Integer limit) {
        return Result.success(recommendService.similarItems(id, limit));
    }
}
