# GEMINI.md

## 프로젝트 개요

이 프로젝트는 UI에 Swing 라이브러리를 사용하여 Java로 작성된 **Space Invaders** 게임입니다. 로그인 시스템, 상점, 스킬 시스템, 멀티플레이어 기능과 같은 추가 기능을 갖춘 고전적인 아케이드 스타일 슈팅 게임입니다. 프로젝트는 Maven으로 빌드되었으며 데이터베이스로 Firebase를 사용합니다.

게임은 로그인 화면, 메인 메뉴, 게임 자체와 같은 다양한 게임 상태에 대한 다른 화면(`Canvas` 객체로 구현됨)을 관리하는 메인 애플리케이션 프레임(`SpaceInvadersApp.java`)을 중심으로 구성됩니다. 핵심 게임 플레이 로직은 게임 루프, 엔티티 관리 및 사용자 입력을 조정하는 `Game` 클래스를 포함하는 `gameplay` 패키지에 포함되어 있습니다.

## 빌드 및 실행

프로젝트는 Maven을 사용하여 빌드하고 실행할 수 있습니다.

### 게임 실행

**방법 1: 실행 스크립트 (권장)**

```bash
# Linux/Mac
./run.sh

# Windows
run.bat
```

**방법 2: Maven 사용**

```bash
mvn exec:java
```

**방법 3: JAR 파일 실행**

먼저 프로젝트를 빌드합니다.

```bash
mvn clean package
```

그런 다음 JAR 파일을 실행합니다.

```bash
java -jar target/space_invaders-1.0-SNAPSHOT.jar
```

### 서버 실행 (멀티플레이어용)

먼저 테스트를 건너뛰고 프로젝트를 빌드합니다.

```bash
mvn clean package -DskipTests
```

그런 다음 서버를 실행합니다.

```bash
java -cp target/space_invaders-1.0-SNAPSHOT.jar org.newdawn.spaceinvaders.server.GameServer $PORT
```

## 개발 규칙

*   **게임 상태 관리:** 게임의 상태는 현재 라운드, 플레이어 통계, 엔티티 및 UI 상태를 추적하는 `GameStateManager` 클래스에 의해 관리됩니다.
*   **입력 처리:** 사용자 입력은 플레이어 이동, 발사 및 메뉴 상호 작용을 위한 키보드 및 마우스 이벤트를 처리하는 `InputManager` 클래스에 의해 처리됩니다.
*   **엔티티 시스템:** 게임은 기본 `Entity` 클래스와 플레이어의 함선, 외계인, 총알 및 기타 게임 개체에 대한 다양한 서브클래스를 포함하는 엔티티-컴포넌트 시스템을 사용합니다.
*   **화면 관리:** `SpaceInvadersApp` 클래스는 화면 내비게이터 역할을 하여 게임의 다른 부분(로그인, 메인 메뉴 등)을 표시하기 위해 다른 `Canvas` 객체 간에 전환합니다.
*   **멀티플레이어:** 프로젝트에는 멀티플레이어 게임 플레이를 위한 클라이언트-서버 아키텍처를 제안하는 `multyplay` 패키지가 포함되어 있습니다. `room` 패키지는 게임 로비 및 클라이언트-서버 통신을 처리하는 것으로 보입니다.
*   **데이터베이스:** `database` 패키지 및 `FirebaseDatabaseClient` 클래스에서 알 수 있듯이 Firebase는 사용자 인증 및 데이터 지속성을 위해 사용됩니다.
*   **UI:** UI는 Java Swing으로 빌드되었으며 게임 요소에 대한 사용자 정의 렌더링을 제공합니다. `UIRenderer` 클래스는 게임의 UI 구성 요소를 그리는 역할을 합니다.


## 리펙토링 규칙

*   리펙토링을 진행할 때, 기존의 기능이 똑같이 작동하도록 유지하면서 리펙토링을 해야합니다.
*   리펙토링을 진행한 후, 빌드를 실행하여 오류가 없는지 확인합니다. 실행은 하지 않아도 됩니다.
*   리펙토링을 진행한 내용을 아래에 표시된 내용과 같은 양식 예시(리펙토링 진행 내용 작성 예시)대로 refactor_content.m에 작성해야합니다.

## 리펙토링 진행 내용 작성 예시
"""
# **응답 처리 불일관성 수정**

## **냄새 : 추상화 불일관성 (Inconsistent Abstraction)**

## 대상 : 클래스 내부의 ”null” 응답 검사, 에러 스트림 읽기, JSON 파싱 로직

## 적용기법 : 응답 정보를 DatabaseResponse 값 객체로 캡슐화를 진행. readData 에서만 hasBody() / isSuccessful()을 해석하도록 변경. 이를 적용함으로써 호출부는 “성공 여부” + “원하는 타입”만 다루고, 이외의 응답 파싱은 외부로부터 숨겨짐.

```java
private static final class DatabaseResponse {
    private final int code;
    private final String body;
    private boolean isSuccessful() { return code >= 200 && code < 300; }
    private boolean hasBody() { return body != null && !body.isEmpty() && !"null".equals(body); }
}
```
"""