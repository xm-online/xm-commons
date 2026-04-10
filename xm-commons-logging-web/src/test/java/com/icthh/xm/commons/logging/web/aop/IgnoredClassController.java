package com.icthh.xm.commons.logging.web.aop;

import com.icthh.xm.commons.logging.aop.IgnoreLogginAspect;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@IgnoreLogginAspect
@RestController
@RequestMapping("/ignored-class")
class IgnoredClassController {

    @GetMapping
    public String endpoint() {
        return "ignored";
    }
}
