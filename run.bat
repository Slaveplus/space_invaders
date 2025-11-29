echo Space Invaders 게임을 시작합니다...

REM JAR 파일이 존재하는지 확인
if not exist "target\space_invaders-1.0-SNAPSHOT.jar" (
    echo JAR 파일이 없습니다. 먼저 빌드합니다...
    mvn clean package
    if errorlevel 1 (
        echo 빌드에 실패했습니다.
        pause
        exit /b 1
    )
)

REM 게임 실행
echo 게임을 실행합니다...
java -jar target\space_invaders-1.0-SNAPSHOT.jar
