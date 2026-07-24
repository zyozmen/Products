pipeline {
    agent any

    tools {
        jdk 'JDK 21'
        maven '3.9.x' // Match your configured Maven name
    }

    environment {
        APP_NAME = 'products-api'
        IMAGE_TAG = "${BUILD_NUMBER}"

        // =================================================================
        // CONFIGURACIÓN DEL CONTENEDOR EXISTENTE
        // =================================================================
        MONGO_CONTAINER_NAME = 'mongo' // Nombre exacto en 'docker ps'
        MONGO_NETWORK_NAME   = 'red-productos'            // O el nombre de la red donde vive tu Mongo
        DB_NAME              = 'GrowShop'
        
        // Dejar en blanco ("") si tu Mongo no requiere usuario/password
        MONGO_USER           = 'growShop'
        MONGO_PASSWORD       = 'GrowSh0p'
        MONGO_PORT           = '27017'
        // =================================================================
    }

    stages {
        stage('Checkout') {  
            steps {
                checkout scm
            }
        }

        stage('Verify Java 21') {
            steps {
                echo 'Verificando que Jenkins ejecute Maven con JDK 21...'
                sh 'chmod +x mvnw'
                sh 'java -version'
                sh './mvnw -version'
            }
        }

        stage('Connect Jenkins to Mongo Network') {
            steps {
                script {
                    echo "Garantizando que Jenkins se comunique con la red de MongoDB..."
                    
                    // Conecta el contenedor de Jenkins (jenkins-playground) a la red donde vive Mongo
                    // Si ya están en la misma red, el '|| true' evita que el pipeline falle
                    sh "docker network connect ${MONGO_NETWORK_NAME} jenkins-playground || true"
                }
            }
        }

        stage('Maven Test & Connect to Mongo') {
            steps {
                script {
                    // Construimos la URI dependiendo de si tu BD tiene credenciales o no
                    def mongoUri = ""
                    
                    if (env.MONGO_USER && env.MONGO_PASSWORD) {
                        // URI con autenticación
                        mongoUri = "mongodb://${MONGO_USER}:${MONGO_PASSWORD}@${MONGO_CONTAINER_NAME}:${MONGO_PORT}/${DB_NAME}?authSource=admin"
                    } else {
                        // URI pública/sin autenticación
                        mongoUri = "mongodb://${MONGO_CONTAINER_NAME}:${MONGO_PORT}/${DB_NAME}"
                    }

                    echo "Conectando Spring Boot a MongoDB usando el host: ${MONGO_CONTAINER_NAME}:${MONGO_PORT}"

                    // Ejecuta Maven pasando la propiedad de Spring Data MongoDB
                    sh "./mvnw clean verify -Dspring.data.mongodb.uri='${mongoUri}'"
                }
            }
        }

        stage('Maven Compile & Test') {
            steps {
                echo 'Ejecutando pruebas y compilación con Maven...'
                // Compila, corre pruebas unitarias y genera el .jar omitiendo fallos por falta de DB si no hay mocks
                sh './mvnw clean package'
            }
        }

        stage('Static Analysis (Opcional)') {
            steps {
                // Si agregas SonarQube más adelante, este es el lugar:
                // sh './mvnw sonar:sonar'
                echo 'Fase lista para análisis de código estático.'
            }
        }

        stage('Build Docker Image') {
            steps {
                echo "Construyendo imagen Docker usando el JAR generado..."
                sh "docker build -t ${APP_NAME}:${IMAGE_TAG} ."
            }
        }

        stage('Run API Container (Playground Local)') {
            steps {
                echo "Desplegando la API en el entorno local de pruebas..."
                sh "docker stop ${APP_NAME} || true"
                sh "docker rm ${APP_NAME} || true"
                sh "docker run -d --name ${APP_NAME} -p 8080:8080 ${APP_NAME}:${IMAGE_TAG}"
            }
        }
    }

    post {
        success {
            echo "Pipeline ejecutado con éxito. La API Spring Boot está corriendo en el puerto 8080."
        }
        failure {
            echo "Falló la compilación o las pruebas de Maven en la API."
        }
        always {
            // Limpia el workspace para no llenar el disco de artifacts pesados
            cleanWs()
        }
    }
}