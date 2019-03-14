package com.ullink.gradle.sonar

class SonarScannerMsbuildExtension {
    String scannerVersion
    String msbuildTaskName
    SonarProperties properties = new SonarProperties()

    void scannerVerion(String scannerVersion) {
        this.scannerVersion = scannerVersion
    }

    void msbuildTaskName(String name) {
        this.msbuildTaskName = name
    }

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
