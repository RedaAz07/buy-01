
pipeline {
    agent any

    stages {
        stage('Checkout') {
            steps {
                git branch: 'main',
                    url: 'https://github.com/RedaAz07/buy-01.git'
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

        stage('Docker Build') {
            steps {
                sh 'docker compose up -d'
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
