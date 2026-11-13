# VTS Standard Pipeline Shared Library

Thư viện CI/CD dùng chung chuẩn DevSecOps cho các dự án Microservices tại Viettel Solutions (VTS).

## Cấu trúc thư viện
- `vars/vtsPipeline.groovy`: Pipeline chuẩn hóa bao gồm Unit Test, SonarQube Scanner, SAST (Semgrep), đóng gói Artifacts và cảnh báo Telegram.

## Cách sử dụng trong Jenkinsfile của dự án con
```groovy
@Library('vts-shared-library') _

vtsPipeline(
    appName: 'vts-order-service',
    runtimeVersion: '20-alpine',
    projectKey: 'vts-order-service',
    enableSonar: true,
    enableSast: true
)
```
