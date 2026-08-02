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

        stage('Verify & Install Tools') {
            steps {
                sh '''
                    mkdir -p .bin
                    export PATH="${WORKSPACE}/.bin:${PATH}"

                    # 1. Verificar o instalar AWS CLI v2
                    if ! command -v aws &> /dev/null; then
                        echo "➜ AWS CLI no encontrado. Instalando localmente en el workspace..."
                        curl -s "https://awscli.amazonaws.com/awscli-exe-linux-x86_64.zip" -o "awscliv2.zip"
                        unzip -q -o awscliv2.zip
                        ./aws/install --bin-dir "${WORKSPACE}/.bin" --install-dir "${WORKSPACE}/.aws-cli" --update
                        rm -rf awscliv2.zip aws/
                    else
                        echo "✓ AWS CLI ya está instalado: $(aws --version)"
                    fi

                    # 2. Verificar o instalar Terraform
                    if ! command -v terraform &> /dev/null; then
                        echo "➜ Terraform no encontrado. Descargando ejecutable portátil..."
                        curl -s -O https://releases.hashicorp.com/terraform/1.5.7/terraform_1.5.7_linux_amd64.zip
                        unzip -q -o terraform_1.5.7_linux_amd64.zip -d "${WORKSPACE}/.bin/"
                        chmod +x "${WORKSPACE}/.bin/terraform"
                        rm -f terraform_1.5.7_linux_amd64.zip
                    else
                        echo "✓ Terraform ya está instalado: $(terraform --version)"
                    fi
                '''
            }
        }

        stage('Terraform Provision & Deploy') {
            steps {
                withCredentials([
                    string(credentialsId: 'aws-access-key-id', variable: 'AWS_ACCESS_KEY_ID'),
                    string(credentialsId: 'aws-secret-access-key', variable: 'AWS_SECRET_ACCESS_KEY')
                ]) {
                    sh '''
                        # Asegurar que las herramientas instaladas en .bin estén en el PATH
                        export PATH="${WORKSPACE}/.bin:${PATH}"
                        export AWS_ACCESS_KEY_ID=${AWS_ACCESS_KEY_ID}
                        export AWS_SECRET_ACCESS_KEY=${AWS_SECRET_ACCESS_KEY}
                        export AWS_DEFAULT_REGION=${AWS_REGION}

                        BUCKET_NAME="terraform-state-505231787824"
                        DYNAMO_TABLE="terraform-locks"

                        echo "=== 1. Verificando/Creando Backend Remoto en AWS ==="
                        
                        # Crear Bucket S3 si no existe
                        if ! aws s3api head-bucket --bucket "$BUCKET_NAME" 2>/dev/null; then
                            echo "Bucket $BUCKET_NAME no existe. Creando..."
                            aws s3api create-bucket \
                                --bucket "$BUCKET_NAME" \
                                --region ${AWS_REGION} \
                                --create-bucket-configuration LocationConstraint=${AWS_REGION}
                            
                            aws s3api put-bucket-versioning \
                                --bucket "$BUCKET_NAME" \
                                --versioning-configuration Status=Enabled
                        else
                            echo "✓ Bucket $BUCKET_NAME ya existe."
                        fi

                        # Crear Tabla DynamoDB si no existe
                        if ! aws dynamodb describe-table --table-name "$DYNAMO_TABLE" 2>/dev/null; then
                            echo "Tabla DynamoDB $DYNAMO_TABLE no existe. Creando..."
                            aws dynamodb create-table \
                                --table-name "$DYNAMO_TABLE" \
                                --attribute-definitions AttributeName=LockID,AttributeType=S \
                                --key-schema AttributeName=LockID,KeyType=HASH \
                                --billing-mode PAY_PER_REQUEST \
                                --region ${AWS_REGION}

                            aws dynamodb wait table-exists --table-name "$DYNAMO_TABLE" --region ${AWS_REGION}
                        else
                            echo "✓ Tabla $DYNAMO_TABLE ya existe."
                        fi

                        echo "=== 2. Ejecutando Terraform ==="
                        terraform init
                        terraform apply -auto-approve -var="image_tag=${IMAGE_TAG}"
                    '''
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