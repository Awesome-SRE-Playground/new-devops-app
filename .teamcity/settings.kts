import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.buildSteps.script
import jetbrains.buildServer.configs.kotlin.triggers.vcs

version = "2025.11"

project {
    buildType(Build)
}

object Build : BuildType({
    name = "CI/CD - Build & Deploy"

    params {
        param("env.DOCKER_USERNAME", "alvishpatelhti")

        password("env.DOCKER_PASSWORD", "")
        password("env.KUBECONFIG_DATA", "")
    }

    vcs {
        root(DslContext.settingsRoot)
    }

    steps {

        script {
            name = "Setup Kubeconfig"
            scriptContent = """
                echo "%env.KUBECONFIG_DATA%" | base64 --decode > kubeconfig.yaml
                echo "===== DEBUG KUBECONFIG ====="
                cat kubeconfig.yaml
                export KUBECONFIG=$(pwd)/kubeconfig.yaml
                kubectl config get-contexts
                kubectl get nodes
            """.trimIndent()
        }

        script {
            name = "Install Dependencies"
            scriptContent = """
                set -e
                cd app
                npm install
            """.trimIndent()
        }

        script {
            name = "Build Docker Image"
            scriptContent = """
                docker build -t %env.DOCKER_USERNAME%/devops-demo-app:%build.number% ./app
            """.trimIndent()
        }

        script {
            name = "Docker Login"
            scriptContent = """
                echo "%env.DOCKER_PASSWORD%" | docker login -u "%env.DOCKER_USERNAME%" --password-stdin
            """.trimIndent()
        }

        script {
            name = "Push Image"
            scriptContent = """
                docker push %env.DOCKER_USERNAME%/devops-demo-app:%build.number%
            """.trimIndent()
        }

        script {
            name = "Deploy via Helm"
            scriptContent = """
                export KUBECONFIG=$(pwd)/kubeconfig.yaml

                helm upgrade --install devops-demo ./devops-demo-chart \
                  -n demo-app \
                  --create-namespace \
                  --set image.tag=%build.number%
            """.trimIndent()
        }
    }

    triggers {
        vcs {}
    }
})
