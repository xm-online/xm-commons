## xm-commons-data-spec
XM^online commons project for data specification processing implementation.

This functionality allows to process input & output data specifications and forms represented in json.

To enable specification data processing:
- declare your base specification implementing [BaseSpecification.java](src%2Fmain%2Fjava%2Fcom%2Ficthh%2Fxm%2Fcommons%2Fdomain%2FBaseSpecification.java)
```java
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({"items", "definitions", "forms"})
@Data
public class MySpec implements BaseSpecification {

    @JsonProperty("items")
    private Collection<FunctionSpec> items;

    @JsonProperty("definitions")
    private List<DefinitionSpec> definitions = null;

    @JsonProperty("forms")
    private List<FormSpec> forms = null;
}
```

- Add an implementation of [DataSpecificationService.java](src%2Fmain%2Fjava%2Fcom%2Ficthh%2Fxm%2Fcommons%2Fconfig%2FDataSpecificationService.java) to link it with processing flow
```java
@Configuration
public class MySpecConfiguration extends DataSpecificationService<MySpec> {

    public MySpecConfiguration(JsonListenerService jsonListenerService,
                               DefaultSpecProcessingService<MySpec> specProcessingService) {
        super(MySpec.class, jsonListenerService, specProcessingService);
    }

    @Override
    public String specKey() { // define the base specification key
        return "myspec";
    }

    @Override
    public String folder() { // define the folder there your base specification files located
        return "appName/myspec";
    }
}
```
