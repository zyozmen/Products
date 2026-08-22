pipeline {
    agent {
        docker {
            image 'maven:3.9-eclipse-temurin-21-alpine'
            // Uso estricto del Socket de Docker para agentes efímeros
            args '--network=products-net -v /var/run/docker.sock:/var/run/docker.sock -v products-maven-repository:/root/.m2'
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
        stage('Unit Tests') {
            steps {
                sh 'mvn -B -ntp clean test'
            }
        }
        stage('E2E Tests') {
            steps {
                timeout(time: 10, unit: 'MINUTES') {
                    sh 'echo "Resolucion DNS de Mongo desde el agente E2E:"; getent hosts mongo'
                    sh 'mvn -B -ntp -Dit.test=ProductoApiIT failsafe:integration-test failsafe:verify'
                }
            }
            post {
                always {
                    junit allowEmptyResults: true, testResults: 'target/failsafe-reports/*.xml'
                }
            }
        }
        stage('Install Tooling') {
            steps {
                // La imagen maven:...-alpine solo trae el socket montado, no los clientes
                sh '''
                    apk add --no-cache aws-cli curl docker-cli kubectl
                    curl -Lo /usr/local/bin/kind https://kind.sigs.k8s.io/dl/v0.29.0/kind-linux-amd64
                    chmod +x /usr/local/bin/kind
                '''
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
        stage('Deploy to kind (Local)') {
            steps {
                sh """
                    docker network inspect products-net >/dev/null 2>&1 || {
                        echo 'La red Docker products-net no existe'
                        exit 1
                    }

                    if ! kind get clusters | grep -qx '${CLUSTER_NAME}'; then
                        kind create cluster --name ${CLUSTER_NAME}
                    fi

                    for node in \$(kind get nodes --name ${CLUSTER_NAME}); do
                        docker network connect products-net "\$node" 2>/dev/null || true
                    done

                    kind load docker-image ${ECR_REGISTRY}:${IMAGE_TAG} --name ${CLUSTER_NAME}

                    kubectl --context kind-${CLUSTER_NAME} get namespace ${K8S_NAMESPACE} >/dev/null 2>&1 || \\
                        kubectl --context kind-${CLUSTER_NAME} create namespace ${K8S_NAMESPACE}

                    sed \\
                        -e "s|image: .*|image: ${ECR_REGISTRY}:${IMAGE_TAG}|" \\
                        -e 's|imagePullPolicy: Always|imagePullPolicy: IfNotPresent|' \\
                        products-api-deployment.yaml > deployment-kind-rendered.yaml

                    kubectl --context kind-${CLUSTER_NAME} apply \\
                        -f products-api-configmap.yaml \\
                        -f products-api-secrets.yaml \\
                        -f deployment-kind-rendered.yaml \\
                        -f products-api-service.yaml

                    kubectl --context kind-${CLUSTER_NAME} -n ${K8S_NAMESPACE} rollout status \\
                        deployment/${DEPLOYMENT_NAME} --timeout=180s
                """
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

                            sed "s|image: .*|image: ${ECR_REGISTRY}:${IMAGE_TAG}|" \
                                products-api-deployment.yaml > deployment-rendered.yaml

                            kubectl apply \
                                -f products-api-configmap.yaml \
                                -f products-api-secrets.yaml \
                                -f deployment-rendered.yaml \
                                -f products-api-service.yaml

                            kubectl -n ${K8S_NAMESPACE} rollout status \
                                deployment/${DEPLOYMENT_NAME} \
                                --timeout=180s || {
                                    echo 'Rollout fallido. Diagnostico de pods:'
                                    kubectl -n ${K8S_NAMESPACE} get pods -o wide
                                    kubectl get nodes -o wide
                                    kubectl describe nodes
                                    kubectl get pods --all-namespaces -o wide
                                    kubectl -n ${K8S_NAMESPACE} describe deployment/${DEPLOYMENT_NAME}
                                    kubectl -n ${K8S_NAMESPACE} describe pods
                                    kubectl -n ${K8S_NAMESPACE} get events --sort-by=.lastTimestamp
                                    exit 1
                                }
                        """
                    }
                }
            }
        }
    }
    post {
        always {
            sh 'command -v docker >/dev/null 2>&1 && docker logout "${ECR_REGISTRY%%/*}" || true'
        }
    }
}