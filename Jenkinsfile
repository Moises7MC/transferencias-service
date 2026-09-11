// Pipeline declarativo de Jenkins para transferencias-service.
//
// "Declarativo" significa que describes QUÉ etapas quieres (bloque pipeline { stages { ... } }),
// y Jenkins se encarga del CÓMO ejecutarlo, con una sintaxis fija y validada.
// La alternativa es un pipeline "scripted" (Groovy puro, más flexible pero más difícil de leer).

pipeline {

    // "agent any" = este pipeline puede correr en cualquier agente disponible de Jenkins.
    // En un equipo real, aquí se suele fijar una etiqueta ("agent { label 'linux' }")
    // para elegir un agente con Docker, o "agent { docker { image 'maven:3.9-eclipse-temurin-21' } }"
    // para que cada build corra dentro de un contenedor limpio y descartable.
    agent any

    // Variables de entorno disponibles en todas las etapas.
    environment {
        // La app usa el wrapper de Maven (./mvnw), así que no depende de que el
        // agente de Jenkins tenga Maven instalado: el propio proyecto lo descarga.
        IMAGE_NAME = "transferencias-service"
        IMAGE_TAG  = "${env.BUILD_NUMBER}"
    }

    // Ajustes generales del pipeline.
    options {
        timestamps()                              // cada línea del log lleva su hora
        buildDiscarder(logRotator(numToKeepStr: '10')) // no acumula builds viejos para siempre
        timeout(time: 20, unit: 'MINUTES')        // si algo se cuelga, Jenkins lo corta
    }

    stages {

        stage('Checkout') {
            steps {
                echo "Descargando el código fuente..."
                checkout scm
            }
        }

        stage('Compilar') {
            steps {
                echo "Compilando con Maven..."
                sh './mvnw -B -ntp clean compile'
            }
        }

        stage('Pruebas unitarias y de integración') {
            steps {
                echo "Ejecutando las 12 pruebas (JUnit 5 + Mockito + MockMvc)..."
                sh './mvnw -B -ntp test'
            }
            post {
                // "always" corre pase lo que pase: si las pruebas fallan, igual
                // queremos ver el reporte para saber CUÁL falló, no solo que falló.
                always {
                    junit testResults: 'target/surefire-reports/*.xml', allowEmptyResults: true
                }
            }
        }

        stage('Análisis de calidad (SonarQube)') {
            when {
                // Esta etapa solo se activa si existe la credencial del token de Sonar.
                // Así el Jenkinsfile funciona igual en una máquina que no tiene Sonar configurado.
                expression { return env.SONAR_TOKEN != null }
            }
            steps {
                echo "Analizando calidad de código con SonarQube/SonarCloud..."
                sh './mvnw -B -ntp sonar:sonar -Dsonar.token=$SONAR_TOKEN'
            }
        }

        stage('Empaquetar') {
            steps {
                echo "Generando el .jar ejecutable..."
                sh './mvnw -B -ntp package -DskipTests'
                archiveArtifacts artifacts: 'target/*.jar', fingerprint: true
            }
        }

        stage('Verificar disponibilidad de Docker') {
            // El agente que ejecuta este pipeline necesita el cliente docker y acceso
            // al socket del host (Docker-outside-of-Docker) para poder construir imágenes.
            // Se verifica en vez de asumir, para que el pipeline falle con un mensaje claro
            // -o simplemente salte las etapas de Docker- en vez de romperse a ciegas.
            steps {
                script {
                    env.DOCKER_AVAILABLE = (sh(script: 'command -v docker', returnStatus: true) == 0) ? 'true' : 'false'
                    echo "Docker disponible en este agente: ${env.DOCKER_AVAILABLE}"
                }
            }
        }

        stage('Construir imagen Docker') {
            when {
                environment name: 'DOCKER_AVAILABLE', value: 'true'
            }
            steps {
                echo "Construyendo la imagen ${IMAGE_NAME}:${IMAGE_TAG}..."
                sh "docker build -t ${IMAGE_NAME}:${IMAGE_TAG} -t ${IMAGE_NAME}:latest ."
            }
        }

        stage('Desplegar a staging') {
            when {
                allOf {
                    branch 'main' // solo se despliega automáticamente desde main
                    environment name: 'DOCKER_AVAILABLE', value: 'true'
                }
            }
            steps {
                echo "Desplegando el contenedor a un entorno de prueba..."
                sh '''
                    docker rm -f transferencias-staging || true
                    docker run -d --name transferencias-staging \
                        --network transferencias-service_default \
                        -e SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/transferencias \
                        -e SPRING_DATASOURCE_USERNAME=transferencias \
                        -e SPRING_DATASOURCE_PASSWORD=transferencias \
                        -e SPRING_DOCKER_COMPOSE_ENABLED=false \
                        -p 8082:8080 \
                        ${IMAGE_NAME}:latest
                '''
            }
        }

        stage('Smoke test') {
            when {
                allOf {
                    branch 'main'
                    environment name: 'DOCKER_AVAILABLE', value: 'true'
                }
            }
            steps {
                echo "Verificando que el servicio responde tras el despliegue..."
                // Reintenta varias veces porque el contenedor tarda unos segundos en levantar.
                sh '''
                    for i in $(seq 1 12); do
                        CODE=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:8082/actuator/health || echo "000")
                        echo "Intento $i: HTTP $CODE"
                        [ "$CODE" = "200" ] && exit 0
                        sleep 5
                    done
                    echo "El servicio no respondió 200 tras el despliegue"
                    exit 1
                '''
            }
        }
    }

    post {
        success {
            echo "✅ Pipeline completo: build ${env.BUILD_NUMBER} listo y desplegado."
        }
        failure {
            // Aquí, en un equipo real, se conectaría una notificación a Slack o correo.
            echo "❌ El pipeline falló. Revisar el log de la etapa marcada en rojo."
        }
        always {
            // Limpieza: nunca dejar el workspace de Jenkins acumulando archivos de builds viejos.
            cleanWs()
        }
    }
}
