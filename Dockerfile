# 🌱 1. OpenJDK 17 기반 이미지 사용
FROM eclipse-temurin:17-jdk

# 🏗 2. 작업 디렉토리 설정
WORKDIR /app

# 📦 3. 프로젝트 전체 복사
COPY . .

# 🔨 4. Gradle로 백엔드 빌드 수행
RUN ./gradlew :backend:build -x test

# 📁 5. 빌드된 JAR 복사
RUN cp backend/build/libs/*.jar app.jar

# 🚀 6. JAR 실행
CMD ["java", "-jar", "app.jar"]
