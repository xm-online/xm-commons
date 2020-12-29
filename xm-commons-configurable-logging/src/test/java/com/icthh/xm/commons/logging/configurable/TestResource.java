package com.icthh.xm.commons.logging.configurable;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

/**
 *
 */

@RestController
@RequestMapping("/api")
public class TestResource {

    @GetMapping("/first")
    public @ResponseBody String testMethodFirst(String firstArg, String secondArg) {
        return "result";
    }

    @GetMapping("/second")
    public @ResponseBody String testMethodSecond(String firstArg, String secondArg) {
        return "result";
    }

}
