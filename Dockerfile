# --- Etapa 1: build -----------------------------------------------------
# Se usa una imagen con Maven + JDK 21 solo para compilar. Esta capa NO
# viaja a producción; su único propósito es generar el .jar.
FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /app

# Se copian primero el pom.xml y el wrapper para aprovechar la caché de capas
# de Docker: si el código cambia pero las dependencias no, Docker reutiliza
# esta capa en vez de volver a descargar todo Maven Central.
COPY pom.xml .
RUN mvn -B -ntp dependency:go-offline

COPY src ./src
RUN mvn -B -ntp package -DskipTests
# Las pruebas ya corrieron en el pipeline de CI antes de llegar aquí;
# repetirlas en cada build de imagen solo alargaría el proceso.

# --- Etapa 2: runtime -----------------------------------------------------
# Imagen final: solo el JRE (sin compilador, sin Maven, sin código fuente).
# Resultado: una imagen mucho más chica y con menos superficie de ataque
# (principio de mínimo privilegio, en línea con las prácticas OWASP).
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# La aplicación corre con un usuario sin privilegios, nunca como root.
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080

# El healthcheck usa el endpoint de Actuator que ya expone la aplicación.
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s \
  CMD wget -qO- http://localhost:8080/actuator/health | grep -q '"status":"UP"' || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
