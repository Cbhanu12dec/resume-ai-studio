FROM eclipse-temurin:21-jre-alpine
RUN apk add --no-cache texlive texlive-xetex
WORKDIR /app
COPY build/libs/latex-compiler-service-*.jar app.jar
EXPOSE 8083
ENTRYPOINT ["java", "-jar", "app.jar"]
