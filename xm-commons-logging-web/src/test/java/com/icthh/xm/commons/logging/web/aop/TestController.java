package com.icthh.xm.commons.logging.web.aop;

import com.icthh.xm.commons.logging.aop.IgnoreLogginAspect;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/test")
class TestController {

    @GetMapping("/normal")
    public String normalEndpoint() {
        return "ok";
    }

    @IgnoreLogginAspect
    @GetMapping("/ignored")
    public String ignoredMethod() {
        return "ignored";
    }
}
