pipeline {
    agent any

    environment {
        APP_NAME = 'products-api'
        IMAGE_TAG = "${BUILD_NUMBER}"
        MONGO_CONTAINER_NAME = 'mongo'
        MONGO_NETWORK_NAME = 'red-productos'
        DB_NAME = 'GrowShop'
        MONGO_PORT = '27017'
    }

    stages {


        stage('Build, Test & Package') {
            steps {
                // El bloque withCredentials inyecta las variables de forma encriptada
                withCredentials([usernamePassword(
                        credentialsId: 'MONGO_DB_CREDENTIALS',
                        usernameVariable: 'MONGO_USER',
                        passwordVariable: 'MONGO_PASSWORD'
                )]) {
                    script {
                        def mongoUri = "mongodb://${MONGO_USER}:${MONGO_PASSWORD}@${MONGO_CONTAINER_NAME}:${MONGO_PORT}/${DB_NAME}?authSource=admin"
                        sh "./mvnw clean package -Dspring.data.mongodb.uri='${mongoUri}'"
                    }
                }
            }
        }

         stage('SonarQube Analysis') {
            steps {
                // Asegúrate de usar comillas DOBLES "" en el comando sh
                withCredentials([string(credentialsId: 'SONAR_TOKEN', variable: 'SONAR_TOKEN')]) {
                    sh "mvn org.sonarsource.scanner.maven:sonar-maven-plugin:sonar -Dsonar.token=${SONAR_TOKEN}"
                }
            }
        }

        stage('Build & Push Docker Image') {
            steps {
                script {
                    // 1. Definir estrategia de etiquetado (Semantic Versioning)
                    // Usa la rama y el número de build. Ej: 'main-15' o 'develop-15'
                    def gitBranch = env.BRANCH_NAME ?: 'main'
                    def imageVersion = "1.0.${BUILD_NUMBER}"
                    def fullImageName = "${DOCKER_USER}/${APP_NAME}"

                    echo "Construyendo imagen: ${fullImageName}:${imageVersion}..."

                    // 2. Construir la imagen localmente
                    def customImage = docker.build("${fullImageName}:${imageVersion}")

                    // 3. Autenticarse en Docker Hub y hacer Push de las etiquetas
                    docker.withRegistry("https://${DOCKER_REGISTRY}", 'DOCKER_HUB_CREDENTIALS') {
                        
                        // Sube la versión específica (ej. products-api:1.0.15)
                        echo "Publicando tag de versión: ${imageVersion}..."
                        customImage.push(imageVersion)

                        // Si estamos en la rama principal, actualiza también el tag 'latest'
                        if (gitBranch == 'main' || gitBranch == 'master') {
                            echo "Publicando tag: latest..."
                            customImage.push('latest')
                        }
                    }
                }
            }
        }
    }
}