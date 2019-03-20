package com.ullink.gradle.sonar

class SonarScannerMsbuildExtension {
    SonarProperties properties = new SonarProperties()

    // imitate the syntax of org.sonarqube plugin
    void properties(@DelegatesTo(value = SonarProperties, strategy = Closure.DELEGATE_FIRST) Closure sonarProperties) {
        sonarProperties.delegate = properties
        sonarProperties.resolveStrategy = Closure.DELEGATE_FIRST
        sonarProperties.call()
    }

    class SonarProperties {
        Map<String, Object> properties = [:]

        void property(String key, Object value) {
            properties[key] = value
        }
    }

}
