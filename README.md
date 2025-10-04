# Space Invaders
2025 소프트웨어공학과 소스코드분석 강의  
SpaceInvaders 코드 분석 및 개선

## 참여자
 - 권지민(팀장)
 - 김승현
 - 장진우

## 실행 방법

### 방법 1: 실행 스크립트 사용 (권장)
```bash
# Linux/Mac
./run.sh

# Windows
run.bat
```

### 방법 2: Maven을 사용한 실행
```bash
mvn exec:java
```

### 방법 3: JAR 파일 직접 실행
```bash
# 먼저 빌드
mvn clean package

# JAR 파일 실행
java -jar target/space_invaders-1.0-SNAPSHOT.jar
```

## 시스템 요구사항
- Java 8 이상
- Maven 3.6 이상 (빌드 시)

## 주요 기능
- 로그인 시스템 (Firebase 연동)
- 게임플레이 (Space Invaders)
- 상점 시스템
- 스킬 시스템
- 사용자 통계 관리
