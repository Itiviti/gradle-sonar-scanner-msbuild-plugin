/*************************************************************************
 * ULLINK CONFIDENTIAL INFORMATION
 * _______________________________
 *
 * All Rights Reserved.
 *
 * NOTICE: This file and its content are the property of Ullink. The
 * information included has been classified as Confidential and may
 * not be copied, modified, distributed, or otherwise disseminated, in
 * whole or part, without the express written permission of Ullink.
 ************************************************************************/
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
