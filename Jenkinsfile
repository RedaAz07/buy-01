pipeline {
    agent any

    triggers {
        githubPush()
    }

    options {
        disableConcurrentBuilds()
        timestamps()
    }

    environment {
        FRONTEND_TEST_REPORT = 'frontend/test-results/*.xml'
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
                        set -e

                        cp "$ENV_FILE" .env

                        mkdir -p Backend/api-gateway/src/main/resources
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
                        sh '''
                            mkdir -p test-results

                            npm test -- \
                                --watch=false \
                                --no-progress
                        '''
                    }
                }
            }

            post {
                always {
                    junit(
                        testResults: 'Backend/**/target/surefire-reports/*.xml',
                        allowEmptyResults: true,
                        skipPublishingChecks: false
                    )

                    junit(
                        testResults: 'frontend/test-results/*.xml',
                        allowEmptyResults: true,
                        skipPublishingChecks: false
                    )

                    archiveArtifacts(
                        artifacts: '''
                            Backend/**/target/surefire-reports/*.xml,
                            frontend/test-results/*.xml
                        ''',
                        allowEmptyArchive: true,
                        fingerprint: true
                    )
                }
            }
        }

        stage('Deploy & Health Check') {
            steps {
                script {

                    try {
                        withCredentials([
                            file(credentialsId: 'buy01-env', variable: 'ENV_FILE'),
                            file(credentialsId: 'gateway-keystore.p12', variable: 'BACKEND_SSL'),
                            file(credentialsId: 'buy01-frontend-cert', variable: 'FRONTEND_CERT'),
                            file(credentialsId: 'buy01-frontend-key', variable: 'FRONTEND_KEY')
                        ]) {

                            sh '''
                                set -e

                                cp "$ENV_FILE" .env

                                mkdir -p Backend/api-gateway/src/main/resources
                                cp "$BACKEND_SSL" \
                                    Backend/api-gateway/src/main/resources/gateway-keystore.p12

                                mkdir -p frontend/certs
                                cp "$FRONTEND_CERT" frontend/certs/cert.pem
                                cp "$FRONTEND_KEY" frontend/certs/key.pem

                                echo "Starting deployment..."
                                docker compose up -d --build

                                echo "Waiting for services..."
                                sleep 15

                                echo "Checking container status..."
                                docker compose ps

                                if docker compose ps | grep -qE "Exited|dead"; then
                                    echo "Deployment health check failed."
                                    docker compose ps
                                    exit 1
                                fi

                                echo "Deployment completed successfully."
                            '''
                        }

                        echo "Deployment successful."

                    } catch (Exception e) {

                        echo "Deployment or health check failed."
                        echo "Starting rollback..."

                       
                        sh '''
                            set +e

                            echo "Current commit:"
                            git rev-parse HEAD

                            echo "Previous commit:"
                            git rev-parse HEAD~1

                            git checkout HEAD~1

                            echo "Re-injecting required secrets..."

                            cp "$ENV_FILE" .env

                            mkdir -p Backend/api-gateway/src/main/resources
                            cp "$BACKEND_SSL" \
                                Backend/api-gateway/src/main/resources/gateway-keystore.p12

                            mkdir -p frontend/certs
                            cp "$FRONTEND_CERT" frontend/certs/cert.pem
                            cp "$FRONTEND_KEY" frontend/certs/key.pem

                            echo "Re-deploying previous commit..."

                            docker compose up -d --build

                            sleep 15

                            docker compose ps

                            if docker compose ps | grep -qE "Exited|dead"; then
                                echo "Rollback deployment also appears unhealthy."
                                exit 1
                            fi

                            echo "Rollback completed."
                        '''

                        error(
                            "Deployment failed: ${e.getMessage()}. " +
                            "Rollback was attempted."
                        )
                    }
                }
            }

            post {
                success {
                    echo "Deployment stage completed successfully."
                }

                failure {
                    echo "Deployment stage failed. Rollback was attempted."
                }
            }
        }
    }

    post {

        success {
            catchError(
                buildResult: 'SUCCESS',
                stageResult: 'SUCCESS'
            ) {
                mail(
                    to: 'zdine30@gmail.com, annizreda07@gmail.com',
                    subject: "SUCCESS: ${env.JOB_NAME} #${env.BUILD_NUMBER}",
                    body: """Build and deployment succeeded.

Job: ${env.JOB_NAME}
Build: #${env.BUILD_NUMBER}
Status: SUCCESS

Build URL:
${env.BUILD_URL}

The application passed the build and test stages and was successfully deployed.
"""
                )
            }
        }

        failure {
            catchError(
                buildResult: 'SUCCESS',
                stageResult: 'SUCCESS'
            ) {
                mail(
                    to: 'zdine30@gmail.com, annizreda07@gmail.com',
                    subject: "FAILED: ${env.JOB_NAME} #${env.BUILD_NUMBER}",
                    body: """Pipeline execution failed.

Job: ${env.JOB_NAME}
Build: #${env.BUILD_NUMBER}
Status: FAILED

Build URL:
${env.BUILD_URL}

The pipeline failed during one of its stages.

If the failure happened during deployment, an automatic rollback was attempted.
Check the Jenkins console output and test reports for details.
"""
                )
            }
        }

        always {
            echo "Pipeline finished with status: ${currentBuild.currentResult}"
        }
    }
}
