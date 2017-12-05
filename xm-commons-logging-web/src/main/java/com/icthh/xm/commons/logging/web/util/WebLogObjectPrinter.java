package com.icthh.xm.commons.logging.web.util;

import com.icthh.xm.commons.logging.LoggingAspectConfig;
import com.icthh.xm.commons.logging.util.LogObjectPrinter;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.springframework.http.ResponseEntity;

/**
 * The {@link WebLogObjectPrinter} class.
 */
@UtilityClass
@Slf4j
public final class WebLogObjectPrinter {

    public static RestResp printRestResult(final JoinPoint joinPoint, final Object res) {
        return printRestResult(joinPoint, res, LoggingAspectConfig.DEFAULT_RESULT_DETAILS);
    }

    public static RestResp printRestResult(final JoinPoint joinPoint, final Object res, final boolean printBody) {

        if (res == null) {
            return new RestResp("OK", "null", printBody);
        }

        Class<?> respClass = res.getClass();
        String status;
        Object bodyToPrint;

        if (ResponseEntity.class.isAssignableFrom(respClass)) {
            ResponseEntity<?> respEn = ResponseEntity.class.cast(res);

            status = String.valueOf(respEn.getStatusCode());

            Object body = respEn.getBody();
            bodyToPrint = LogObjectPrinter.printResult(joinPoint, body, printBody);

        } else {
            status = "OK";
            bodyToPrint = LogObjectPrinter.printResult(joinPoint, res, printBody);
        }
        return new RestResp(status, bodyToPrint, printBody);

    }

    @AllArgsConstructor
    @Getter
    public static class RestResp {
        private String status;
        private Object bodyToPrint;
        private boolean printBody;

        @Override
        public String toString() {
            return "status=" + status + ", body=" + bodyToPrint;
        }
    }

}
