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
        IMAGE_TAG      = "${BUILD_NUMBER}"
        CLUSTER_NAME   = 'products-cluster'
        K8S_NAMESPACE  = 'products'
        DEPLOYMENT_NAME = 'backend-products-api'
        CONTAINER_NAME = 'products-api'
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
                sh 'apk add --no-cache aws-cli docker-cli kubectl'
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
                            ECR_HOST="\${ECR_REGISTRY%%/*}"
                            aws ecr get-login-password --region ${AWS_REGION} | docker login --username AWS --password-stdin "\$ECR_HOST"
                            docker build -t ${ECR_REGISTRY}:${IMAGE_TAG} -t ${ECR_REGISTRY}:latest .
                            docker push ${ECR_REGISTRY}:${IMAGE_TAG}
                            docker push ${ECR_REGISTRY}:latest
                        """
                    }
                }
            }
        }
        stage('Deploy to EKS') {
            steps {
                script {
                    withCredentials([
                        string(credentialsId: 'aws-access-key-id', variable: 'AWS_ACCESS_KEY_ID'),
                        string(credentialsId: 'aws-secret-access-key', variable: 'AWS_SECRET_ACCESS_KEY')
                    ]) {
                        sh """
                            export AWS_ACCESS_KEY_ID=${AWS_ACCESS_KEY_ID}
                            export AWS_SECRET_ACCESS_KEY=${AWS_SECRET_ACCESS_KEY}

                            aws eks update-kubeconfig \
                                --name ${CLUSTER_NAME} \
                                --region ${AWS_REGION}

                            kubectl get namespace ${K8S_NAMESPACE} >/dev/null 2>&1 || \
                                kubectl create namespace ${K8S_NAMESPACE}

                            kubectl apply \
                                -f products-api-configmap.yaml \
                                -f products-api-secrets.yaml \
                                -f products-api-deployment.yaml \
                                -f products-api-service.yaml

                            kubectl -n ${K8S_NAMESPACE} set image \
                                deployment/${DEPLOYMENT_NAME} \
                                ${CONTAINER_NAME}=${ECR_REGISTRY}:${IMAGE_TAG}

                            kubectl -n ${K8S_NAMESPACE} rollout status \
                                deployment/${DEPLOYMENT_NAME} \
                                --timeout=180s
                        """
                    }
                }
            }
        }
    }
    post {
        always {
            sh 'docker logout "${ECR_REGISTRY%%/*}"'
        }
    }
}