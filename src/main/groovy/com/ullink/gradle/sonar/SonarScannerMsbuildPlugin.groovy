package com.ullink.gradle.sonar

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.Exec
import org.sonarqube.gradle.ActionBroadcast
import org.sonarqube.gradle.SonarPropertyComputer
import org.sonarqube.gradle.SonarQubeExtension

/**
 * Uses SonarScanner for MSBuild to run the Sonarqube analysis.
 * See https://docs.sonarqube.org/display/SCAN/Analyzing+with+SonarQube+Scanner+for+MSBuild
 * Sonar properties can be configured using the same syntax as for the org.sonarqube plugin
 * in order to keep compatibility.
 */
class SonarScannerMsbuildPlugin implements Plugin<Project> {

    static final SONAR_SCANNER_GITHUB_URL = 'https://github.com/SonarSource/sonar-scanner-msbuild'
    static final SONAR_SCANNER_VERSION = '4.6.0.1930'
    static final SONAR_SCANNER_ZIP = "sonar-scanner-msbuild-${SONAR_SCANNER_VERSION}-net46.zip"
    static final SONAR_SCANNER_EXE = 'SonarScanner.MSBuild.exe'
    // keep the 'sonarqube' name for compatibility with the org.sonarqube plugin
    static final SONAR_SCANNER_TASK_NAME = 'sonarqube'
    static final MANDATORY_ARGS = [k: 'sonar.projectKey',
                                   n: 'sonar.projectName',
                                   v: 'sonar.projectVersion'
    ]
    static final IGNORED_PROPERTIES = [
        'sonar.working.directory' // is automatically set and cannot be overridden on the command line
    ]

    def actionBroadcast = new ActionBroadcast()

    @Override
    void apply(Project project) {
        project.extensions.create('sonarqube', SonarQubeExtension, actionBroadcast)

        project.apply plugin: 'de.undercouch.download'

        project.tasks.register(SONAR_SCANNER_TASK_NAME, Exec) {
            commandLine = [getSonarScannerFile(project), 'end']
        }

        // We must make sure that sonar-scanner-msbuild is initialized before started.
        // It cannot be done by tasks because we don't know if sonarqube task is declared or not
        // before the task graph is ready. That is why the initialization is done at evaluation time.
        project.gradle.taskGraph.whenReady { graph ->
            if (graph.hasTask("${project.path}:${SONAR_SCANNER_TASK_NAME}") || graph.hasTask(":${SONAR_SCANNER_TASK_NAME}")) {
                project.logger.info("${SONAR_SCANNER_TASK_NAME} task was detected. Initializing sonar-scanner-msbuild...")
                initSonarScanner(project)
            }
        }
    }

    private void initSonarScanner(Project project) {
        downloadSonarScanner(project)
        project.exec {
            commandLine = [getSonarScannerFile(project), 'begin'] + buildArgs(computeSonarProperties(project))
        }
    }

    private void downloadSonarScanner(Project project) {
        def sonarScanner = getSonarScannerFile(project)
        if (sonarScanner.exists()) {
            project.logger.info("Not downloading sonar-scanner-msbuild ${SONAR_SCANNER_VERSION} because it " +
                "already exists in cache ${getCacheDir(project).path}.")
        } else {
            def tempDir = File.createTempDir()
            tempDir.deleteOnExit()
            project.download {
                src "${SONAR_SCANNER_GITHUB_URL}/releases/download/${SONAR_SCANNER_VERSION}/${SONAR_SCANNER_ZIP}"
                dest tempDir
            }
            def scannerDir = getVersionCacheDir(project)
            scannerDir.mkdirs()
            project.copy {
                from project.zipTree(new File(tempDir, SONAR_SCANNER_ZIP))
                into scannerDir
            }
            project.logger.info("sonar-scanner-msbuild ${SONAR_SCANNER_VERSION} has been downloaded into " +
                getCacheDir(project).path)
        }
    }

    private static def getCacheDir(Project project) {
        new File(project.gradle.gradleUserHomeDir, 'caches/sonar-scanner-msbuild')
    }

    private static def getVersionCacheDir(Project project) {
        new File(getCacheDir(project), SONAR_SCANNER_VERSION)
    }

    private static def getSonarScannerFile(Project project) {
        new File(getVersionCacheDir(project), SONAR_SCANNER_EXE)
    }

    private static def buildArgs(Map<String, Object> properties) {

        def mandatoryArgs = MANDATORY_ARGS.collect { key, value ->
            "/${key}:${properties[value]}"
        }

        def otherArgs = properties.findAll { key, value ->
            !(key in MANDATORY_ARGS.values()) && !(key in IGNORED_PROPERTIES) && value != ''
        }.collect { key, value ->
            "/d:${key}=${value}"
        }

        return mandatoryArgs + otherArgs
    }

    private def computeSonarProperties(Project project) {
        def actionBradcastMap = [:]
        actionBradcastMap.put(project, actionBroadcast)
        def propertyComputer = new SonarPropertyComputer(actionBradcastMap, project)
        return propertyComputer.computeSonarProperties()
    }
}
