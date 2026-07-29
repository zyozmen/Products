pipeline {
    agent any

    environment {
        APP_NAME           = 'products-api'
        MONGO_CONTAINER_NAME = 'mongo'
        MONGO_NETWORK_NAME = 'red-productos'
        DB_NAME            = 'GrowShop'
        MONGO_PORT         = '27017'
        DOCKER_USER        = 'zyozmen'
        // Asegura consistencia de versión en todo el pipeline
        APP_VERSION        = "1.0.${BUILD_NUMBER}"
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
                    sh "mvn org.sonarsource.scanner.maven:sonar-maven-plugin:sonar -Dsonar.token=${SONAR_TOKEN}"
                }
            }
        }

        stage('Build & Push Docker Image') {
            steps {
                script {
                    def gitBranch = env.BRANCH_NAME ?: 'main'
                    def fullImageName = "${DOCKER_USER}/${APP_NAME}"

                    echo "Construyendo imagen: ${fullImageName}:${APP_VERSION}..."
                    def customImage = docker.build("${fullImageName}:${APP_VERSION}")

                    // Revisa que el ID 'DOCKER_HUB_CREDENTIALS' sea el correcto en tu Jenkins
                    docker.withRegistry("https://index.docker.io/v1/", 'DOCKER_HUB_CREDENTIALS') {
                        echo "Publicando tag de versión: ${APP_VERSION}..."
                        customImage.push(APP_VERSION)

                        if (gitBranch == 'main' || gitBranch == 'master') {
                            echo "Publicando tag: latest..."
                            customImage.push('latest')
                        }
                    }
                }
            }
        }

        stage('Deploy to AWS EC2') {
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
}