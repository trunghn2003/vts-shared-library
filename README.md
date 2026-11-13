# VTS Pipeline Shared Library (Enterprise DevSecOps)

Thư viện chuẩn hóa CI/CD của Viettel Solutions (VTS), hỗ trợ cả 2 mô hình vận hành trong doanh nghiệp:

---

### Mô hình 1: Dùng các Step độc lập (Linh hoạt cho mọi ngôn ngữ)
Dành cho các dự án muốn tự do viết cấu trúc Pipeline nhưng cần tuân thủ các cổng an ninh và hạ tầng của Viettel:
```groovy
pipeline {
    agent any
    stages {
        stage('Unit Test') {
            steps {
                sh 'mvn test' // hoặc yarn test, go test, pytest
            }
        }
        stage('An ninh & Chất lượng') {
            steps {
                vtsSonarScan(projectKey: 'my-service')
                vtsSecurityScan(enforce: true)
            }
        }
    }
    post {
        always {
            vtsNotifyTelegram(appName: 'my-service')
        }
    }
}
```

---

### Mô hình 2: Dùng Framework chuẩn hóa với Closures (Nhanh chóng & Nghiêm ngặt)
Dành cho các Microservices tiêu chuẩn, Dev chỉ cần định nghĩa lệnh build/test của mình:
```groovy
@Library('vts-shared-library') _

vtsStandardPipeline {
    appName      = 'vts-order-service'
    projectKey   = 'vts-order-service'
    runtimeImage = 'node:20-alpine' // hoặc maven:3.9-eclipse-temurin-17
    
    // Dev tự điền lệnh test của mình
    testCommand = {
        sh 'node -v'
        sh 'node index.js'
    }

    // Dev tự điền lệnh đóng gói
    buildCommand = {
        sh 'tar -czf vts-order-service-release.tar.gz index.js'
    }
}
```
