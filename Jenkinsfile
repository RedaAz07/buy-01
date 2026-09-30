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
                        cp "$BACKEND_SSL" Backend/api-gateway/src/main/resources/gateway-keystore.p12
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
                    def backendServices = ['registry', 'user-service', 'product-service', 'media-Service', 'api-gateway']

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
                    def backendServices = ['registry', 'user-service', 'product-service', 'media-Service', 'api-gateway']

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
                    junit 'frontend/test-results/*.xml'
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
                            echo "🚀 Deploying commit: ${env.GIT_COMMIT}"

                                sh '''
                                        docker compose up -d --build
                                        echo "Waiting 25 seconds for microservices to initialize..."
                                        sleep 25
                                        echo "Current Container Status:"
                                        docker compose ps
                                        # Exited, dead, Restarting, أو unhealthy
                                        if docker compose ps | grep -qE "Exited|dead|Restarting|unhealthy"; then
                                            echo "❌ Health check failed: One or more containers crashed or are stuck restarting!"
                                            exit 1
                                        fi
                                        echo "✅ All containers are healthy and running!"
                                        '''
                        } catch (Exception deployError) {
                            echo '⚠️ Deployment failed! Checking rollback availability...'

                            if (env.GIT_PREVIOUS_SUCCESSFUL_COMMIT) {
                                sh '''
                                    echo "Rolling back to commit: ${GIT_PREVIOUS_SUCCESSFUL_COMMIT}"
                                    docker compose down
                                    git checkout --force ${GIT_PREVIOUS_SUCCESSFUL_COMMIT}

                                    # Re-inject secrets & rebuild stable release
                                    cp "$ENV_FILE" .env
                                    cp "$BACKEND_SSL" Backend/api-gateway/src/main/resources/gateway-keystore.p12
                                    mkdir -p frontend/certs
                                    cp "$FRONTEND_CERT" frontend/certs/cert.pem
                                    cp "$FRONTEND_KEY" frontend/certs/key.pem

                                    docker compose up -d --build
                                '''
                                error("Deployment failed. Rolled back to commit ${env.GIT_PREVIOUS_SUCCESSFUL_COMMIT}")
                            } else {
                                error('Deployment failed and no previous successful commit exists to roll back to.')
                            }
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
Commit: ${env.GIT_COMMIT}
Logs: ${env.BUILD_URL}"""
                )
            }
        }

        failure {
            catchError(buildResult: 'SUCCESS', stageResult: 'SUCCESS') {
                mail(
                    to: 'zdine30@gmail.com, annizreda07@gmail.com',
                    subject: "FAILED: ${env.JOB_NAME} #${env.BUILD_NUMBER}",
                    body: """Build failed during pipeline execution.

Job: ${env.JOB_NAME}
Build: #${env.BUILD_NUMBER}
Failed commit: ${env.GIT_COMMIT}
Previous stable commit: ${env.GIT_PREVIOUS_SUCCESSFUL_COMMIT ?: 'N/A'}
Logs: ${env.BUILD_URL}"""
                )
            }
        }
    }
}
