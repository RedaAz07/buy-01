pipeline {
    agent any

    triggers {
        githubPush()
    }

    options {
        disableConcurrentBuilds()
    }

    stages {
        stage('Prepare Secrets') {
            steps {
                withCredentials([
                    file(credentialsId: 'buy01-env', variable: 'ENV_FILE'),
                    file(credentialsId: 'gateway-keystore.p12', variable: 'BACKEND_SSL'),
                    file(credentialsId: 'buy01-frontend-cert', variable: 'FRONTEND_CERT'),
                    file(credentialsId: 'buy01-frontend-key', variable: 'FRONTEND_KEY')
                ]) {
                    sh '''
                        cp "$ENV_FILE" .env
                        cp "$BACKEND_SSL" \
                            Backend/api-gateway/src/main/resources/gateway-keystore.p12

                        mkdir -p frontend/certs

                        cp "$FRONTEND_CERT" frontend/certs/cert.pem
                        cp "$FRONTEND_KEY" frontend/certs/key.pem
                    '''
                }
            }
        }

        stage('Build') {
            steps {
                script {
                    def backendServices = [
                        'registry',
                        'user-service',
                        'product-service',
                        'media-Service',
                        'api-gateway'
                    ]

                    backendServices.each { service ->
                        dir("Backend/${service}") {
                            sh 'chmod +x mvnw'
                            sh './mvnw clean package -DskipTests'
                        }
                    }

                    dir('frontend') {
                        sh 'npm ci --legacy-peer-deps'
                        sh 'npm run build -- --configuration production'
                    }
                }
            }
        }

        stage('Test') {
            steps {
                script {
                    def backendServices = [
                'registry',
                'user-service',
                'product-service',
                'media-Service',
                'api-gateway'
            ]

                    backendServices.each { service ->
                        dir("Backend/${service}") {
                            sh './mvnw test'
                        }
                    }

                    dir('frontend') {
                        sh 'npm test -- --watch=false'
                    }
                }
            }

            post {
                always {
                    junit 'Backend/**/target/surefire-reports/*.xml'
                }
            }
        }

        stage('Deploy & Health Check') {
            steps {
                script {
                    withCredentials([
                        file(credentialsId: 'buy01-env', variable: 'ENV_FILE'),
                        file(credentialsId: 'gateway-keystore.p12', variable: 'BACKEND_SSL'),
                        file(credentialsId: 'buy01-frontend-cert', variable: 'FRONTEND_CERT'),
                        file(credentialsId: 'buy01-frontend-key', variable: 'FRONTEND_KEY')
                    ]) {
                        try {
                            sh 'docker compose up -d --build'

                            sh '''
                                echo "Waiting 15s for containers to stabilize..."
                                sleep 15
                                if docker compose ps | grep -qE "Exited|dead"; then
                                    echo "Container health check failed!"
                                    exit 1
                                fi
                            '''
                        } catch (Exception e) {
                            echo ' Deployment or Health Check failed! Initiating rollback...'

                            sh '''
                                echo "Rolling back repository to previous commit (HEAD~1)..."
                                git checkout HEAD~1
                                # Re-inject secrets for previous state
                                cp "$ENV_FILE" .env
                                cp "$BACKEND_SSL" Backend/api-gateway/src/main/resources/gateway-keystore.p12
                                mkdir -p frontend/certs
                                cp "$FRONTEND_CERT" frontend/certs/cert.pem
                                cp "$FRONTEND_KEY" frontend/certs/key.pem
                                echo "Re-deploying previous stable version..."
                                docker compose up -d --build
                            '''

                            error("Deployment failed: ${e.getMessage()}. Successfully rolled back to previous commit.")
                        }
                    }
                }
            }
        }
    }

    post {
        always {
            cleanWs()
        }
        success {
            catchError(buildResult: 'SUCCESS', stageResult: 'SUCCESS') {
                mail(
                    to: 'zdine30@gmail.com, annizreda07@gmail.com',
                    subject: "SUCCESS: ${env.JOB_NAME} #${env.BUILD_NUMBER}",
                    body: """Build succeeded.

Job: ${env.JOB_NAME}
Build: #${env.BUILD_NUMBER}
Logs: ${env.BUILD_URL}"""
                )
            }
        }

        failure {
            catchError(buildResult: 'SUCCESS', stageResult: 'SUCCESS') {
                mail(
                    to: 'zdine30@gmail.com, annizreda07@gmail.com',
                    subject: "FAILED (Rolled Back): ${env.JOB_NAME} #${env.BUILD_NUMBER}",
                    body: """Build failed during pipeline execution.

Job: ${env.JOB_NAME}
Build: #${env.BUILD_NUMBER}
Logs: ${env.BUILD_URL}

If failure occurred in 'Deploy', the environment was automatically rolled back to the previous stable commit."""
                )
            }
        }
    }
}
