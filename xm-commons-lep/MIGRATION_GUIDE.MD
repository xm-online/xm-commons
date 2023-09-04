## Migration to new version of LEP
### Replace commons and add groovy commons
TDB example
### Migrate resolvers

_Simple example:_
Old version:
```java
@Component
public class ChangeStateTransitionLepKeyResolver extends AppendLepKeyResolver {

    @Override
    protected String[] getAppendSegments(SeparatorSegmentedLepKey baseKey,
                                         LepMethod method,
                                         LepManagerService managerService) {
        String translatedXmEntityTypeKey = translateToLepConvention(getRequiredStrParam(method, "xmEntityTypeKey"));
        String translatedPrevStateKey = translateToLepConvention(getRequiredStrParam(method, "prevStateKey"));
        String translatedNextStateKey = translateToLepConvention(getRequiredStrParam(method, "nextStateKey"));

        return new String[] {
            translatedXmEntityTypeKey,
            translatedPrevStateKey,
            translatedNextStateKey
        };
    }

}
```
New version
```java
@Component
public class CommonsLepResolver implements LepKeyResolver {

    @Override
    public List<String> segments(LepMethod method) {
        return List.of(
                method.getParameter("xmEntityTypeKey", String.class),
                method.getParameter("prevStateKey", String.class),
                method.getParameter("nextStateKey", String.class)
        );
    }
}
```

_Advanced example:_

Old version
```java
@Component
public class CommonsLepResolver extends SeparatorSegmentedLepKeyResolver {
    @Override
    protected LepKey resolveKey(SeparatorSegmentedLepKey inBaseKey, LepMethod method, LepManagerService managerService) {
        String group = getRequiredParam(method, "group", String.class) + ".Commons";
        String name = getRequiredParam(method, "name", String.class);
        SeparatorSegmentedLepKey baseKey = new SeparatorSegmentedLepKey(group, XmLepConstants.EXTENSION_KEY_SEPARATOR, XmLepConstants.EXTENSION_KEY_GROUP_MODE);
        GroupMode groupMode = new GroupMode.Builder().prefixAndIdIncludeGroup(baseKey.getGroupSegmentsSize()).build();
        return baseKey.append(name, groupMode);
    }
}
```
New version
```java
@Component
public class CommonsLepResolver implements LepKeyResolver {

    @Override
    public String group(LepMethod method) {
        return method.getParameter("group", String.class);
    }

    @Override
    public List<String> segments(LepMethod method) {
        return List.of(method.getParameter("name", String.class));
    }
}
```

### Migrate interfaces annotated `@LepService` with methods annotated `@LogicExtensionPoint` to classes.

### Replace init destroy lep context
Old version:
```
...
    lepManager.beginThreadContext(threadContext -> {
        threadContext.setValue(THREAD_CONTEXT_KEY_TENANT_CONTEXT, tenantContextHolder.getContext());
        threadContext.setValue(THREAD_CONTEXT_KEY_AUTH_CONTEXT, authContextHolder.getContext());
    });

    // run lep methods

    lepManager.endThreadContext();
...
```

New version:

Use `LepManagementService`
```
 try (var context = lepManagementService.beginThreadContext()) {
      // run lep methods
 }
```

### Remove usage of CoreContextsHolder in LEPs. For create new thread use LepThreadHelper.
```groovy
lepContext.thread.runInThread(executor) {
    // you task
}
```
#### For migration period class CoreContextsHolder supported, but deprecated for removal.


### For create lepContext you need to extend BaseLepContext and implement LepContextFactory
#### Microservice MUST have class with name LepContext

TBD example

### To add field to lepContext globally (in commons or in libs) implement LepAdditionalContext. Pls use this ONLY IN LIBS OR XM-COMMONS.
#### See example in OutboxTransportService or TraceService
Example:
```
@Override
public String additionalContextKey() {
    return FIELD_NAME; // name of additional field
}

@Override
public TraceService additionalContextValue() {
    return this; // new lepContext field value
}

@Override
public Class<? extends LepAdditionalContextField> fieldAccessorInterface() {
    return TraceServiceField.class; // interface that help to typesafe access field from lep
}
```
#### LepAdditionalContextField - it`s interface to keep lepContext type safe
Example:
```java
public interface TraceServiceField extends LepAdditionalContextField {
    String FIELD_NAME = "traceService";
    default TraceService getLepServices() { // default it's important!!!
        return (TraceService)get(FIELD_NAME);
    }
}
```
When in microservice you extend BaseLepContext you LepContext can implement this interface.

Example:
```java
public class LepContext extends BaseLepContext implements TraceServiceField {
    public MyCoolService myCoolService;
    // no need to implement a method from LepServiceFactoryField
}
```

And after that field traceService will be present in lepContext.
```groovy
lepContext.traceService // groovy will call method getLepServices() for you
```

### Update LepSpringConfiguration
Replace extends from WebLepSpringConfiguration to extends from GroovyLepEngineConfiguration,
Or just remove lep configuration.

Replace getTenantScriptStorageType() to TenantScriptStorageTypeProvider

### Remove code that add _LepInterceptor_ in you configuration.
LepInterceptor will be added in LepInterceptorConfiguration

### Simple test migration

To make test migration with minimal changes:
1 - Extends **TEST** configuration from `GroovyLepEngineConfiguration`
2 - Add next beans:

```groovy
    @Override
    public LepUpdateMode lepUpdateMode() {
        return LepUpdateMode.SYNCHRONOUS;
    }

    @Bean
    public LoggingConfigService LoggingConfigService() {
        return new LoggingConfigServiceStub();
    }
```


#### Specify tenant before beginThreadContext
```groovy
TenantContextUtils.setTenant(tenantContextHolder, "TEST");
```

### If you write xm-commons or lib add to dependency ONLY `implementation project(':xm-commons-lep')`,  if you need groovy lep in test add test deps `testImplementation project(':xm-commons-lep-groovy')`
This will allow to choose lep engine in target microservices.

## TODO _change spring lep configuration_

