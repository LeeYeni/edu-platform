# 🌱 1. OpenJDK 17 기반 이미지 사용
FROM eclipse-temurin:17-jdk

# 🏗 2. 컨테이너 내 작업 디렉토리 설정
WORKDIR /app

# 📦 3. Gradle Wrapper 및 설정 파일 복사
COPY gradlew .
COPY gradle gradle
COPY build.gradle .
COPY settings.gradle .

# 📁 4. 전체 프로젝트 복사
COPY . .

# 🛠 5. gradlew 실행 권한 부여 및 빌드 (테스트 제외)
RUN chmod +x gradlew
RUN ./gradlew clean build -x test --no-daemon

# 👁️‍🗨️ 6. 빌드된 JAR 파일이 있는지 확인 (Render 로그에서 확인용)
RUN ls -al backend/build/libs

# 🚀 7. 애플리케이션 실행
CMD ["java", "-jar", "backend/build/libs/backend-0.0.1-SNAPSHOT.jar"]
