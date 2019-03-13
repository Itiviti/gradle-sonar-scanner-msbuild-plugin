# gradle-sonar-scanner-msbuild-plugin

A gradle plugin for running Sonarqube on .NET projects, using [SonarQube Scanner for MSbuild](https://docs.sonarqube.org/display/SCAN/Analyzing+with+SonarQube+Scanner+for+MSBuild).

Follows the same configuration syntax as the [Sonar Scanner for Gradle plugin](https://github.com/SonarSource/sonar-scanner-gradle).

### Example usage

```
apply plugin: 'com.ullink.sonar-scanner-msbuild'

sonarqube {
    properties {
        property 'sonar.projectKey', 'my.project.key'
        property 'sonar.otherProperty', 'value'
    }
}
```

Note that when `sonar.projectKey` is not set, it will use the project name as project key.
