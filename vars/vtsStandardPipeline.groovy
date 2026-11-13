def call(Closure body) {
    // 1. Thu thập cấu hình truyền từ Jenkinsfile của dự án qua Closure
    def config = [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = config
    body()

    def appName        = config.appName ?: env.JOB_BASE_NAME
    def projectKey     = config.projectKey ?: appName
    def runtimeImage   = config.runtimeImage ?: 'node:20-alpine'
    def testCommand    = config.testCommand ?: { sh 'node index.js' }
    def buildCommand   = config.buildCommand ?: { sh "tar -czf ${appName}-release.tar.gz index.js" }
    def artifactFile   = config.artifactFile ?: "${appName}-release.tar.gz"
    def enableSonar    = config.enableSonar != null ? config.enableSonar : true
    def enableSast     = config.enableSast != null ? config.enableSast : true
    def enforceGate    = config.enforceGate != null ? config.enforceGate : true

    pipeline {
        agent any

        stages {
            // GIAI ĐOẠN 1: DEV TỰ ĐỊNH NGHĨA LỆNH TEST
            stage('1. Kiểm thử chức năng (Unit Tests)') {
                steps {
                    echo "=========================================================="
                    echo "🚀 [VTS Framework] Chạy Unit Test cho ${appName}..."
                    echo "=========================================================="
                    script {
                        docker.image(runtimeImage).inside {
                            testCommand()
                        }
                    }
                }
            }

            // GIAI ĐOẠN 2: ÉP BUỘC CHẠY BẢO MẬT & CHẤT LƯỢNG
            stage('2. Quét An ninh & Chất lượng Song song') {
                parallel {
                    stage('Quét SonarQube') {
                        when { expression { return enableSonar } }
                        steps {
                            script {
                                // Tái sử dụng chính viên gạch vtsSonarScan
                                vtsSonarScan(projectKey: projectKey)
                            }
                        }
                    }

                    stage('Quét An ninh SAST') {
                        when { expression { return enableSast } }
                        steps {
                            script {
                                // Tái sử dụng chính viên gạch vtsSecurityScan
                                vtsSecurityScan(enforce: enforceGate)
                            }
                        }
                    }
                }
            }

            // GIAI ĐOẠN 3: DEV TỰ ĐỊNH NGHĨA LỆNH ĐÓNG GÓI
            stage('3. Đóng gói Artifacts (Release)') {
                steps {
                    echo "📦 [VTS Framework] Đóng gói thành phẩm cho ${appName}..."
                    script {
                        buildCommand()
                        archiveArtifacts artifacts: "${artifactFile}", fingerprint: true
                        echo "Đã lưu trữ thành phẩm: ${artifactFile}"
                    }
                }
            }
        }

        post {
            success {
                script {
                    vtsNotifyTelegram(status: 'SUCCESS', appName: appName)
                }
            }
            failure {
                script {
                    vtsNotifyTelegram(status: 'FAILURE', appName: appName)
                }
            }
        }
    }
}
