# xm-commons

XM^online 2 - Common Utils and Endpoints

## Use commons for local development in other MS

1. Increase xm-commons version in gradle/version.gradle
2. Deploy to local maven repo with command:
    ```shell script
    ./gradlew clean install
    ```
3. Change xm-commons version in target MS in gradle.properties (for example entity)
4. Start target MS.
