# Historial de Configuración, Debugging y Despliegue de Pipeline CI/CD

## 📌 Resumen General
Este documento contiene la secuencia histórica de ajustes, correcciones de errores, instalaciones del sistema y configuraciones requeridas para lograr la ejecución exitosa del pipeline de Jenkins para el microservicio **`products-service`**, empaquetado en Docker y desplegado en AWS ECS Fargate mediante Terraform.

---

## 🛠️ 1. Modificaciones Realizadas al `Jenkinsfile`

### A. Correcciones de Seguridad y Optimización del Build
* **Aislamiento de Credenciales de MongoDB:** Se eliminó la inyección directa de credenciales en el comando `./mvnw package -D...` para evitar su exposición pública en la tabla de procesos del sistema (`ps aux`). Se migraron a la variable de entorno `SPRING_DATA_MONGODB_URI` mediante el bloque `withEnv`.
* **Eliminación de Redundancia de Compilación:** Se removió la fase de empaquetado JAR duplicado en el agente de Jenkins, delegando la construcción del artefacto final exclusivamente al `Dockerfile` multi-stage.

### B. Corrección de Funciones Inexistentes y DSL
* **Eliminación de DSL Inválido:** Se removió la llamada sintácticamente incorrecta `credentialsId()` dentro del bloque `docker.withRegistry`.
* **Manejo Dinámico del Binario de Terraform:** Se configuró la descarga automática y ejecución del binario portátil de Terraform `./terraform` dentro del workspace en caso de no encontrarse instalado en el sistema host.

### C. Estructura Final Sugerida del `Jenkinsfile`
```groovy
pipeline {
    agent any

    environment {
        AWS_REGION     = 'us-east-2'
        REPO_NAME      = 'products-service'
        ECR_ACCOUNT_ID = '505231787824'
        ECR_URL        = "${ECR_ACCOUNT_ID}.dkr.ecr.${AWS_REGION}[.amazonaws.com/$](https://.amazonaws.com/$){REPO_NAME}"
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

        stage('AWS ECR Login & Build') {
            steps {
                withCredentials([
                    string(credentialsId: 'aws-access-key-id', variable: 'AWS_ACCESS_KEY_ID'),
                    string(credentialsId: 'aws-secret-access-key', variable: 'AWS_SECRET_ACCESS_KEY')
                ]) {
                    sh """
                        aws ecr get-login-password --region ${AWS_REGION} | docker login --username AWS --password-stdin ${ECR_ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com
                        docker build -t ${ECR_URL}:${IMAGE_TAG} .
                        docker push ${ECR_URL}:${IMAGE_TAG}
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
                        export AWS_ACCESS_KEY_ID=${AWS_ACCESS_KEY_ID}
                        export AWS_SECRET_ACCESS_KEY=${AWS_SECRET_ACCESS_KEY}
                        export AWS_DEFAULT_REGION=${AWS_REGION}

                        if ! command -v terraform &> /dev/null; then
                            curl -s -O [https://releases.hashicorp.com/terraform/1.5.7/terraform_1.5.7_linux_amd64.zip](https://releases.hashicorp.com/terraform/1.5.7/terraform_1.5.7_linux_amd64.zip)
                            unzip -q -o terraform_1.5.7_linux_amd64.zip
                            chmod +x terraform
                            TF_CMD="./terraform"
                        else
                            TF_CMD="terraform"
                        fi

                        \$TF_CMD init
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