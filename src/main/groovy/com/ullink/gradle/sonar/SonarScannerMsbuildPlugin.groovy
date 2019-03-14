package com.ullink.gradle.sonar

import de.undercouch.gradle.tasks.download.Download
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.Exec

/**
 * Uses SonarScanner for MSBuild to run the Sonarqube analysis.
 * See https://docs.sonarqube.org/display/SCAN/Analyzing+with+SonarQube+Scanner+for+MSBuild
 * Sonar properties can be configured using the same syntax as for the org.sonarqube plugin
 * in order ro keep compatibility.
 */
class SonarScannerMsbuildPlugin implements Plugin<Project> {

    static final DEFAULT_SCANNER_VERSION = '4.6.0.1930'
    static final DEFAULT_MSBUILD_TASK_NAME = 'msbuild'
    static final File CACHE_DIR = new File(System.getProperty('user.home'), '.gradle/caches/sonar-scanner-msbuild')
    static final PROJECT_KEY_ARG = 'sonar.projectKey'
    static final MANDATORY_ARGS = [n: 'sonar.projectName', v: 'sonar.projectVersion']

    @Override
    void apply(Project project) {
        def scannerExtension = project.extensions.create('sonarqube', SonarScannerMsbuildExtension)
        project.afterEvaluate {
            def msbuild = project.tasks[scannerExtension.msbuildTaskName ?: DEFAULT_MSBUILD_TASK_NAME]
            if (msbuild == null) {
                project.logger.warn('Not setting up sonar-msbuild-scanner since no msbuild task was found. ' +
                    "Please specify the name of the msbuild task using the 'msbuildTaskName' method.")
            } else {
                def scannerVersion = scannerExtension.scannerVersion ?: DEFAULT_SCANNER_VERSION
                def scannerZipName = "sonar-scanner-msbuild-${scannerVersion}-net46.zip"
                def scannerDir = new File(CACHE_DIR, scannerVersion)
                def sonarScanner = new File(scannerDir, 'SonarScanner.MSBuild.exe')

                // Since some properties may not be evaluated at this moment, let's initialize the arguments
                // right before the init task's execution
                def configureArgsTask = project.task('configureSonarProperties') {
                    doLast {
                        project.tasks.initSonarScanner.commandLine += buildArgs(project, scannerExtension.properties.properties)
                    }
                }
                def initSonarScannerTask = project.task('initSonarScanner', type: Exec, dependsOn: configureArgsTask) {
                    commandLine = [sonarScanner, 'begin']
                }

                // keep the 'sonarqube' name for compatibility
                def sonarScannerTask = project.task('sonarqube', type: Exec) {
                    commandLine = [sonarScanner, 'end']
                }
                project.task('sonar', dependsOn: sonarScannerTask) // alias for sonarqube

                if (!sonarScanner.exists()) {
                    CACHE_DIR.mkdirs()
                    def scannerDownloadTask = project.task('downloadSonarScannerMsbuild', type: Download) {
                        src "https://github.com/SonarSource/sonar-scanner-msbuild/releases/download/${scannerVersion}/${scannerZipName}"
                        dest File.createTempDir()
                    }
                    def scannerCacheDir = new File(CACHE_DIR, scannerVersion)
                    scannerCacheDir.mkdir()
                    def unzipTask = project.task('unzipSonarScannerMsbuild', type: Copy, dependsOn: scannerDownloadTask) {
                        from project.zipTree(new File(scannerDownloadTask.dest, scannerZipName))
                        into scannerCacheDir
                    }
                    initSonarScannerTask.dependsOn unzipTask
                }
                msbuild.dependsOn initSonarScannerTask
                msbuild.finalizedBy sonarScannerTask
            }
        }
    }

    private static def buildArgs(Project project, Map<String, Object> properties) {
        def args = []

        def projectKey = properties[PROJECT_KEY_ARG] ?: project.name
        args += "/k:\"${projectKey}\""

        MANDATORY_ARGS.each { key, value ->
            def propertyValue = properties[value]
            if (propertyValue != null) {
                args += "/${key}:\"${propertyValue}\""
            }
        }

        properties.each { key, value ->
            if (!(key in (MANDATORY_ARGS.values() + PROJECT_KEY_ARG))) {
                args += "/d:${key}=${value}"
            }
        }
        return args
    }
}
