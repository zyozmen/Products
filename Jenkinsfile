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

        stage('Static Analysis & Quality Gate') {
            steps {
                withCredentials([string(credentialsId: 'SONAR_TOKEN', variable: 'SONAR_TOKEN')]) {
            // Cambia comillas simples ' por comillas dobles "
            sh "mvn org.sonarsource.scanner.maven:sonar-maven-plugin:sonar -Dsonar.token=${SONAR_TOKEN}"
            }
        }
        }

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
    }
}