def call(Map params = [:]) {
    def status     = params.get('status', 'SUCCESS')
    def appName    = params.get('appName', env.JOB_BASE_NAME)
    def icon       = (status == 'SUCCESS') ? '🛡️' : '❌'
    def statusText = (status == 'SUCCESS') ? 'BẢO MẬT &amp; CHẤT LƯỢNG ĐẠT CHUẨN (PASSED ✅)' : 'PHÁT HIỆN LỖI BẢO MẬT / CHẤT LƯỢNG (FAILED ❌)'

    node('built-in') {
        withCredentials([
            string(credentialsId: 'telegram-bot-token', variable: 'BOT_TOKEN'),
            string(credentialsId: 'telegram-chat-id', variable: 'CHAT_ID')
        ]) {
            sh """
                curl -s -X POST "https://api.telegram.org/bot\${BOT_TOKEN}/sendMessage" \
                    -d "chat_id=\${CHAT_ID}" \
                    -d "parse_mode=HTML" \
                    -d "text=${icon} <b>[VTS Pipeline Alert]</b>%0A• <b>Dự án:</b> ${appName}%0A• <b>Trạng thái:</b> ${statusText}%0A• <b>Build:</b> #${env.BUILD_NUMBER}%0A• <b>Xem chi tiết:</b> ${env.BUILD_URL}"
            """
        }
    }
}
