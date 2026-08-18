pipeline {
    agent {
        docker {
            image 'maven:3.9-eclipse-temurin-21-alpine'
            // Uso estricto del Socket de Docker para agentes efímeros
            args '-v /var/run/docker.sock:/var/run/docker.sock -v /root/.m2:/root/.m2'
        }
    }
    environment {
        AWS_REGION     = 'us-east-2'
        ECR_REGISTRY   = credentials('ECR_REGISTRY')
        ECR_REPOSITORY = 'backend-prod'
        IMAGE_TAG      = "${BUILD_NUMBER}"
        CLUSTER_NAME   = 'products-cluster'
        SERVICE_NAME   = 'prod-backend-service'
        TASK_FAMILY    = 'prod-backend-service'
    }
    stages {
        stage('Build & Test') {
            steps {
                sh 'mvn clean package -DskipTests'
            }
        }
        stage('Install Tooling') {
            steps {
                // La imagen maven:...-alpine solo trae el socket montado, no los clientes
                sh 'apk add --no-cache aws-cli docker-cli jq'
            }
        }
        stage('Build & Push Docker Image') {
            steps {
                script {
                    withCredentials([
                        string(credentialsId: 'aws-access-key-id', variable: 'AWS_ACCESS_KEY_ID'),
                        string(credentialsId: 'aws-secret-access-key', variable: 'AWS_SECRET_ACCESS_KEY')
                    ]) {
                        sh """
                            export AWS_ACCESS_KEY_ID=${AWS_ACCESS_KEY_ID}
                            export AWS_SECRET_ACCESS_KEY=${AWS_SECRET_ACCESS_KEY}
                            aws ecr get-login-password --region ${AWS_REGION} | docker login --username AWS --password-stdin ${ECR_REGISTRY}
                            docker build -t ${ECR_REGISTRY}/${ECR_REPOSITORY}:${IMAGE_TAG} -t ${ECR_REGISTRY}/${ECR_REPOSITORY}:latest .
                            docker push ${ECR_REGISTRY}/${ECR_REPOSITORY}:${IMAGE_TAG}
                            docker push ${ECR_REGISTRY}/${ECR_REPOSITORY}:latest
                        """
                    }
                }
            }
        }
        stage('Deploy to ECS Fargate') {
            steps {
                script {
                    withCredentials([
                        string(credentialsId: 'aws-access-key-id', variable: 'AWS_ACCESS_KEY_ID'),
                        string(credentialsId: 'aws-secret-access-key', variable: 'AWS_SECRET_ACCESS_KEY')
                    ]) {
                        // Clona la Task Definition activa, la apunta al tag inmutable del build
                        // y registra una revision nueva: el despliegue queda trazado a esa imagen exacta.
                        sh """
                            export AWS_ACCESS_KEY_ID=${AWS_ACCESS_KEY_ID}
                            export AWS_SECRET_ACCESS_KEY=${AWS_SECRET_ACCESS_KEY}

                            NEW_IMAGE="${ECR_REGISTRY}/${ECR_REPOSITORY}:${IMAGE_TAG}"

                            aws ecs describe-task-definition \
                                --task-definition ${TASK_FAMILY} \
                                --region ${AWS_REGION} \
                                --query 'taskDefinition' > current-task-def.json

                            jq --arg IMAGE "\$NEW_IMAGE" \
                                '.containerDefinitions[0].image = \$IMAGE
                                 | del(.taskDefinitionArn, .revision, .status, .requiresAttributes, .compatibilities, .registeredAt, .registeredBy)' \
                                current-task-def.json > new-task-def.json

                            NEW_REVISION=\$(aws ecs register-task-definition \
                                --cli-input-json file://new-task-def.json \
                                --region ${AWS_REGION} \
                                --query 'taskDefinition.revision' \
                                --output text)

                            aws ecs update-service \
                                --cluster ${CLUSTER_NAME} \
                                --service ${SERVICE_NAME} \
                                --task-definition ${TASK_FAMILY}:\$NEW_REVISION \
                                --region ${AWS_REGION}
                        """
                    }
                }
            }
        }
    }
    post {
        always {
            sh 'docker logout ${ECR_REGISTRY}'
        }
    }
}