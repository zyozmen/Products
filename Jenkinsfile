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
                    // [Certeza] Inyección vía variable de entorno para evitar exposición en 'ps aux'
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

        stage('AWS ECR Login & Build') {
            steps {
                withCredentials([
                    string(credentialsId: 'aws-access-key-id', variable: 'AWS_ACCESS_KEY_ID'),
                    string(credentialsId: 'aws-secret-access-key', variable: 'AWS_SECRET_ACCESS_KEY')
                ]) {
                    sh """
                        # --- PASO DE DIAGNÓSTICO ---
                        echo "=== USUARIO ACTUAL ==="
                        whoami

                        echo "=== PATH DEL SISTEMA ==="
                        echo \$PATH

                        echo "=== BUSCANDO EL EJECUTABLE AWS ==="
                        which aws || find / -name aws -type f 2>/dev/null || echo "AWS NO ENCONTRADO EN NINGUNA PARTE"
                        # ---------------------------
                    """
                }
            }
        }

        stage('Terraform Provision & Deploy') {
            steps {
                withCredentials([
                    string(credentialsId: 'aws-access-key-id', variable: 'AWS_ACCESS_KEY_ID'),
                    string(credentialsId: 'aws-secret-access-key', variable: 'AWS_SECRET_ACCESS_KEY')
                ]) {
                    sh """
                        # Configuración de credenciales de AWS en variables de entorno para Terraform
                        export AWS_ACCESS_KEY_ID=${AWS_ACCESS_KEY_ID}
                        export AWS_SECRET_ACCESS_KEY=${AWS_SECRET_ACCESS_KEY}
                        export AWS_DEFAULT_REGION=${AWS_REGION}


                        BUCKET_NAME="terraform-state-505231787824"
                        DYNAMO_TABLE="terraform-locks"

                        # 1. Crear Bucket S3 si no existe
                        if ! aws s3api head-bucket --bucket "\$BUCKET_NAME" 2>/dev/null; then
                            echo "Bucket \$BUCKET_NAME no existe. Creando..."
                            aws s3api create-bucket \
                                --bucket "\$BUCKET_NAME" \
                                --region ${AWS_REGION} \
                                --create-bucket-configuration LocationConstraint=${AWS_REGION}
                            
                            aws s3api put-bucket-versioning \
                                --bucket "\$BUCKET_NAME" \
                                --versioning-configuration Status=Enabled
                        fi

                        # 2. Crear Tabla DynamoDB si no existe
                        if ! aws dynamodb describe-table --table-name "\$DYNAMO_TABLE" 2>/dev/null; then
                            echo "Tabla DynamoDB \$DYNAMO_TABLE no existe. Creando..."
                            aws dynamodb create-table \
                                --table-name "\$DYNAMO_TABLE" \
                                --attribute-definitions AttributeName=LockID,AttributeType=S \
                                --key-schema AttributeName=LockID,KeyType=HASH \
                                --billing-mode PAY_PER_REQUEST \
                                --region ${AWS_REGION}

                            aws dynamodb wait table-exists --table-name "\$DYNAMO_TABLE" --region ${AWS_REGION}
                        fi

                        # 3. Garantizar binario de Terraform local en el workspace si el sistema no lo tiene
                        if ! command -v terraform &> /dev/null; then
                            echo "--> Terraform no encontrado en el PATH. Descargando binario ejecutable portátil..."
                            curl -s -O https://releases.hashicorp.com/terraform/1.5.7/terraform_1.5.7_linux_amd64.zip
                            unzip -q -o terraform_1.5.7_linux_amd64.zip
                            chmod +x terraform
                            TF_CMD="./terraform"
                        else
                            TF_CMD="terraform"
                        fi

                        # 4. Ejecutar comandos de infraestructura
                        echo "--> Ejecutando Terraform init..."
                        \$TF_CMD init

                        echo "--> Aplicando cambios en AWS ECS con la imagen: ${IMAGE_TAG}..."
                        \$TF_CMD apply -auto-approve -var="image_tag=${IMAGE_TAG}"
                    """
                }
            }
        }

    }

    post {
        always {
            sh "docker rmi ${ECR_URL}:${IMAGE_TAG} || true"
            cleanWs()
        }
        failure {
            echo "El pipeline falló en la ejecución. Revisa los logs de los pasos anteriores."
        }
    }
}