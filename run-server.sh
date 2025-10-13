#!/bin/bash

# GameServer 실행 스크립트

echo "GameServer를 시작합니다..."

# 포트 설정 (기본값: 7777)
PORT=${1:-7777}
echo "포트: $PORT"

# JAR 파일이 존재하는지 확인
if [ ! -f "target/space_invaders-1.0-SNAPSHOT.jar" ]; then
    echo "JAR 파일이 없습니다. 먼저 빌드합니다..."
    mvn clean package -DskipTests
    if [ $? -ne 0 ]; then
        echo "빌드에 실패했습니다."
        exit 1
    fi
fi

# JAR 파일을 사용하여 GameServer만 실행
echo "GameServer를 실행합니다..."
java -cp target/space_invaders-1.0-SNAPSHOT.jar org.newdawn.spaceinvaders.server.GameServer $PORT
