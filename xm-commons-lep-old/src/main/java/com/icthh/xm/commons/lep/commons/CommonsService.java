package com.icthh.xm.commons.lep.commons;

import com.icthh.xm.commons.lep.LogicExtensionPoint;
import com.icthh.xm.commons.lep.spring.LepService;
import com.icthh.xm.commons.logging.aop.IgnoreLogginAspect;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.NotImplementedException;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@LepService
@IgnoreLogginAspect
public class CommonsService {

    @LogicExtensionPoint(value = "Commons", resolver = CommonsLepResolver.class)
    public Object execute(String group, String name, Object args) {
        throw new NotImplementedException("Commons in package:" + group + " with name: Commons$$" + name + "$$around.groovy not found");
    }

}
