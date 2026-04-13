import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.buildFeatures.perfmon
import jetbrains.buildServer.configs.kotlin.buildSteps.script
import jetbrains.buildServer.configs.kotlin.triggers.vcs

/*
The settings script is an entry point for defining a TeamCity
project hierarchy. The script should contain a single call to the
project() function with a Project instance or an init function as
an argument.

VcsRoots, BuildTypes, Templates, and subprojects can be
registered inside the project using the vcsRoot(), buildType(),
template(), and subProject() methods respectively.

To debug settings scripts in command-line, run the

    mvnDebug org.jetbrains.teamcity:teamcity-configs-maven-plugin:generate

command and attach your debugger to the port 8000.

To debug in IntelliJ Idea, open the 'Maven Projects' tool window (View
-> Tool Windows -> Maven Projects), find the generate task node
(Plugins -> teamcity-configs -> teamcity-configs:generate), the
'Debug' option is available in the context menu for the task.
*/

version = "2025.11"

project {

    buildType(Build)
}

object Build : BuildType({
    name = "CI/CD - Build & Deploy"

    params {
        param("env.DOCKER_USERNAME", "aelvishpatelhti")

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
                echo "$KUBECONFIG_DATA" | base64 --decode > kubeconfig.yaml
                export KUBECONFIG=$(pwd)/kubeconfig.yaml
                
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
                docker build -t $DOCKER_USERNAME/new-devops-app:%build.number% ./app
            """.trimIndent()
        }

        script {
            name = "Docker Login"
            scriptContent = """
                echo "$DOCKER_PASSWORD" | docker login -u "$DOCKER_USERNAME" --password-stdin
            """.trimIndent()
        }

        script {
            name = "Push Image"
            scriptContent = """
                docker push $DOCKER_USERNAME/devops-demo-app:%build.number%
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

    features {
        perfmon {}
    }
})
