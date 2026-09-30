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

                            echo "Deploying commit: ${env.GIT_COMMIT}"

                            sh '''
                                docker compose up -d --build

                                echo "Waiting 15 seconds for containers to stabilize..."
                                sleep 15

                                echo "Checking container status..."

                                if docker compose ps | grep -qE "Exited|dead"; then
                                    echo "Container health check failed!"
                                    docker compose ps
                                    exit 1
                                fi

                                docker compose ps
                            '''

                        } catch (Exception deployError) {

                            echo "Deployment failed."

                           
                            if (!env.GIT_PREVIOUS_SUCCESSFUL_COMMIT) {
                                error(
                                    "Deploy failed and no previous successful commit " +
                                    "exists to roll back to. Manual intervention required."
                                )
                            }

                            echo """
                            Deploy failed — rolling back to:
                            ${env.GIT_PREVIOUS_SUCCESSFUL_COMMIT}
                            """

                            try {

                                sh '''
                                    echo "Stopping failed deployment..."

                                    docker compose down

                                    echo "Checking out previous successful commit..."

                                    git checkout --force ${GIT_PREVIOUS_SUCCESSFUL_COMMIT}

                                    echo "Re-injecting secrets..."

                                    cp "$ENV_FILE" .env

                                    cp "$BACKEND_SSL" \
                                        Backend/api-gateway/src/main/resources/gateway-keystore.p12

                                    mkdir -p frontend/certs

                                    cp "$FRONTEND_CERT" frontend/certs/cert.pem
                                    cp "$FRONTEND_KEY" frontend/certs/key.pem

                                    echo "Rebuilding previous successful version..."

                                    docker compose up -d --build

                                    echo "Waiting 15 seconds for rollback deployment..."

                                    sleep 15

                                    echo "Checking rollback containers..."

                                    if docker compose ps | grep -qE "Exited|dead"; then
                                        echo "Rollback health check failed!"
                                        docker compose ps
                                        exit 1
                                    fi

                                    docker compose ps

                                    echo "Rollback completed successfully."
                                '''

                            } catch (Exception rollbackError) {

                                error("""
Deployment failed AND rollback failed.

Failed deployment commit:
${env.GIT_COMMIT}

Previous successful commit:
${env.GIT_PREVIOUS_SUCCESSFUL_COMMIT}

Rollback error:
${rollbackError.getMessage()}

Manual intervention is required.
""")
                            }

                            error(
                                "Deployment failed. " +
                                "Successfully rolled back to previous successful commit " +
                                "${env.GIT_PREVIOUS_SUCCESSFUL_COMMIT}"
                            )

                        } finally {

                         
                            sh '''
                                rm -f .env
                                rm -f Backend/api-gateway/src/main/resources/gateway-keystore.p12
                                rm -f frontend/certs/cert.pem
                                rm -f frontend/certs/key.pem
                            '''
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

Previous successful commit:
${env.GIT_PREVIOUS_SUCCESSFUL_COMMIT ?: 'Not available'}

Logs: ${env.BUILD_URL}"""
                )
            }
        }
    }
}
