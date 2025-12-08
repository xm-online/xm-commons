## xm-commons-function-export
XM^online commons project for API file export function execution implementation.

It supports exporting data in the following file formats:
- csv
- xlsx

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
          - "GET"
      inputForm: ...
      outputForm: ...
definitions:
    - key: userInfo
      value: ...
forms:
    - key: datesForm
      ref: ...

```
Each function configuration must reference a LEP function. 

### LEP Execution Details

1. The LEP function should return either:
- Page\<T\> — if data is paged
- List\<T\> — if all data is returned at once

2. During LEP execution, the system automatically injects the following functionInput parameters:
- page => Page index (starts from 0)
- size => Page size (default: 1000). Can be overridden with property: `application.function.export.defaultPageSize`

3. When LEP returns a Page: the function will be called iteratively to collect all pages data. All results are merged and exported.
4. When LEP returns a List: the function will be called once and wrapped into a pageable structure internally. The resulting data is exported as a single chunk.
