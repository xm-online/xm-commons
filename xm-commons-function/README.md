## xm-commons-function
XM^online commons project for API function execution implementation.

This functionality allows you to configure and execute lep functions with api.

You can configure a lep functions in `/config/teants/TENANT_NAME/appName/functions.yml` | `/config/teants/TENANT_NAME/appName/functions/*.yml` files.
See the [FunctionApiSpecs.java](src%2Fmain%2Fjava%2Fcom%2Ficthh%2Fxm%2Fcommons%2Fdomain%2Fspec%2FFunctionApiSpecs.java) for structure.
```yaml
---
validateFunctionInput: true
functions:
    - key: store/STORE-INFO
      anonymous: true
      txType: READ_ONLY
      inputSpec: ...
      outputSpec: ...
    - key: store/GET-EMPLOYEES-AGE
      httpMethods:
          - "POST"
      inputForm: ...
      outputForm: ...
definitions:
    - key: userInfo
      value: ...
forms:
    - key: datesForm
      ref: ...

```
Create a LEP function according to the path declared in yaml configuration and call your function 
```
GET https://.../api/functions/store/STORE-INFO?name=testStore
Content-Type: application/json
authorization: Bearer ...
```

Check these resources for available REST calls:

[FunctionApiDocsResource.java](src%2Fmain%2Fjava%2Fcom%2Ficthh%2Fxm%2Fcommons%2Fweb%2Frest%2FFunctionApiDocsResource.java)

[FunctionMvcResource.java](src%2Fmain%2Fjava%2Fcom%2Ficthh%2Fxm%2Fcommons%2Fweb%2Frest%2FFunctionMvcResource.java)

[FunctionResource.java](src%2Fmain%2Fjava%2Fcom%2Ficthh%2Fxm%2Fcommons%2Fweb%2Frest%2FFunctionResource.java)

[FunctionUploadResource.java](src%2Fmain%2Fjava%2Fcom%2Ficthh%2Fxm%2Fcommons%2Fweb%2Frest%2FFunctionUploadResource.java)
