package com.zegao.zeaiagent.Controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("ai")
public class HealthController {
    @RequestMapping("health")
    public String health() {
        return "OK";
    }
}
