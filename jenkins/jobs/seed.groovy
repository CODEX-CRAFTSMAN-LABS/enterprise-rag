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

folder('deploy') {
    displayName('deploy')
    description('Deployment jobs for Enterprise RAG.')
}

folder('deploy/dev') {
    displayName('dev')
    description('Development deployment jobs.')
}

folder('deploy/prod') {
    displayName('prod')
    description('Production deployment jobs.')
}

def createServicePipelineJob = { String jobPath, String display, String service, String envName, String jenkinsfileBranch, String descriptionText ->
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
                'Full Git commit SHA (40 chars) to build, push to ECR, and deploy.'
            )
        }

        properties {
            disableConcurrentBuilds()
        }

        environmentVariables {
            env('SERVICE', service)
            env('ENV_NAME', envName)
            env('AWS_REGION', awsRegion)
            env('AWS_ACCOUNT_ID', awsAccountId)
            env('ECR_REGISTRY', ecrRegistry)
            env('STAGING_SSH_HOST', stagingSshHost)
            env('STAGING_SSH_USER', stagingSshUser)
            env('STAGING_PATH', stagingPath)
            env('DEPLOY_TARGET', deployTarget)
            env('ENABLE_DEPLOY', enableDeploy)
            env('ENABLE_SONAR', enableSonar)
            env('OLLAMA_IMAGE', ollamaImage)
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
                scriptPath('jenkins/Jenkinsfile.service')
                lightweight(true)
            }
        }
    }
}

createServicePipelineJob(
    'deploy/dev/ingestion-service',
    'ingestion-service',
    'ingestion',
    'dev',
    prodBranch,
    'Dev: build rag-common + ingestion-service at GIT_COMMIT_SHA, push to ECR, deploy ingestion only.'
)

createServicePipelineJob(
    'deploy/dev/query-service',
    'query-service',
    'query',
    'dev',
    prodBranch,
    'Dev: build rag-common + query-service at GIT_COMMIT_SHA, push to ECR, deploy query only.'
)

createServicePipelineJob(
    'deploy/prod/ingestion-service',
    'ingestion-service',
    'ingestion',
    'prod',
    prodBranch,
    'Prod: build rag-common + ingestion-service at GIT_COMMIT_SHA, push to ECR, deploy ingestion only.'
)

createServicePipelineJob(
    'deploy/prod/query-service',
    'query-service',
    'query',
    'prod',
    prodBranch,
    'Prod: build rag-common + query-service at GIT_COMMIT_SHA, push to ECR, deploy query only.'
)
