def gitRepoUrl = binding.hasVariable('GIT_REPO_URL')
    ? binding.getVariable('GIT_REPO_URL')
    : 'https://github.com/CODEX-CRAFTSMAN-LABS/enterprise-rag.git'
def devBranch = binding.hasVariable('DEV_BRANCH') ? binding.getVariable('DEV_BRANCH') : 'develop'
def prodBranch = binding.hasVariable('PROD_BRANCH') ? binding.getVariable('PROD_BRANCH') : 'main'

def awsRegion = binding.hasVariable('AWS_REGION') ? binding.getVariable('AWS_REGION') : 'ap-south-1'
def awsAccountId = binding.hasVariable('AWS_ACCOUNT_ID') ? binding.getVariable('AWS_ACCOUNT_ID') : '440977419877'
def ecrRegistry = binding.hasVariable('ECR_REGISTRY') ? binding.getVariable('ECR_REGISTRY') : "${awsAccountId}.dkr.ecr.${awsRegion}.amazonaws.com"
def stagingSshHost = binding.hasVariable('STAGING_SSH_HOST') ? binding.getVariable('STAGING_SSH_HOST') : ''
def stagingSshUser = binding.hasVariable('STAGING_SSH_USER') ? binding.getVariable('STAGING_SSH_USER') : 'ubuntu'
def stagingPath = binding.hasVariable('STAGING_PATH') ? binding.getVariable('STAGING_PATH') : '/opt/enterprise-rag'
def deployTarget = binding.hasVariable('DEPLOY_TARGET') ? binding.getVariable('DEPLOY_TARGET') : 'ec2'
def enableDeploy = binding.hasVariable('ENABLE_DEPLOY') ? binding.getVariable('ENABLE_DEPLOY') : 'true'
def enableSonar = binding.hasVariable('ENABLE_SONAR') ? binding.getVariable('ENABLE_SONAR') : 'false'
def ollamaImage = binding.hasVariable('OLLAMA_IMAGE') ? binding.getVariable('OLLAMA_IMAGE') : 'alpine/ollama:0.23.2'

folder('build') {
    displayName('build')
    description('Build, test, and push service images to ECR (no deploy).')
}

folder('deploy') {
    displayName('deploy')
    description('Deploy pre-built images from ECR to dev or prod.')
}

folder('deploy/dev') {
    displayName('dev')
    description('Development deployment jobs.')
}

folder('deploy/prod') {
    displayName('prod')
    description('Production deployment jobs.')
}

def createBuildJob = { String jobPath, String display, String service, String jenkinsfileBranch, String descriptionText ->
    pipelineJob(jobPath) {
        displayName(display)
        description(descriptionText)

        logRotator {
            numToKeep(30)
        }

        parameters {
            stringParam(
                'GIT_COMMIT_SHA',
                '',
                'Full Git commit SHA (40 chars) to checkout, build, test, and push to ECR.'
            )
        }

        properties {
            disableConcurrentBuilds()
        }

        definition {
            cpsScm {
                scm {
                    git {
                        remote {
                            url(gitRepoUrl)
                        }
                        branch(jenkinsfileBranch)
                        extensions {
                            cleanBeforeCheckout()
                        }
                    }
                }
                scriptPath('jenkins/Jenkinsfile.build')
                lightweight(true)
            }
        }
    }
}

def createDeployJob = { String jobPath, String display, String service, String envName, String jenkinsfileBranch, String descriptionText ->
    pipelineJob(jobPath) {
        displayName(display)
        description(descriptionText)

        logRotator {
            numToKeep(30)
        }

        parameters {
            stringParam(
                'GIT_COMMIT_SHA',
                '',
                'Full Git commit SHA (40 chars) — image tag in ECR (from build job).'
            )
        }

        properties {
            disableConcurrentBuilds()
        }

        definition {
            cpsScm {
                scm {
                    git {
                        remote {
                            url(gitRepoUrl)
                        }
                        branch(jenkinsfileBranch)
                        extensions {
                            cleanBeforeCheckout()
                        }
                    }
                }
                scriptPath('jenkins/Jenkinsfile.deploy-service')
                lightweight(true)
            }
        }
    }
}

createBuildJob(
    'build/ingestion-service',
    'ingestion-service',
    'ingestion',
    prodBranch,
    'Build rag-common + ingestion-service at GIT_COMMIT_SHA and push to ECR.'
)

createBuildJob(
    'build/query-service',
    'query-service',
    'query',
    prodBranch,
    'Build rag-common + query-service at GIT_COMMIT_SHA and push to ECR.'
)

createDeployJob(
    'deploy/dev/ingestion-service',
    'ingestion-service',
    'ingestion',
    'dev',
    prodBranch,
    'Deploy ingestion-service image (GIT_COMMIT_SHA) to dev — run build/ingestion-service first.'
)

createDeployJob(
    'deploy/dev/query-service',
    'query-service',
    'query',
    'dev',
    prodBranch,
    'Deploy query-service image (GIT_COMMIT_SHA) to dev — run build/query-service first.'
)

createDeployJob(
    'deploy/prod/ingestion-service',
    'ingestion-service',
    'ingestion',
    'prod',
    prodBranch,
    'Deploy ingestion-service image (GIT_COMMIT_SHA) to prod — run build/ingestion-service first.'
)

createDeployJob(
    'deploy/prod/query-service',
    'query-service',
    'query',
    'prod',
    prodBranch,
    'Deploy query-service image (GIT_COMMIT_SHA) to prod — run build/query-service first.'
)
