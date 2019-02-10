package com.icthh.xm.commons.permission.inspector.kafka;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.icthh.xm.commons.messaging.event.system.SystemEvent;
import com.icthh.xm.commons.permission.domain.Privilege;
import com.icthh.xm.commons.permission.domain.mapper.PrivilegeMapper;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * The {@link PrivilegeEventProducerUnitTest} class.
 */
// https://github.com/spring-projects/spring-integration-kafka/blob/master/src/test/java/org/springframework/integration/kafka/outbound/KafkaProducerMessageHandlerTests.java
public class PrivilegeEventProducerUnitTest {

    private static String topicSystemQueue = "testSystemQueue";

    @Mock
    private KafkaTemplate<String, String> template;

    private PrivilegeEventProducer producer;

    @Before
    public void beforeEachTest() {
        MockitoAnnotations.initMocks(this);

        producer = new PrivilegeEventProducer(template);
        ReflectionTestUtils.setField(producer, "appName", "test-ms");
        ReflectionTestUtils.setField(producer, "topicName", topicSystemQueue);
    }

    @Test
    public void successEventSerialization() {
        Privilege privilege = new Privilege();
        privilege.setKey("READ");
        privilege.setMsName("MS1");

        producer.sendEvent("123", Collections.singleton(privilege));

        verify(template, times(1))
            .send(eq(topicSystemQueue),
                  sysEventPrivilegeEq("READ", "MS1"));
    }

    /**
     * Convenience factory method for using the custom ValueObject matcher.
     */
    private static String sysEventPrivilegeEq(String privilegeKey, String appName) {
        return argThat(new SysEventPrivilegeMatcher(privilegeKey, appName));
    }

    /**
     * Custom matcher for verifying actual and expected ValueObjects match.
     */
    private static class SysEventPrivilegeMatcher implements ArgumentMatcher<String> {

        private final ObjectMapper jsonMapper = new ObjectMapper()
            .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
            .setSerializationInclusion(JsonInclude.Include.NON_NULL)
            .registerModule(new JavaTimeModule());

        private final String privilegeKey;
        private final String appName;

        SysEventPrivilegeMatcher(String privilegeKey, String appName) {
            this.privilegeKey = privilegeKey;
            this.appName = appName;
        }

        private SystemEvent toEvent(String str) {
            try {
                return jsonMapper.readValue(str, SystemEvent.class);
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        }

        @Override
        public boolean matches(String actualObj) {
            if (actualObj == null) {
                return false;
            }

            SystemEvent actual = toEvent(actualObj);

            Privilege actualPrivilege = getFirstPrivilege(actual);

            return Objects.equals(privilegeKey, actualPrivilege.getKey())
                && Objects.equals(appName, actualPrivilege.getMsName());
        }

        private static Privilege getFirstPrivilege(SystemEvent actual) {
            String privilegesStr = (String) actual.getDataMap().get("privileges");
            Map<String, Set<Privilege>> pkeyToPrivileges = PrivilegeMapper.ymlToPrivileges(privilegesStr);
            Set<Privilege> privileges = pkeyToPrivileges.values().iterator().next();
            return privileges.iterator().next();
        }

        @Override
        public String toString() {
            return privilegeKey + "/" + appName;
        }

    }

}
