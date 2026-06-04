#Образ JDK 21 на базе Alpine Linux для сборки проекта
FROM eclipse-temurin:21-jdk-alpine AS compile-stage

WORKDIR /build

#Копирование файлов сборщика Maven и исходного кода проекта
COPY mvnw .
COPY mvnw.cmd .
COPY .mvn .mvn
COPY pom.xml .
COPY src src

# Сбор готового исполняемого .jar файла, отключая тесты
RUN ./mvnw clean package -DskipTests

#Использование минимального базового образа (открытый JRE Alpine)
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

#Отсутствие лишних пакетов: в финальный слой переносим только один собранный jar-файл
COPY --from=compile-stage /build/target/myapp-0.0.1-SNAPSHOT.jar app.jar

#Запуск от непривилегированного пользователя.
RUN addgroup -S securitygroup && adduser -S securityuser -G securitygroup
RUN chown -R securityuser:securitygroup /app
USER securityuser

#Закрытие ненужных портов.
EXPOSE 8085

#Запуск приложения с флагами оптимизации памяти для виртуальной машины Java
CMD ["java", "-XX:+UseG1GC", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]