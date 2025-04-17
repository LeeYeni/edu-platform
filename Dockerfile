# 🌱 1. OpenJDK 기반 이미지 사용
FROM eclipse-temurin:17-jdk

# 🏗 2. 작업 디렉토리 생성
WORKDIR /app

# 📦 3. Gradle Wrapper와 빌드 파일 복사
COPY gradlew .
COPY gradle gradle
COPY build.gradle .
COPY settings.gradle .

# 📁 4. 전체 프로젝트 복사
COPY . .

# 🛠 5. 실행 권한 부여 및 빌드
RUN chmod +x gradlew
RUN ./gradlew build --no-daemon

# 🚀 6. 실행
CMD ["java", "-jar", "backend/build/libs/*.jar"]
