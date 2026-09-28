pipeline {
    agent any

    stages {
        stage('Checkout') {
            steps {
                git branch: 'main',
                    url: 'https://github.com/RedaAz07/buy-01.git'
            }
        }

        stage('Prepare Secrets') {
            steps {
                withCredentials([
                    file(credentialsId: 'buy01-env', variable: 'ENV_FILE'),
                    file(credentialsId: 'gateway-keystore.p12', variable: 'SSL_FILE')
                ]) {
                    sh '''
                        cp "$ENV_FILE" .env
                        cp "$SSL_FILE" Backend/api-gateway/src/main/resources/gateway-keystore.p12
                    '''
                }
            }
        }

        stage('Build') {
            steps {
                dir('Backend/api-gateway') {
                    sh 'chmod +x mvnw'
                    sh './mvnw clean package -DskipTests'
                }
            }
        }

        stage('Test') {
            steps {
                dir('Backend/api-gateway') {
                    sh './mvnw test'
                }
            }
        }

        stage('Docker Info') {
            steps {
                sh 'docker version'
                sh 'docker compose version'
                sh 'docker buildx version'
            }
        }
    }

    post {
        success {
            script {
                mail(
                    to: 'zdine30@gmail.com',
                    subject: "SUCCESS: ${env.JOB_NAME} #${env.BUILD_NUMBER}",
                    body: """Build succeeded.

Job: ${env.JOB_NAME}
Build: #${env.BUILD_NUMBER}
Logs: ${env.BUILD_URL}"""
                )
            }
        }

        failure {
            script {
                mail(
                    to: 'zdine30@gmail.com',
                    subject: "FAILED: ${env.JOB_NAME} #${env.BUILD_NUMBER}",
                    body: """Build failed.

Job: ${env.JOB_NAME}
Build: #${env.BUILD_NUMBER}
Logs: ${env.BUILD_URL}"""
                )
            }
        }
    }
}
