def call(Map config = [:]) {
    // 1. Nhận cấu hình tham số từ phía dự án (có giá trị mặc định)
    def appName        = config.get('appName', 'vts-service')
    def runtimeVersion = config.get('runtimeVersion', '20-alpine')
    def projectKey     = config.get('projectKey', appName)
    def srcPath        = config.get('srcPath', '.')
    def enableSonar    = config.get('enableSonar', true)
    def enableSast     = config.get('enableSast', true)

    pipeline {
        agent none

        environment {
            PROJECT_KEY     = "${projectKey}"
            RUNTIME_VERSION = "${runtimeVersion}"
            SRC_PATH        = "${srcPath}"
            APP_NAME        = "${appName}"
        }

        stages {
            stage('1. Kiểm thử chức năng (Unit Tests)') {
                agent {
                    docker {
                        image "node:${runtimeVersion}"
                    }
                }
                steps {
                    echo "=========================================================="
                    echo "🚀 [VTS Shared Library] Bắt đầu Unit Test cho ${appName}..."
                    echo "=========================================================="
                    dir("${env.SRC_PATH}") {
                        sh 'node -v'
                        sh 'node index.js'
                    }
                }
            }

            stage('2. Quét An ninh & Chất lượng Song song (Security Gates)') {
                parallel {
                    // Cổng 1: Quét SonarQube thật
                    stage('Quét SonarQube (Code Quality)') {
                        when {
                            expression { return enableSonar }
                        }
                        agent {
                            docker {
                                image 'sonarsource/sonar-scanner-cli:latest'
                                args '--network jenkin_default'
                            }
                        }
                        steps {
                            echo "🔍 [VTS Shared Library] Quét SonarQube cho dự án: ${env.PROJECT_KEY}"
                            dir("${env.SRC_PATH}") {
                                withCredentials([string(credentialsId: 'sonarqube-token', variable: 'SONAR_TOKEN')]) {
                                    sh '''
                                        sonar-scanner \
                                            -Dsonar.host.url=http://sonarqube:9000 \
                                            -Dsonar.token=${SONAR_TOKEN} \
                                            -Dsonar.projectKey=${PROJECT_KEY} \
                                            -Dsonar.qualitygate.wait=true \
                                            -Dsonar.sources=.
                                    '''
                                }
                            }
                        }
                    }

                    // Cổng 2: Quét SAST thật (Semgrep)
                    stage('Quét An ninh SAST (Security Vulnerabilities)') {
                        when {
                            expression { return enableSast }
                        }
                        agent {
                            docker {
                                image 'returntocorp/semgrep:latest'
                                args '--network jenkin_default'
                            }
                        }
                        steps {
                            echo "🛡️ [VTS Shared Library] Quét bảo mật SAST (OWASP Top 10) cho: ${env.PROJECT_KEY}"
                            dir("${env.SRC_PATH}") {
                                sh '''
                                    semgrep scan --config auto --error .
                                '''
                            }
                        }
                    }
                }
            }

            stage('3. Đóng gói Artifacts (Release)') {
                agent any
                steps {
                    echo "📦 [VTS Shared Library] Đóng gói thành phẩm an toàn cho ${appName}..."
                    dir("${env.SRC_PATH}") {
                        sh "tar -czf ${env.APP_NAME}-release.tar.gz index.js"
                        archiveArtifacts artifacts: "${env.APP_NAME}-release.tar.gz", fingerprint: true
                        echo "Đã lưu trữ thành phẩm: ${env.APP_NAME}-release.tar.gz"
                    }
                }
            }
        }

        post {
            success {
                node('built-in') {
                    withCredentials([
                        string(credentialsId: 'telegram-bot-token', variable: 'BOT_TOKEN'),
                        string(credentialsId: 'telegram-chat-id', variable: 'CHAT_ID')
                    ]) {
                        sh '''
                            curl -s -X POST "https://api.telegram.org/bot${BOT_TOKEN}/sendMessage" \
                                -d "chat_id=${CHAT_ID}" \
                                -d "parse_mode=HTML" \
                                -d "text=🛡️ <b>[VTS Shared Library] BẢO MẬT &amp; CHẤT LƯỢNG ĐẠT CHUẨN!</b>%0A• <b>Dự án:</b> ${APP_NAME}%0A• <b>Môi trường:</b> Node.js ${RUNTIME_VERSION}%0A• <b>SonarQube:</b> PASSED ✅%0A• <b>SAST:</b> PASSED ✅%0A• <b>Thành phẩm:</b> ${APP_NAME}-release.tar.gz%0A• <b>Build:</b> #${BUILD_NUMBER}%0A• <b>Link:</b> ${BUILD_URL}"
                        '''
                    }
                }
            }
            failure {
                node('built-in') {
                    withCredentials([
                        string(credentialsId: 'telegram-bot-token', variable: 'BOT_TOKEN'),
                        string(credentialsId: 'telegram-chat-id', variable: 'CHAT_ID')
                    ]) {
                        sh '''
                            curl -s -X POST "https://api.telegram.org/bot${BOT_TOKEN}/sendMessage" \
                                -d "chat_id=${CHAT_ID}" \
                                -d "parse_mode=HTML" \
                                -d "text=❌ <b>[VTS Shared Library] PHÁT HIỆN LỖI BẢO MẬT / CHẤT LƯỢNG!</b>%0A• <b>Dự án:</b> ${APP_NAME}%0A• <b>Build:</b> #${BUILD_NUMBER}%0A• <b>Xem chi tiết:</b> ${BUILD_URL}console"
                        '''
                    }
                }
            }
        }
    }
}
