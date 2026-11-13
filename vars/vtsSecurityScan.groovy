def call(Map params = [:]) {
    def enforce = params.get('enforce', true)
    def srcPath = params.get('srcPath', '.')

    echo "🛡️ [VTS Shared Step] Bắt đầu quét an ninh bảo mật Semgrep (OWASP Top 10)..."
    dir("${srcPath}") {
        docker.image('returntocorp/semgrep:latest').inside('--network jenkin_default') {
            if (enforce) {
                sh 'semgrep scan --config auto --error .'
            } else {
                sh 'semgrep scan --config auto . || true'
            }
        }
    }
}
