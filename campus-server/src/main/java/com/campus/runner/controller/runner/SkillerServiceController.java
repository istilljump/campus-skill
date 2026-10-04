package com.campus.runner.controller.runner;

import com.campus.runner.context.BaseContext;
import com.campus.runner.dto.ServiceItemDTO;
import com.campus.runner.entity.ServiceItem;
import com.campus.runner.result.Result;
import com.campus.runner.service.ServiceItemService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;

/**
 * 技能者端-服务货架接口（挂牌/编辑/上下架服务）
 */
@RestController
@Slf4j
@Api(tags = "技能者端-服务货架接口")
@RequestMapping("/skiller/service")
public class SkillerServiceController {

    @Autowired
    private ServiceItemService serviceItemService;

    @GetMapping("/list")
    @ApiOperation("我的服务列表")
    public Result<List<ServiceItem>> list() {
        return Result.success(serviceItemService.listOwn(BaseContext.getCurrentId()));
    }

    @PostMapping
    @ApiOperation("发布服务（默认上架）")
    public Result<Long> create(@RequestBody @Valid ServiceItemDTO serviceItemDTO) {
        return Result.success(serviceItemService.create(BaseContext.getCurrentId(), serviceItemDTO));
    }

    @PutMapping("/{id}")
    @ApiOperation("编辑自己的服务")
    public Result<String> update(@PathVariable Long id, @RequestBody @Valid ServiceItemDTO serviceItemDTO) {
        serviceItemService.update(BaseContext.getCurrentId(), id, serviceItemDTO);
        return Result.success("服务已更新");
    }

    @PutMapping("/{id}/status/{status}")
    @ApiOperation("上架/下架服务（0下架 1上架）")
    public Result<String> updateStatus(@PathVariable Long id, @PathVariable Integer status) {
        serviceItemService.updateStatus(BaseContext.getCurrentId(), id, status);
        return Result.success("状态已更新");
    }
}
