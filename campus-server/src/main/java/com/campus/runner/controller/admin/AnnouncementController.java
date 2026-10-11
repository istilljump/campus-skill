package com.campus.runner.controller.admin;

import com.campus.runner.context.BaseContext;
import com.campus.runner.dto.AnnouncementDTO;
import com.campus.runner.entity.Announcement;
import com.campus.runner.result.PageResult;
import com.campus.runner.result.Result;
import com.campus.runner.service.AnnouncementService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

/**
 * 管理端-系统公告
 */
@RestController
@Slf4j
@Api(tags = "管理端-系统公告接口")
@RequestMapping("/admin/message")
public class AnnouncementController {

    @Autowired
    private AnnouncementService announcementService;

    @PostMapping("/broadcast")
    @ApiOperation("发布系统公告（广播全体用户与技能者）")
    public Result<String> broadcast(@RequestBody @Valid AnnouncementDTO announcementDTO) {
        announcementService.broadcast(BaseContext.getCurrentId(),
                announcementDTO.getTitle(), announcementDTO.getContent());
        return Result.success("公告已发布");
    }

    @GetMapping("/page")
    @ApiOperation("公告历史分页")
    public Result<PageResult<Announcement>> page(@RequestParam(defaultValue = "1") Integer page,
                                                 @RequestParam(defaultValue = "10") Integer pageSize) {
        return Result.success(announcementService.page(page, pageSize));
    }
}
