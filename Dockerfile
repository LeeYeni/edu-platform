# 🌱 1. OpenJDK 17 기반 이미지 사용
FROM eclipse-temurin:17-jdk

# 🏗 2. 컨테이너 내 작업 디렉토리 설정
WORKDIR /app

# 📁 3. 필요한 jar 파일만 복사
COPY backend/build/libs/backend-0.0.1-SNAPSHOT.jar app.jar

# 🚀 4. JAR 파일 실행
CMD ["java", "-jar", "app.jar"]
