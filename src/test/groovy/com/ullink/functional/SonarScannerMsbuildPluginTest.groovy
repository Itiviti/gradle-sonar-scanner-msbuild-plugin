package com.ullink.functional

import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.UnexpectedBuildFailure
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification

class SonarScannerMsbuildPluginTest extends Specification {
    @Rule TemporaryFolder testProjectDir = new TemporaryFolder()
    File buildFile

    def setup() {
        buildFile = testProjectDir.newFile('build.gradle')
        buildFile << """
            plugins {
                id 'com.ullink.sonar-scanner-msbuild'
                id 'de.undercouch.download'
            }
        """
    }

    def "sonarqube task fails when mandatory parameters are not passed"() {
        when:
            def result = GradleRunner.create()
                    .withDebug(true)
                    .withProjectDir(testProjectDir.root)
                    .withArguments('sonarqube', '-is')
                    .withPluginClasspath()
                    .build()

        then:
            UnexpectedBuildFailure exception = thrown()
            exception.message.contains("SonarScanner for MSBuild 4.6")
            exception.message.contains("Failed to request and parse 'http://localhost:9000/api/server/version': Unable to connect to the remote server")
            exception.message.contains("Pre-processing failed. Exit code: 1")
    }
}
