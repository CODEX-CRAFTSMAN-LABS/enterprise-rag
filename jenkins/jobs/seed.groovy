def gitRepoUrl = binding.hasVariable('GIT_REPO_URL')
    ? binding.getVariable('GIT_REPO_URL')
    : 'https://github.com/CODEX-CRAFTSMAN-LABS/enterprise-rag.git'
def devBranch = binding.hasVariable('DEV_BRANCH') ? binding.getVariable('DEV_BRANCH') : 'develop'
def prodBranch = binding.hasVariable('PROD_BRANCH') ? binding.getVariable('PROD_BRANCH') : 'main'

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

def createPipelineJob = { String jobPath, String display, String jenkinsfileBranch, String defaultBuildBranch, String scriptPath, String descriptionText ->
    pipelineJob(jobPath) {
        displayName(display)
        description(descriptionText)

        logRotator {
            numToKeep(30)
        }

        properties {
            parameters {
                stringParam(
                    'GIT_BRANCH',
                    defaultBuildBranch,
                    'Git branch to build (any branch, e.g. main, develop, feature/foo)'
                )
            }
            disableConcurrentBuilds()
        }

        definition {
            cpsScm {
                scm {
                    git {
                        remote {
                            url(gitRepoUrl)
                        }
                        // Load Jenkinsfile from this branch; actual code branch is chosen at build time via GIT_BRANCH.
                        branch(jenkinsfileBranch)
                        extensions {
                            cleanBeforeCheckout()
                        }
                    }
                }
                scriptPath(scriptPath)
                lightweight(true)
            }
        }
    }
}

createPipelineJob(
    'deploy/dev/enterprise-rag',
    'enterprise-rag',
    prodBranch,
    devBranch,
    'jenkins/Jenkinsfile.dev',
    'Full pipeline: build, test, push to ECR, deploy. Pick GIT_BRANCH under Build with Parameters.'
)

pipelineJob('deploy/dev/enterprise-rag-from-ecr') {
    displayName('enterprise-rag-from-ecr')
    description('Deploy images already in ECR (e.g. from GitHub Actions). Set IMAGE_TAG=<full-git-commit-sha>.')

    logRotator {
        numToKeep(30)
    }

    properties {
        parameters {
            stringParam(
                'IMAGE_TAG',
                '',
                'Full Git commit SHA (40 chars) from GitHub Actions ECR push log'
            )
            stringParam(
                'GIT_BRANCH',
                prodBranch,
                'Branch for compose/deploy scripts checkout'
            )
        }
        disableConcurrentBuilds()
    }

    definition {
        cpsScm {
            scm {
                git {
                    remote {
                        url(gitRepoUrl)
                    }
                    branch(prodBranch)
                    extensions {
                        cleanBeforeCheckout()
                    }
                }
            }
            scriptPath('jenkins/Jenkinsfile.deploy-ecr')
            lightweight(true)
        }
    }
}

createPipelineJob(
    'deploy/prod/enterprise-rag',
    'enterprise-rag',
    prodBranch,
    prodBranch,
    'jenkins/Jenkinsfile.prod',
    'Production deployment pipeline for Enterprise RAG. Pick any branch under Build with Parameters.'
)
