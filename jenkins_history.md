# Histórico de Resolución: Pipeline CI/CD Jenkins + SonarQube + Maven

## 1. Resumen Ejecutivo
Durante el despliegue e integración del pipeline de CI/CD para el proyecto Backend (`products`), se presentaron una serie de errores en cadena relacionados con sintaxis de Groovy, resolución de plugins de Maven, autenticación con SonarQube, red entre contenedores Docker y el orden de ejecución en el ciclo de vida de compilación.

---

## 2. Errores Identificados y Soluciones Aplicadas

### Paso 1: Error de Sintaxis en Jenkinsfile (Groovy Compilation Failure)
* **Síntoma:** `org.codehaus.groovy.control.MultipleCompilationErrorsException: expecting '}', found '' @ line 49`
* **Causa:** El script de Jenkins contenía un desbalance en el cierre de llaves (`}`) al final del archivo.
* **Solución:** Corrección de la estructura de bloques (`stage`, `steps`) en el `Jenkinsfile`.

---

### Paso 2: Plugin de SonarQube no encontrado en Maven
* **Síntoma:** `[ERROR] No plugin found for prefix 'sonar' in the current project...`
* **Causa:** Maven no lograba mapear el comando corto `sonar:sonar` al plugin correspondiente sin tenerlo definido en los `pluginGroups` o en el `pom.xml`.
* **Solución:** Se invocó el plugin utilizando su nombre completo (FQDN):
  `org.sonarsource.scanner.maven:sonar-maven-plugin:sonar`

---

### Paso 3: Error de Red / Puerto Mapeado en Docker (`Connection refused`)
* **Síntoma:** `Failed to connect to sonarqube/172.19.0.3:8070: Connection refused`
* **Causa:** Se intentó conectar al puerto `8070` (mapeado hacia el host) dentro de la red privada de Docker.
* **Solución:** Ajuste del host para apuntar al puerto de escucha interno del contenedor de SonarQube (`9000`):
  `-Dsonar.host.url=http://sonarqube:9000`

---

### Paso 4: Fallo de Autenticación (`Not authorized` / Inyección de Variables)
* **Síntoma:** `Not authorized. Please check the user token in the property 'sonar.token'`
* **Causa:**
    1. Uso de comillas simples (`'`) en el comando `sh` de Jenkins, lo que evitaba que Groovy evaluara la variable `$SONAR_TOKEN`.
    2. Incompatibilidad de credenciales o formato al pasar la variable.
* **Solución:**
    * Se configuraron las credenciales de tipo **Secret text** en Jenkins con ID `SONAR_TOKEN`.
    * Se cambiaron las comillas simples por comillas dobles (`"`) en el bloque `sh` para permitir la interpolación de variables.

---

### Paso 5: Falta de Bytecode / `.class` para Análisis Estático
* **Síntoma:** `Your project contains .java files, please provide compiled classes with sonar.java.binaries property...`
* **Causa:** La etapa de SonarQube se estaba ejecutando **antes** de la etapa de compilación (`Build`), por lo que la carpeta `target/classes` no existía al analizar el código Java.
* **Solución:** Reordenamiento de las etapas o inclusión de la fase de compilación previa al análisis:
  `mvn clean compile org.sonarsource.scanner.maven:sonar-maven-plugin:sonar`

---

## 3. Configuración Final Exitosa (`Jenkinsfile`)

```groovy
pipeline {
    agent any

    stages {
        stage('Build & Compile') {
            steps {
                sh 'mvn clean compile'
            }
        }

        stage('SonarQube Analysis') {
            steps {
                withCredentials([string(credentialsId: 'SONAR_TOKEN', variable: 'SONAR_TOKEN')]) {
                    sh "mvn org.sonarsource.scanner.maven:sonar-maven-plugin:sonar -Dsonar.host.url=http://sonarqube:9000 -Dsonar.token=${SONAR_TOKEN}"
                }
            }
        }
    }
}