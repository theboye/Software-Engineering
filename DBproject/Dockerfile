# Используем официальный образ OpenJDK
FROM openjdk:17-slim

# Устанавливаем Maven
RUN apt-get update && apt-get install -y maven

WORKDIR /app

# Копируем исходный код
COPY src /app/src
COPY pom.xml /app/

# Собираем приложение
RUN mvn clean package -DskipTests

EXPOSE 8080

CMD ["java", "-jar", "target/telegram-bot-0.0.1-SNAPSHOT.jar"]