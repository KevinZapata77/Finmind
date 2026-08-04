# ---------------------------------------------------------------------------
# FinMind - imagen de la API
#
# Construccion en dos etapas:
#   1) compila con Maven sobre JDK 21
#   2) copia solo el .jar a una imagen con JRE, sin Maven ni codigo fuente
#
# La imagen final no incluye el JDK ni el codigo, solo lo necesario para correr.
# ---------------------------------------------------------------------------

# ------------------------------- etapa 1: build ----------------------------
FROM maven:3.9.9-eclipse-temurin-21 AS build

WORKDIR /build

# Se copia primero el pom para que Docker cachee las dependencias:
# si el pom no cambia, no vuelve a descargarlas en cada build.
COPY pom.xml .
RUN mvn -B dependency:go-offline

COPY src ./src
RUN mvn -B clean package -DskipTests

# ------------------------------ etapa 2: runtime ---------------------------
FROM eclipse-temurin:21-jre-alpine AS runtime

# SEG-09, minimo privilegio: la aplicacion no corre como root.
RUN addgroup -S finmind && adduser -S finmind -G finmind

WORKDIR /app
COPY --from=build /build/target/*.jar app.jar
RUN chown -R finmind:finmind /app

USER finmind

EXPOSE 8080

# MaxRAMPercentage: la JVM respeta el limite de memoria del contenedor.
# Sin esto, en un contenedor pequeno la JVM asume mas RAM de la que tiene
# y el proceso muere por OOM.
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75 -XX:+UseSerialGC"

# Las credenciales NO van en la imagen. Se inyectan como variables de entorno
# al ejecutar el contenedor (ARQ-03, SEG-02).
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
