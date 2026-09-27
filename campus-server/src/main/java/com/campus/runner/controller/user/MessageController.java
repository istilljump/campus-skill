package com.campus.runner.controller.user;

import com.campus.runner.context.BaseContext;
import com.campus.runner.entity.Message;
import com.campus.runner.result.Result;
import com.campus.runner.service.MessageService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 用户端-消息中心
 */
@RestController("userMessageController")
@Slf4j
@Api(tags = "用户端-消息中心接口")
@RequestMapping("/user/message")
public class MessageController {

    @Autowired
    private MessageService messageService;

    @GetMapping("/list")
    @ApiOperation("最新消息列表（默认20条）")
    public Result<List<Message>> list(@RequestParam(defaultValue = "20") Integer limit) {
        return Result.success(messageService.listLatest(BaseContext.getCurrentId(), limit));
    }

    @GetMapping("/unread")
    @ApiOperation("未读消息数量")
    public Result<Integer> unread() {
        return Result.success(messageService.countUnread(BaseContext.getCurrentId()));
    }

    @PutMapping("/readAll")
    @ApiOperation("全部标记已读")
    public Result<String> readAll() {
        messageService.markAllRead(BaseContext.getCurrentId());
        return Result.success();
    }
}
