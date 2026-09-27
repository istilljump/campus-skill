package com.campus.runner.controller.admin;

import com.campus.runner.dto.ErrandTypeDTO;
import com.campus.runner.result.Result;
import com.campus.runner.service.ErrandTypeService;
import com.campus.runner.vo.ErrandTypeVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
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
 * 管理端-跑腿订单类型管理
 */
@RestController
@Slf4j
@Api(tags = "管理端-订单类型管理接口")
@RequestMapping("/admin/errandType")
public class ErrandTypeController {

    @Autowired
    private ErrandTypeService errandTypeService;

    @GetMapping("/list")
    @ApiOperation("全部订单类型")
    public Result<List<ErrandTypeVO>> listAll() {
        return Result.success(errandTypeService.listAll());
    }

    @PostMapping
    @ApiOperation("新增订单类型")
    public Result<String> save(@RequestBody @Valid ErrandTypeDTO errandTypeDTO) {
        errandTypeService.save(errandTypeDTO);
        return Result.success("新增成功");
    }

    @PutMapping
    @ApiOperation("修改订单类型")
    public Result<String> update(@RequestBody @Valid ErrandTypeDTO errandTypeDTO) {
        errandTypeService.update(errandTypeDTO);
        return Result.success("修改成功");
    }

    @PutMapping("/status/{status}")
    @ApiOperation("启用/停用订单类型")
    public Result<String> updateStatus(@PathVariable Integer status, Long id) {
        errandTypeService.updateStatus(id, status);
        return Result.success(status == 1 ? "已启用" : "已停用");
    }

    @DeleteMapping("/{id}")
    @ApiOperation("删除订单类型")
    public Result<String> delete(@PathVariable Long id) {
        errandTypeService.delete(id);
        return Result.success("删除成功");
    }
}
