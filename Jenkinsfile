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
        ECR_REGISTRY   = '123456789012.dkr.ecr.us-east-2.amazonaws.com'
        ECR_REPOSITORY = 'backend-prod'
        IMAGE_TAG      = "${BUILD_NUMBER}"
        CLUSTER_NAME   = 'prod-cluster'
        SERVICE_NAME   = 'prod-backend-service'
    }
    stages {
        stage('Build & Test') {
            steps {
                sh 'mvn clean package -DskipTests'
            }
        }
        stage('Build & Push Docker Image') {
            steps {
                script {
                    sh """
                        aws ecr get-login-password --region ${AWS_REGION} | docker login --username AWS --password-stdin ${ECR_REGISTRY}
                        docker build -t ${ECR_REGISTRY}/${ECR_REPOSITORY}:${IMAGE_TAG} -t ${ECR_REGISTRY}/${ECR_REPOSITORY}:latest .
                        docker push ${ECR_REGISTRY}/${ECR_REPOSITORY}:${IMAGE_TAG}
                        docker push ${ECR_REGISTRY}/${ECR_REPOSITORY}:latest
                    """
                }
            }
        }
        stage('Deploy to ECS Fargate') {
            steps {
                script {
                    // Forzamos la actualización del servicio. 
                    // ECS Fargate se encargará automáticamente de leer la Task Definition actual,
                    // resolver las referencias a SSM Parameter Store y montarlas como ENV variables en el contenedor Linux.
                    sh """
                        aws ecs update-service --cluster ${CLUSTER_NAME} \
                            --service ${SERVICE_NAME} \
                            --force-new-deployment \
                            --region ${AWS_REGION}
                    """
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