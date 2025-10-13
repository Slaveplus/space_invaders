#!/bin/bash

# Space Invaders 게임 실행 스크립트

echo "Space Invaders 게임을 시작합니다..."

# JAR 파일이 존재하는지 확인
if [ ! -f "target/space_invaders-1.0-SNAPSHOT.jar" ]; then
    echo "JAR 파일이 없습니다. 먼저 빌드합니다..."
    mvn clean package -DskipTests
    if [ $? -ne 0 ]; then
        echo "빌드에 실패했습니다."
        exit 1
    fi
fi

# 게임 실행
echo "게임을 실행합니다..."
java -jar target/space_invaders-1.0-SNAPSHOT.jar
