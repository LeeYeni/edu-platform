# 🌱 1. OpenJDK 17 기반 이미지 사용
FROM eclipse-temurin:17-jdk

# 🏗 2. 작업 디렉토리 설정
WORKDIR /app

# 📁 3. 로컬에서 빌드된 JAR만 복사
COPY backend/build/libs/backend-0.0.1-SNAPSHOT.jar app.jar

# 🚀 4. JAR 실행
CMD ["java", "-jar", "app.jar"]
