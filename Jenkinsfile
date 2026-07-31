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
                script {
                    // El SDK de Jenkins gestiona la autenticación con ECR sin invocar la AWS CLI directamente
                        docker.withRegistry("https://${ECR_URL}", "ecr:${AWS_REGION}:${credentialsId('aws-access-key-id')}") {
                            sh "docker build -t ${ECR_URL}:${IMAGE_TAG} ."
                            sh "docker push ${ECR_URL}:${IMAGE_TAG}"
                        }
                    }
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
                        terraform init
                        terraform apply -auto-approve -var="image_tag=${IMAGE_TAG}"
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