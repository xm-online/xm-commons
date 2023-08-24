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

### To add field to lepContext globally (in commons or in libs) implement LepAdditionalContext
#### See example in LepServiceFactoryImpl or OutboxTransportService or TraceService
Example:
```
@Override
public String additionalContextKey() {
    return FIELD_NAME; // name of additional field
}

@Override
public LepServiceFactory additionalContextValue() {
    return this; // new lepContext field value
}

@Override
public Class<? extends LepAdditionalContextField> fieldAccessorInterface() {
    return LepServiceFactoryField.class; // interface that help to typesafe access field from lep
}
```
#### LepAdditionalContextField - it`s interface to keep lepContext type safe
Example:
```java
public interface LepServiceFactoryField extends LepAdditionalContextField {
    String FIELD_NAME = "lepServices";
    default LepServiceFactory getLepServices() {
        return (LepServiceFactory)get(FIELD_NAME);
    }
}
```
When in microservice you extend BaseLepContext you LepContext can implement this interface.

Example:
```java
public class LepContext extends BaseLepContext implements LepServiceFactoryField {
    public MyCoolService myCoolService;
    // no need to implement a method from LepServiceFactoryField
}
```

And after that field lepServices will be present in lepContext.
```groovy
lepContext.lepServices // groovy will call method getLepServices() for you
```

### Update LepSpringConfiguration
Replace WebLepSpringConfiguration to LepSpringConfiguration

Replace getTenantScriptStorageType() to TenantScriptStorageTypeProvider

### Remove code that add _LepInterceptor_.
LepInterceptor will be added in LepInterceptorConfiguration

### In Tests after XmLepScriptConfigServerResourceLoader.onRefresh you need to call XmLepScriptConfigServerResourceLoader.refreshFinished

## TODO _change spring lep configuration_

