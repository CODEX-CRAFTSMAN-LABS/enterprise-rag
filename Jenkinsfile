// DEPRECATED — use environment-specific pipelines:
//   jenkins/Jenkinsfile.dev  → develop branch → enterprise-rag-dev
//   jenkins/Jenkinsfile.prod → main branch     → enterprise-rag-prod
//
// Enterprise RAG — Jenkins declarative pipeline (generic / local)
// Stages: Checkout → Build/Test → Coverage → SonarQube → Checkmarx SAST → Docker → Archive
//
// Required Jenkins credentials (Manage Credentials):
//   sonar-token-id     — Secret text (SonarQube token)
//   checkmarx-credentials — Username/password for Checkmarx server (optional)
//
// Optional environment (job or folder level):
//   SONAR_HOST_URL          e.g. https://sonar.company.com
//   CHECKMARX_SERVER_URL    e.g. https://checkmarx.company.com
//   CHECKMARX_PROJECT_NAME  enterprise-rag
//   CHECKMARX_TEAM          /CxServer/Enterprise
//   ENABLE_CHECKMARX        true|false (default false)

pipeline {
    agent any

    options {
        buildDiscarder(logRotator(numToKeepStr: '20'))
        timestamps()
        timeout(time: 45, unit: 'MINUTES')
        disableConcurrentBuilds()
    }

    environment {
        JAVA_HOME = tool name: 'jdk-17', type: 'jdk'
        PATH = "${JAVA_HOME}/bin:${env.PATH}"
        GRADLE_OPTS = '-Dorg.gradle.daemon=false -Dorg.gradle.parallel=true'
        COVERAGE_MINIMUM = '0.75'
        ENABLE_CHECKMARX = "${env.ENABLE_CHECKMARX ?: 'false'}"
        ENABLE_SONAR = "${env.ENABLE_SONAR ?: 'true'}"
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
                sh 'chmod +x gradlew'
            }
        }

        stage('Build & Unit Tests') {
            steps {
                sh './gradlew clean build jacocoAggregatedReport --no-daemon'
            }
            post {
                always {
                    junit allowEmptyResults: true, testResults: '**/build/test-results/test/*.xml'
                }
            }
        }

        stage('Coverage Gate') {
            steps {
                sh "./gradlew coverageCheck -PcoverageMinimum=${COVERAGE_MINIMUM} --no-daemon"
            }
            post {
                always {
                    publishHTML(target: [
                        allowMissing: true,
                        alwaysLinkToLastBuild: true,
                        keepAll: true,
                        reportDir: 'build/reports/jacoco/aggregated/html',
                        reportFiles: 'index.html',
                        reportName: 'JaCoCo Coverage'
                    ])
                    archiveArtifacts artifacts: 'build/reports/jacoco/aggregated/jacocoTestReport.xml', fingerprint: true, allowEmptyArchive: true
                }
            }
        }

        stage('SonarQube') {
            when {
                expression { return env.ENABLE_SONAR == 'true' }
            }
            steps {
                withSonarQubeEnv('SonarQube') {
                    sh '''
                        if command -v sonar-scanner >/dev/null 2>&1; then
                          sonar-scanner -Dproject.settings=sonar-project.properties
                        else
                          echo "WARN: sonar-scanner not on agent PATH. Install SonarScanner or use SonarQube Jenkins plugin with automatic scanner."
                          echo "Skipping Sonar scan — configure SONAR_HOST_URL and sonar-scanner on the agent."
                        fi
                    '''
                }
            }
        }

        stage('Quality Gate') {
            when {
                expression { return env.ENABLE_SONAR == 'true' }
            }
            steps {
                timeout(time: 5, unit: 'MINUTES') {
                    waitForQualityGate abortPipeline: true
                }
            }
        }

        stage('Checkmarx SAST') {
            when {
                expression { return env.ENABLE_CHECKMARX == 'true' }
            }
            steps {
                script {
                    // Requires Checkmarx Jenkins plugin (CxScanBuilder) on the controller
                    // See docs/operations/cicd-and-quality.md for setup
                    echo 'Running Checkmarx SAST scan...'
                    step([
                        $class: 'CxScanBuilder',
                        preset: '36',
                        projectName: "${env.CHECKMARX_PROJECT_NAME ?: 'enterprise-rag'}",
                        teamPath: "${env.CHECKMARX_TEAM ?: '/CxServer'}",
                        comment: "Jenkins build ${env.BUILD_NUMBER}",
                        vulnerabilityThresholdResult: 'FAILURE',
                        highThreshold: 0,
                        mediumThreshold: 5,
                        lowThreshold: 10,
                        incremental: true,
                        excludeFolders: '.git,build,.gradle,docs,infra,k8s',
                        fullScanbranch: 'main'
                    ])
                }
            }
        }

        stage('Docker Build') {
            when {
                anyOf {
                    branch 'main'
                    branch 'master'
                    branch 'develop'
                }
            }
            steps {
                sh '''
                    docker build -f ingestion-service/Dockerfile -t enterprise-rag/ingestion-service:${BUILD_NUMBER} .
                    docker build -f query-service/Dockerfile -t enterprise-rag/query-service:${BUILD_NUMBER} .
                '''
            }
        }
    }

    post {
        success {
            echo 'Pipeline succeeded.'
        }
        failure {
            echo 'Pipeline failed — check tests, coverage, Sonar quality gate, or Checkmarx thresholds.'
        }
        always {
            cleanWs(deleteDirs: true, patterns: [[pattern: '**/build/tmp', type: 'INCLUDE']])
        }
    }
}
