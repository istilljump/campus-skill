package com.campus.runner.controller.runner;

import com.campus.runner.context.BaseContext;
import com.campus.runner.entity.Message;
import com.campus.runner.result.Result;
import com.campus.runner.service.MessageService;
import com.campus.runner.service.RunnerService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 跑腿员端-消息中心（收件箱与用户端共用，按用户ID存储）
 */
@RestController("runnerMessageController")
@Slf4j
@Api(tags = "跑腿员端-消息中心接口")
@RequestMapping("/runner/message")
public class MessageController {

    @Autowired
    private MessageService messageService;

    @Autowired
    private RunnerService runnerService;

    @GetMapping("/list")
    @ApiOperation("最新消息列表（默认20条）")
    public Result<List<Message>> list(@RequestParam(defaultValue = "20") Integer limit) {
        Long userId = runnerService.resolveUserId(BaseContext.getCurrentId());
        return Result.success(messageService.listLatest(userId, limit));
    }

    @GetMapping("/unread")
    @ApiOperation("未读消息数量")
    public Result<Integer> unread() {
        Long userId = runnerService.resolveUserId(BaseContext.getCurrentId());
        return Result.success(messageService.countUnread(userId));
    }

    @PutMapping("/readAll")
    @ApiOperation("全部标记已读")
    public Result<String> readAll() {
        Long userId = runnerService.resolveUserId(BaseContext.getCurrentId());
        messageService.markAllRead(userId);
        return Result.success();
    }
}
