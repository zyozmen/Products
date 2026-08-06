pipeline {
    agent any

    environment {
        AWS_REGION     = 'us-east-2'
        REPO_NAME      = 'products-service'
        ECR_ACCOUNT_ID = '505231787824'
        ECR_URL        = "${ECR_ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com/${REPO_NAME}"
        IMAGE_TAG      = "build-${BUILD_NUMBER}-${GIT_COMMIT.take(7)}"

        APP_NAME             = 'products-api'
        MONGO_CONTAINER_NAME = 'mongo'
        MONGO_PORT           = '27017'
        DB_NAME              = 'GrowShop'
    }

    stages {

        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Test & Verify') {
            steps {
                withCredentials([usernamePassword(
                        credentialsId: 'MONGO_DB_CREDENTIALS',
                        usernameVariable: 'MONGO_USER',
                        passwordVariable: 'MONGO_PASSWORD'
                )]) {
                    withEnv(["SPRING_DATA_MONGODB_URI=mongodb://${MONGO_USER}:${MONGO_PASSWORD}@${MONGO_CONTAINER_NAME}:${MONGO_PORT}/${DB_NAME}?authSource=admin"]) {
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

        // MODIFICACIÓN 1: Crear infraestructura básica (ECR Backend) antes de Docker
        stage('Terraform Init & Infrastructure Provision') {
            steps {
                withCredentials([
                    string(credentialsId: 'aws-access-key-id', variable: 'AWS_ACCESS_KEY_ID'),
                    string(credentialsId: 'aws-secret-access-key', variable: 'AWS_SECRET_ACCESS_KEY')
                ]) {
                    sh '''
                        export PATH="${WORKSPACE}/.bin:${PATH}"
                        export AWS_ACCESS_KEY_ID=${AWS_ACCESS_KEY_ID}
                        export AWS_SECRET_ACCESS_KEY=${AWS_SECRET_ACCESS_KEY}
                        export AWS_DEFAULT_REGION=${AWS_REGION}

                        BUCKET_NAME="terraform-state-505231787824"
                        DYNAMO_TABLE="terraform-locks"

                        if ! aws s3api head-bucket --bucket "$BUCKET_NAME" 2>/dev/null; then
                            aws s3api create-bucket --bucket "$BUCKET_NAME" --region ${AWS_REGION} --create-bucket-configuration LocationConstraint=${AWS_REGION}
                            aws s3api put-bucket-versioning --bucket "$BUCKET_NAME" --versioning-configuration Status=Enabled
                        fi

                        if ! aws dynamodb describe-table --table-name "$DYNAMO_TABLE" 2>/dev/null; then
                            aws dynamodb create-table --table-name "$DYNAMO_TABLE" --attribute-definitions AttributeName=LockID,AttributeType=S --key-schema AttributeName=LockID,KeyType=HASH --billing-mode PAY_PER_REQUEST --region ${AWS_REGION}
                            aws dynamodb wait table-exists --table-name "$DYNAMO_TABLE" --region ${AWS_REGION}
                        fi

                        terraform init
                        # Target solo al ECR para garantizar que exista antes del push de Docker
                        terraform apply -auto-approve -target=aws_ecr_repository.products_service
                    '''
                }
            }
        }

        stage('Docker Build & Push to ECR') {
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

                        aws ecr get-login-password --region ${AWS_REGION} | docker login --username AWS --password-stdin ${ECR_ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com
                        docker build -t ${REPO_NAME}:${IMAGE_TAG} .
                        docker tag ${REPO_NAME}:${IMAGE_TAG} ${ECR_URL}:${IMAGE_TAG}
                        docker push ${ECR_URL}:${IMAGE_TAG}
                    """
                }
            }
        }

    // MODIFICACIÓN 2: Desplegar el resto de la aplicación pasando el TAG real generado
        stage('Terraform Deploy App') {
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

                        # Pasa el tag exacto construido en el paso de Docker
                        terraform apply -auto-approve -var="image_tag=${IMAGE_TAG}"
                    """
                }
            }
        }
    }

    post {
        always {
            sh "docker rmi ${ECR_URL}:${IMAGE_TAG} || true"
            sh "docker rmi ${REPO_NAME}:${IMAGE_TAG} || true"
            cleanWs()
        }
        failure {
            echo "El pipeline falló en la ejecución. Revisa los logs de los pasos anteriores."
        }
    }
}