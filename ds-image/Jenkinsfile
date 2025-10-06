pipeline {
    agent {
        label 'DS agent'
    }

    options {
        disableConcurrentBuilds()
        timeout(time: 10, unit: 'MINUTES')
        buildDiscarder(logRotator(numToKeepStr: '5'))
    }

    environment {
        MVN_SETTINGS = '/etc/m2/settings.xml' //This should be changed in Jenkins config for the DS agent
        PROJECT = 'ds-image'
    }

    triggers {
        // This triggers the pipeline when a PR is opened or updated or so I hope
        githubPush()
    }

    parameters {
        string(name: 'ORIGINAL_BRANCH', defaultValue: "${env.BRANCH_NAME}", description: 'Branch of first job to run, will also be PI_ID for a PR')
        string(name: 'ORIGINAL_JOB', defaultValue: "ds-image", description: 'What job was the first to build?')
    }

    stages {
        stage('Echo Environment Variables') {
            steps {
                echo "PROJECT: ${env.PROJECT}"
                echo "ORIGINAL_BRANCH: ${params.ORIGINAL_BRANCH}"
                echo "ORIGINAL_JOB: ${params.ORIGINAL_JOB}"
            }
        }

        stage('Change version if part of PR') {
            when {
                expression {
                    params.ORIGINAL_BRANCH ==~ "PR-[0-9]+"
                }
            }
            steps {
                script {
                    sh "mvn -s ${env.MVN_SETTINGS} versions:set -DnewVersion=${params.ORIGINAL_BRANCH}-${params.ORIGINAL_JOB}-${env.PROJECT}-SNAPSHOT"
                    echo "Changing MVN version to: ${params.ORIGINAL_BRANCH}-${params.ORIGINAL_JOB}-${env.PROJECT}-SNAPSHOT"
                }
            }
        }

        stage('Change dependencies') {
            when {
                expression {
                    params.ORIGINAL_BRANCH ==~ "PR-[0-9]+"
                }
            }
            steps {
                script {
                    switch (params.ORIGINAL_JOB) {
                        case ['ds-parent']:
                            sh "mvn -s ${env.MVN_SETTINGS} versions:use-dep-version -Dincludes=dk.kb.dsparent:* -DdepVersion=${params.ORIGINAL_BRANCH}-${params.ORIGINAL_JOB}-ds-parent-SNAPSHOT -DforceVersion=true"
                            sh "mvn -s ${env.MVN_SETTINGS} versions:use-dep-version -Dincludes=dk.kb.dsshared:* -DdepVersion=${params.ORIGINAL_BRANCH}-${params.ORIGINAL_JOB}-ds-shared-SNAPSHOT -DforceVersion=true"
                            sh "mvn -s ${env.MVN_SETTINGS} versions:use-dep-version -Dincludes=dk.kb.license:* -DdepVersion=${params.ORIGINAL_BRANCH}-${params.ORIGINAL_JOB}-ds-license-SNAPSHOT -DforceVersion=true"
                            sh "mvn -s ${env.MVN_SETTINGS} versions:use-dep-version -Dincludes=dk.kb.kaltura:* -DdepVersion=${params.ORIGINAL_BRANCH}-${params.ORIGINAL_JOB}-ds-kaltura-SNAPSHOT -DforceVersion=true"

                            echo "Changing MVN dependency ds-parent to: ${params.ORIGINAL_BRANCH}-${params.ORIGINAL_JOB}-ds-parent-SNAPSHOT"
                            echo "Changing MVN dependency ds-shared to: ${params.ORIGINAL_BRANCH}-${params.ORIGINAL_JOB}-ds-shared-SNAPSHOT"
                            echo "Changing MVN dependency ds-license to: ${params.ORIGINAL_BRANCH}-${params.ORIGINAL_JOB}-ds-license-SNAPSHOT"
                            echo "Changing MVN dependency ds-kaltura to: ${params.ORIGINAL_BRANCH}-${params.ORIGINAL_JOB}-ds-kaltura-SNAPSHOT"
                            break
                        case ['ds-shared']:
                            sh "mvn -s ${env.MVN_SETTINGS} versions:use-dep-version -Dincludes=dk.kb.dsshared:* -DdepVersion=${params.ORIGINAL_BRANCH}-${params.ORIGINAL_JOB}-ds-shared-SNAPSHOT -DforceVersion=true"
                            sh "mvn -s ${env.MVN_SETTINGS} versions:use-dep-version -Dincludes=dk.kb.license:* -DdepVersion=${params.ORIGINAL_BRANCH}-${params.ORIGINAL_JOB}-ds-license-SNAPSHOT -DforceVersion=true"
                            sh "mvn -s ${env.MVN_SETTINGS} versions:use-dep-version -Dincludes=dk.kb.kaltura:* -DdepVersion=${params.ORIGINAL_BRANCH}-${params.ORIGINAL_JOB}-ds-kaltura-SNAPSHOT -DforceVersion=true"

                            echo "Changing MVN dependency ds-shared to: ${params.ORIGINAL_BRANCH}-${params.ORIGINAL_JOB}-ds-shared-SNAPSHOT"
                            echo "Changing MVN dependency ds-license to: ${params.ORIGINAL_BRANCH}-${params.ORIGINAL_JOB}-ds-license-SNAPSHOT"
                            echo "Changing MVN dependency ds-kaltura to: ${params.ORIGINAL_BRANCH}-${params.ORIGINAL_JOB}-ds-kaltura-SNAPSHOT"
                            break
                        case ['ds-storage', 'ds-license']:
                            sh "mvn -s ${env.MVN_SETTINGS} versions:use-dep-version -Dincludes=dk.kb.license:* -DdepVersion=${params.ORIGINAL_BRANCH}-${params.ORIGINAL_JOB}-ds-license-SNAPSHOT -DforceVersion=true"
                            sh "mvn -s ${env.MVN_SETTINGS} versions:use-dep-version -Dincludes=dk.kb.kaltura:* -DdepVersion=${params.ORIGINAL_BRANCH}-${params.ORIGINAL_JOB}-ds-kaltura-SNAPSHOT -DforceVersion=true"

                            echo "Changing MVN dependency ds-license to: ${params.ORIGINAL_BRANCH}-${params.ORIGINAL_JOB}-ds-license-SNAPSHOT"
                            echo "Changing MVN dependency ds-kaltura to: ${params.ORIGINAL_BRANCH}-${params.ORIGINAL_JOB}-ds-kaltura-SNAPSHOT"
                            break
                        case ['ds-present', 'ds-kaltura']:
                            sh "mvn -s ${env.MVN_SETTINGS} versions:use-dep-version -Dincludes=dk.kb.kaltura:* -DdepVersion=${params.ORIGINAL_BRANCH}-${params.ORIGINAL_JOB}-ds-kaltura-SNAPSHOT -DforceVersion=true"

                            echo "Changing MVN dependency ds-kaltura to: ${params.ORIGINAL_BRANCH}-${params.ORIGINAL_JOB}-ds-kaltura-SNAPSHOT"
                            break
                    }
                }
            }
        }

        stage('Build') {
            steps {
                withMaven(options: [artifactsPublisher(fingerprintFilesDisabled: true, archiveFilesDisabled: true)], traceability: true) {
                    // Execute Maven build
                    sh "mvn -s ${env.MVN_SETTINGS} clean package"
                }
            }
        }

        stage('Analyze build results') {
            steps {
                recordIssues(
                    aggregatingResults: true,
                    tools: [
                        java(),
                        javaDoc(),
                        mavenConsole(),
                        taskScanner(
                            highTags: 'FIXME',
                            normalTags: 'TODO',
                            includePattern: '**/*.java',
                            excludePattern: 'target/**/*'
                        )
                    ]
                )
            }
        }

        stage('Push to Nexus') {
            when {
                // Check if Build was successful
                expression {
                    currentBuild.currentResult == "SUCCESS" && params.ORIGINAL_BRANCH ==~ "master|release_v[0-9]+|PR-[0-9]+"
                }
            }
            steps {
                withMaven(options: [artifactsPublisher(fingerprintFilesDisabled: true, archiveFilesDisabled: true)], traceability: true) {
                    sh "mvn -s ${env.MVN_SETTINGS} clean deploy -DskipTests=true"
                }
            }
        }
    }
}
