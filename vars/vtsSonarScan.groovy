def call(Map params = [:]) {
    def projectKey         = params.get('projectKey', env.JOB_BASE_NAME)
    def serverUrl          = params.get('serverUrl', 'http://sonarqube:9000')
    def credentialsId      = params.get('credentialsId', 'sonarqube-token')
    def waitForQualityGate = params.get('waitForQualityGate', true)
    def srcPath            = params.get('srcPath', '.')

    echo "🔍 [VTS Shared Step] Bắt đầu quét SonarQube cho dự án: ${projectKey}..."
    dir("${srcPath}") {
        docker.image('sonarsource/sonar-scanner-cli:latest').inside('--network jenkin_default') {
            withCredentials([string(credentialsId: credentialsId, variable: 'SONAR_TOKEN')]) {
                sh """
                    sonar-scanner \
                        -Dsonar.host.url=${serverUrl} \
                        -Dsonar.token=\${SONAR_TOKEN} \
                        -Dsonar.projectKey=${projectKey} \
                        -Dsonar.qualitygate.wait=${waitForQualityGate} \
                        -Dsonar.sources=.
                """
            }
        }
    }
}
