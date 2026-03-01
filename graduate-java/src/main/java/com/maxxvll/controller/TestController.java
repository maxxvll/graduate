package com.maxxvll.controller;

import com.maxxvll.common.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 测试控制器，用于验证内容协商配置是否正常工作
 */
@RestController
@RequestMapping("/test")
public class TestController {

    @GetMapping("/ping")
    public Result<String> ping() {
        return Result.success("pong");
    }

    @GetMapping("/json")
    public Result<Object> testJson() {
        return Result.success("JSON响应测试成功");
    }
}