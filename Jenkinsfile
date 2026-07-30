pipeline {
    agent any

    environment {
        APP_NAME             = 'products-api'
        MONGO_CONTAINER_NAME = 'mongo'
        MONGO_NETWORK_NAME   = 'red-productos'
        DB_NAME              = 'GrowShop'
        MONGO_PORT           = '27017'
        DOCKER_USER          = 'zyozmen'
        APP_VERSION          = "1.0.${BUILD_NUMBER}"
    }

    stages {

        stage('Build, Test & Package') {
            steps {
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
                withCredentials([string(credentialsId: 'SONAR_TOKEN', variable: 'SONAR_TOKEN')]) {
                    // [Certeza] Es OBLIGATORIO envolver la ejecución dentro de withSonarQubeEnv
                    // para que Jenkins inyecte la variable de contexto de la tarea a waitForQualityGate
                    withSonarQubeEnv('SonarQube-Server') { 
                        sh "mvn org.sonarsource.scanner.maven:sonar-maven-plugin:sonar -Dsonar.token=${SONAR_TOKEN}"
                    }
                }
            }
        }

        stage('Quality Gate') {
            steps {
                // Se define un tiempo máximo de espera por si el Webhook de SonarQube no responde
                timeout(time: 4, unit: 'MINUTES') {
                    script {
                        // abortPipeline: true hace que el build de Jenkins falle inmediatamente si SonarQube marca ERROR
                        def qg = waitForQualityGate()
                        if (qg.status != 'OK') {
                            error "Pipeline abortado debido a fallo en el Quality Gate de SonarQube: ${qg.status}"
                        }
                    }
                }
            }
        }

        stage('Build & Push Docker Image') {
            when {
                // Solo se empaqueta si estamos en develop, main o master
                branch pattern: "develop|main|master", comparator: "REGEXP"
            }
            steps {
                script {
                    def gitBranch = env.BRANCH_NAME ?: 'main'
                    def fullImageName = "${DOCKER_USER}/${APP_NAME}"

                    echo "Construyendo imagen: ${fullImageName}:${APP_VERSION} para la rama [${gitBranch}]..."
                    def customImage = docker.build("${fullImageName}:${APP_VERSION}")

                    docker.withRegistry("https://index.docker.io/v1/", 'DOCKER_HUB_CREDENTIALS') {
                        echo "Publicando tag de versión: ${APP_VERSION}..."
                        customImage.push(APP_VERSION)

                        if (gitBranch == 'main' || gitBranch == 'master') {
                            echo "Publicando tag: latest para Producción..."
                            customImage.push('latest')
                        }
                    }
                }
            }
        }

        stage('Deploy to AWS EC2 (Production)') {
            when {
                // Restricción estricta: Solo despliega a AWS si el commit llega a main o master
                branch pattern: "main|master", comparator: "REGEXP"
            }
            steps {
                withCredentials([
                    sshUserPrivateKey(
                        credentialsId: 'SSH_DEPLOY_KEY', 
                        keyFileVariable: 'SSH_KEY', 
                        usernameVariable: 'SSH_USER'
                    ),
                    usernamePassword(
                        credentialsId: 'DOCKER_HUB_CREDENTIALS', 
                        usernameVariable: 'DOCKER_USER', 
                        passwordVariable: 'DOCKER_PASS'
                    )
                ]) {
                    script {
                        def awsIp = "18.224.29.18"
                        // En producción desplegamos la imagen etiquetada con la versión de este build
                        def fullImageTag = "${DOCKER_USER}/${APP_NAME}:${APP_VERSION}"
                        
                        sh """
                            ssh -i \${SSH_KEY} -o StrictHostKeyChecking=no \${SSH_USER}@${awsIp} \
                            DOCKER_USER='${DOCKER_USER}' \
                            DOCKER_PASS='${DOCKER_PASS}' \
                            APP_NAME='${APP_NAME}' \
                            IMAGE_TAG='${fullImageTag}' \
                            NETWORK_NAME='${MONGO_NETWORK_NAME}' \
                            'bash -s' << 'EOF'
                                set -e
                                ENV_FILE="/etc/products-api/.env"

                                echo "--> Autenticando en Docker Hub..."
                                echo "\$DOCKER_PASS" | docker login -u "\$DOCKER_USER" --password-stdin

                                echo "--> Descargando imagen desde Docker Hub: \$IMAGE_TAG"
                                docker pull "\$IMAGE_TAG"

                                echo "--> Verificando red aislada..."
                                docker network inspect "\$NETWORK_NAME" >/dev/null 2>&1 || docker network create "\$NETWORK_NAME"

                                echo "--> Removiendo contenedor anterior..."
                                if [ \$(docker ps -aq -f name=^\${APP_NAME}\$) ]; then
                                    docker stop "\$APP_NAME" || true
                                    docker rm "\$APP_NAME" || true
                                fi

                                echo "--> Desplegando contenedor en AWS..."
                                docker run -d \\
                                    --name "\$APP_NAME" \\
                                    --restart unless-stopped \\
                                    --network "\$NETWORK_NAME" \\
                                    --env-file "\$ENV_FILE" \\
                                    -p 8080:8080 \\
                                    "\$IMAGE_TAG"

                                echo "--> Limpiando imágenes en desuso..."
                                docker image prune -f

                                echo "--> Verificando estado del contenedor..."
                                sleep 5
                                docker ps -f name="\$APP_NAME"
EOF
"""
                    }
                }
            }
        }

    }

    post {
        always {
            // Buenas prácticas: limpiar imágenes intermedias del host de Jenkins
            sh 'docker image prune -f || true'
        }
        failure {
            echo "Falló el pipeline en la rama: ${env.BRANCH_NAME}"
        }
    }
}