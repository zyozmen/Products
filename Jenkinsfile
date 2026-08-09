def getBranchName() {
    def branch = env.BRANCH_NAME ?: env.GIT_BRANCH ?: env.CHANGE_BRANCH ?: ''

    if (!branch) {
        try {
            branch = sh(script: 'git rev-parse --abbrev-ref HEAD', returnStdout: true).trim()
        } catch (Exception e) {
            branch = ''
        }
    }

    return branch
        .replaceFirst(/^origin\//, '')
        .replaceFirst(/^refs\/heads\//, '')
        .replaceFirst(/^\*\//, '')
        .replaceFirst(/^\//, '')
        .tokenize('/')
        .last()
}

def isMainBranch() {
    return getBranchName() == 'main'
}

def isDevelopBranch() {
    return getBranchName() == 'develop'
}

def isMainOrDevelopBranch() {
    return isMainBranch() || isDevelopBranch()
}

def getMongoUri() {
    def branchName = getBranchName()

    if (branchName == 'main') {
        return 'mongodb+srv://zyozgke1992_db_user:GrowShop@products-db-cluster.9wjnrah.mongodb.net/GrowShop?retryWrites=true&w=majority&tls=true&authSource=admin'
    }

    return 'mongodb+srv://zyozgke1992_db_user:GrowShop@products-db-cluster.9wjnrah.mongodb.net/GrowShopDev?retryWrites=true&w=majority&tls=true&authSource=admin'
}

pipeline {
    agent any

    environment {
        AWS_REGION = 'us-east-2'
        REPO_NAME = 'products-service'
        ECR_ACCOUNT_ID = '505231787824'
        ECR_URL = "${ECR_ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com/${REPO_NAME}"
        IMAGE_TAG = "build-${BUILD_NUMBER}-${GIT_COMMIT.take(7)}"

        APP_NAME = 'products-api'
        MONGO_CONTAINER_NAME = 'mongo'
        MONGO_PORT = '27017'
        DB_NAME = 'GrowShop'
        DEV_DB_NAME = 'GrowShopDev'
        K8S_NAMESPACE = 'products'
        K8S_CLUSTER_NAME = 'products-cluster'
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Start test infrastructure') {
            when {
                expression { isDevelopBranch() }
            }
            steps {
                sh '''
                    docker network inspect products-net >/dev/null 2>&1 || docker network create products-net

                    if docker ps -a --filter "name=^/${MONGO_CONTAINER_NAME}$" --format '{{.Names}}' | grep -Fxq "${MONGO_CONTAINER_NAME}"; then
                        if ! docker ps --filter "name=^/${MONGO_CONTAINER_NAME}$" --format '{{.Names}}' | grep -Fxq "${MONGO_CONTAINER_NAME}"; then
                            docker start "${MONGO_CONTAINER_NAME}" >/dev/null
                        fi
                    else
                        docker run -d --name "${MONGO_CONTAINER_NAME}" --network products-net -p "${MONGO_PORT}:${MONGO_PORT}" \
                          -e MONGO_INITDB_ROOT_USERNAME=growShop \
                          -e MONGO_INITDB_ROOT_PASSWORD=GrowSh0p \
                          -e MONGO_INITDB_DATABASE=${DB_NAME} \
                          mongo:6.0
                    fi

                    sleep 10
                    docker ps --filter "name=${MONGO_CONTAINER_NAME}"
                '''
            }
        }

        stage('Test & Verify') {
            when {
                expression { isMainOrDevelopBranch() }
            }
            steps {
                script {
                    echo "Rama detectada: ${getBranchName()}"
                    def mongoUri = getMongoUri()
                    def dbName = isMainBranch() ? env.DB_NAME : env.DEV_DB_NAME
                    withEnv(["SPRING_DATA_MONGODB_URI=${mongoUri}", "SPRING_DATA_MONGODB_DATABASE=${dbName}", "MONGO_DATABASE=${dbName}"]) {
                        sh './mvnw clean test'
                    }
                }
            }
        }

        stage('SonarQube Analysis') {
            steps {
                withCredentials([string(credentialsId: 'SONAR_TOKEN', variable: 'SONAR_TOKEN')]) {
                    withSonarQubeEnv('SonarQube-Server') {
                        sh "mvn org.sonarsource.scanner.maven:sonar-maven-plugin:sonar -Dsonar.token=${SONAR_TOKEN}"
                    }
                }
            }
        }

        stage('Quality Gate') {
            steps {
                timeout(time: 5, unit: 'MINUTES') {
                    script {
                        def qg = waitForQualityGate()
                        if (qg.status != 'OK') {
                            error "Pipeline abortado debido a fallo en el Quality Gate de SonarQube: ${qg.status}"
                        }
                    }
                }
            }
        }

        stage('Local Docker Deploy') {
            when {
                expression { isDevelopBranch() }
            }
            steps {
                sh '''
                    docker network inspect products-net >/dev/null 2>&1 || docker network create products-net

                    if docker ps -a --filter "name=^/${MONGO_CONTAINER_NAME}$" --format '{{.Names}}' | grep -Fxq "${MONGO_CONTAINER_NAME}"; then
                        if ! docker ps --filter "name=^/${MONGO_CONTAINER_NAME}$" --format '{{.Names}}' | grep -Fxq "${MONGO_CONTAINER_NAME}"; then
                            docker start "${MONGO_CONTAINER_NAME}" >/dev/null
                        fi
                    else
                        docker run -d --name "${MONGO_CONTAINER_NAME}" --network products-net -p "${MONGO_PORT}:${MONGO_PORT}" \
                          -e MONGO_INITDB_ROOT_USERNAME=growShop \
                          -e MONGO_INITDB_ROOT_PASSWORD=GrowSh0p \
                          -e MONGO_INITDB_DATABASE=${DB_NAME} \
                          mongo:8.0
                    fi

                    if docker ps -a --filter "name=^/Products-Api$" --format '{{.Names}}' | grep -Fxq 'Products-Api'; then
                        if ! curl -fsS http://localhost:8080/actuator/health >/dev/null 2>&1; then
                            docker rm -f Products-Api >/dev/null 2>&1 || true
                        fi
                    fi

                    if ! docker ps -a --filter "name=^/Products-Api$" --format '{{.Names}}' | grep -Fxq 'Products-Api'; then
                        docker build -t ${APP_NAME}:local .
                        docker run -d --name Products-Api --network products-net -p 8080:8080 \
                          -e SPRING_DATA_MONGODB_URI=mongodb://growShop:GrowSh0p@mongo:27017/GrowShop?authSource=admin \
                          ${APP_NAME}:local
                    fi

                    for i in $(seq 1 20); do
                        if curl -fsS http://localhost:8080/actuator/health >/dev/null 2>&1; then
                            break
                        fi
                        sleep 3
                    done

                    docker ps --filter "name=${MONGO_CONTAINER_NAME}" --filter "name=Products-Api"
                '''
            }
        }

        stage('Docker Build & Push to ECR') {
            when {
                expression { isMainBranch() }
            }
            steps {
                withCredentials([
                    string(credentialsId: 'aws-access-key-id', variable: 'AWS_ACCESS_KEY_ID'),
                    string(credentialsId: 'aws-secret-access-key', variable: 'AWS_SECRET_ACCESS_KEY')
                ]) {
                    sh """
                        export PATH="${WORKSPACE}/.bin:${PATH}"
                        export AWS_ACCESS_KEY_ID=${AWS_ACCESS_KEY_ID}
                        export AWS_SECRET_ACCESS_KEY=${AWS_SECRET_ACCESS_KEY}
                        export AWS_DEFAULT_REGION=${AWS_REGION}

                        aws ecr describe-repositories --repository-names ${REPO_NAME} --region ${AWS_REGION} >/dev/null 2>&1 || \
                        aws ecr create-repository --repository-name ${REPO_NAME} --region ${AWS_REGION} >/dev/null

                        aws ecr get-login-password --region ${AWS_REGION} | docker login --username AWS --password-stdin ${ECR_ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com

                        docker build -t ${REPO_NAME}:${IMAGE_TAG} .
                        docker tag ${REPO_NAME}:${IMAGE_TAG} ${ECR_URL}:${IMAGE_TAG}
                        docker push ${ECR_URL}:${IMAGE_TAG}
                    """
                }
            }
        }

        stage('Terraform Provision Infrastructure') {
            when {
                expression { isMainBranch() }
            }
            steps {
                withCredentials([
                    string(credentialsId: 'aws-access-key-id', variable: 'AWS_ACCESS_KEY_ID'),
                    string(credentialsId: 'aws-secret-access-key', variable: 'AWS_SECRET_ACCESS_KEY')
                ]) {
                    sh """
                        export PATH="${WORKSPACE}/.bin:${PATH}"
                        export AWS_ACCESS_KEY_ID=${AWS_ACCESS_KEY_ID}
                        export AWS_SECRET_ACCESS_KEY=${AWS_SECRET_ACCESS_KEY}
                        export AWS_DEFAULT_REGION=${AWS_REGION}

                        BUCKET_NAME="terraform-state-505231787824"
                        DYNAMO_TABLE="terraform-locks"

                        if ! aws s3api head-bucket --bucket "\$BUCKET_NAME" 2>/dev/null; then
                            aws s3api create-bucket --bucket "\$BUCKET_NAME" --region ${AWS_REGION} --create-bucket-configuration LocationConstraint=${AWS_REGION}
                            aws s3api put-bucket-versioning --bucket "\$BUCKET_NAME" --versioning-configuration Status=Enabled
                        fi

                        if ! aws dynamodb describe-table --table-name "\$DYNAMO_TABLE" 2>/dev/null; then
                            aws dynamodb create-table --table-name "\$DYNAMO_TABLE" --attribute-definitions AttributeName=LockID,AttributeType=S --key-schema AttributeName=LockID,KeyType=HASH --billing-mode PAY_PER_REQUEST --region ${AWS_REGION}
                            aws dynamodb wait table-exists --table-name "\$DYNAMO_TABLE" --region ${AWS_REGION}
                        fi

                        terraform init -input=false
                        terraform apply -auto-approve -var="image_tag=${IMAGE_TAG}"
                    """
                }
            }
        }

        stage('Deploy to EKS') {
            when {
                expression { isMainBranch() }
            }
            steps {
                withCredentials([
                    string(credentialsId: 'aws-access-key-id', variable: 'AWS_ACCESS_KEY_ID'),
                    string(credentialsId: 'aws-secret-access-key', variable: 'AWS_SECRET_ACCESS_KEY')
                ]) {
                    script {
                        def mongoUri = getMongoUri()
                        def secretUri = env.MONGODB_URI ?: mongoUri

                        sh """
                            export PATH="${WORKSPACE}/.bin:${PATH}"
                            export AWS_ACCESS_KEY_ID=${AWS_ACCESS_KEY_ID}
                            export AWS_SECRET_ACCESS_KEY=${AWS_SECRET_ACCESS_KEY}
                            export AWS_DEFAULT_REGION=${AWS_REGION}

                            aws eks update-kubeconfig --name ${K8S_CLUSTER_NAME} --region ${AWS_REGION}

                            kubectl create namespace ${K8S_NAMESPACE} --dry-run=client -o yaml | kubectl apply -f -

                            if [ -f products-api-configmap.yaml ]; then
                                kubectl apply -f products-api-configmap.yaml -n ${K8S_NAMESPACE}
                            fi

                            kubectl create secret generic backend-secrets \
                              --namespace ${K8S_NAMESPACE} \
                              --from-literal=MONGODB_URI="${secretUri}" \
                              --from-literal=SPRING_DATA_MONGODB_URI="${secretUri}" \
                              --from-literal=MONGO_URI="${secretUri}" \
                              --dry-run=client -o yaml | kubectl apply -f -

                            if [ -f products-api-deployment.yaml ]; then
                                sed -i "s|image: .*|image: ${ECR_URL}:${IMAGE_TAG}|g" products-api-deployment.yaml
                                kubectl apply -f products-api-deployment.yaml -n ${K8S_NAMESPACE}
                            else
                                kubectl set image deployment/backend-products-api products-api=${ECR_URL}:${IMAGE_TAG} -n ${K8S_NAMESPACE} || true
                            fi

                            if [ -f products-api-service.yaml ]; then
                                kubectl apply -f products-api-service.yaml -n ${K8S_NAMESPACE}
                            fi

                            kubectl rollout status deployment/backend-products-api -n ${K8S_NAMESPACE} --timeout=300s
                        """
                    }
                }
            }
        }
    }

    post {
        always {
            sh '''
                docker rmi ${ECR_URL}:${IMAGE_TAG} >/dev/null 2>&1 || true
                docker rmi ${REPO_NAME}:${IMAGE_TAG} >/dev/null 2>&1 || true
            '''
            cleanWs()
        }
        failure {
            echo 'El pipeline falló en la ejecución. Revisa los logs de los pasos anteriores.'
        }
    }
}