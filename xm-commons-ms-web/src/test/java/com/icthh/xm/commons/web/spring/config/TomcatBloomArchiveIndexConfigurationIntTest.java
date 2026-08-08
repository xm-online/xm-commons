package com.icthh.xm.commons.web.spring.config;

import org.apache.catalina.Container;
import org.apache.catalina.Context;
import org.springframework.boot.tomcat.TomcatWebServer;
import org.springframework.boot.tomcat.servlet.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class TomcatBloomArchiveIndexConfigurationIntTest {

    @Test
    public void customizerSetsBloomArchiveIndexStrategyOnStartedContext() {
        TomcatServletWebServerFactory factory = new TomcatServletWebServerFactory(0);
        new TomcatBloomArchiveIndexConfiguration().tomcatBloomArchiveIndexCustomizer().customize(factory);

        WebServer webServer = factory.getWebServer();
        try {
            Context context = findContext((TomcatWebServer) webServer);
            assertNotNull(context.getResources(), "context resources");
            assertEquals("BLOOM", context.getResources().getArchiveIndexStrategy());
        } finally {
            webServer.stop();
        }
    }

    private Context findContext(TomcatWebServer webServer) {
        for (Container child : webServer.getTomcat().getHost().findChildren()) {
            if (child instanceof Context context) {
                return context;
            }
        }
        throw new AssertionError("no tomcat context found");
    }
}
