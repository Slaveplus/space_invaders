## 1. `multyplay` 패키지 리팩토링

### **부모 필드 가리기(Field Shadowing) 해결 - c97e5dcd 커밋**
**문제:** 자식 클래스에서 부모와 동일한 이름의 필드를 중복 선언
**냄새:** **거부된 상속 (Refused Bequest)**
**냄새에 대한 자세한 내용:**
자식 클래스 `RemoteShotEntity`가 부모 클래스 `Entity`에 이미 존재하는 `spritePath` 필드를 재선언(shadowing)하고 있었습니다. 이로 인해 부모의 기능을 제대로 활용하지 못하고, 코드 독해 시 어떤 필드가 사용되는지 혼란을 주어 잠재적인 버그를 유발할 수 있었습니다.

**대상:**
*   `MultiplayerGameCanvas`의 내부 클래스 `RemoteShotEntity`의 `spritePath` 필드

**적용 기법:**
**필드 내리기(Pull Up Field)의 역 - 필드 제거:** 자식 클래스의 중복 필드를 제거하고, 부모 클래스의 필드를 직접 사용하도록 코드를 수정했습니다. `changeSkin()`과 같은 부모의 메서드를 활용하여 스킨 변경 로직의 중복을 제거하고 상속 구조를 올바르게 활용하도록 개선했습니다.

**개선 전 코드 (핵심 부분):**
```java
private static class RemoteShotEntity extends Entity {
    private String spritePath; // 부모 필드를 가리는 중복 필드

    RemoteShotEntity(EntitySnapshot snapshot, Map<String, String> meta) {
        super(resolveShotSprite(snapshot), (int) snapshot.x, (int) snapshot.y);
        // ...
        String spriteOverride = meta.get("sprite");
        if (spriteOverride != null && !spriteOverride.isEmpty()) {
            this.spritePath = spriteOverride; // 불필요한 초기화
            changeSkin(spriteOverride);
    }
}
```

**개선 후 코드 (핵심 부분):**
```java
private static class RemoteShotEntity extends Entity {
    // private String spritePath; <-- 필드 제거

    RemoteShotEntity(EntitySnapshot snapshot, Map<String, String> meta) {
        super(resolveShotSprite(snapshot), (int) snapshot.x, (int) snapshot.y);
        apply(meta);
    }

    private void apply(Map<String, String> meta) {
        // ...
        String spriteOverride = meta.get("sprite");
        if (spriteOverride != null && !spriteOverride.isEmpty()) {
            changeSkin(spriteOverride); // 부모의 메서드를 직접 사용
        }
    }
}
```


### **의도적으로 비운 메서드 명시 - 8e1244b9 커밋**
**문제:** 의도적으로 비어있는 메서드가 정적 분석 경고를 유발
**냄새:** **게으른 클래스 (Lazy Class) 또는 스타일 위반**
**냄새에 대한 자세한 내용:**
`Remote*` 엔티티 시리즈는 서버로부터 받은 상태를 시각적으로만 표현할 뿐, 자체적인 로직(이동, 충돌 처리)을 수행하지 않습니다. 이 때문에 `move()`, `collidedWith()` 메서드를 의도적으로 비워두었는데, 이는 정적 분석 도구에서 "비어있는 메서드" 경고(`java:S1186`)를 발생시켜 코드 품질 저하로 오인될 수 있었습니다.

**대상:**
*   `MultiplayerGameCanvas` 내 `Remote*` 내부 클래스들의 `move()`, `collidedWith()` 빈 메서드들

**적용 기법:**
**주석으로 의도 명시:** 해당 메서드들이 의도적으로 비어 있음을 알리는 주석(`// Method is intentionally empty.`)을 추가했습니다. 이를 통해 정적 분석 경고를 해결하고, 다른 개발자가 코드를 보았을 때 이것이 실수가 아닌 의도된 설계임을 명확히 알 수 있도록 했습니다.

**개선 전 코드 (핵심 부분):**
```java
@Override
public void move(long delta) {
}

@Override
public void collidedWith(Entity other) {
}
```

**개선 후 코드 (핵심 부분):**
```java
@Override
public void move(long delta) {
    // Method is intentionally empty.
}

@Override
public void collidedWith(Entity other) {
    // Method is intentionally empty.
}
```


### **하드코딩된 문자열 상수화 - be0112cd 커밋**
**문제:** 소스코드에 '마법처럼' 나타나는 문자열들이 중복 사용됨
**냄새:** **하드코딩된 리터럴 (Magic Numbers/Strings)**
**냄새에 대한 자세한 내용:**
이미지 경로, UI 메시지, 설정 키와 같은 문자열 리터럴들이 코드 여러 곳에 직접 하드코딩되어 중복으로 사용되었습니다. 이로 인해 경로 변경이나 문구 수정 시 관련된 모든 코드를 찾아 일일이 수정해야 했고, 이 과정에서 실수가 발생할 가능성이 높았습니다.

**대상:**
*   `MultiplayerGameCanvas.java`의 이미지 경로, UI 텍스트 등
*   `MultiplayerInputManager.java`의 스킬 관련 메시지 텍스트

**적용 기법:**
**상수 추출 (Extract Constant):**
1.  **클래스 내부 상수:** 특정 클래스 내에서만 사용되는 문자열("클라이언트: ", "Arial" 등)은 `private static final String` 상수로 선언하여 응집도를 높였습니다.
2.  **외부 상수 클래스 분리:** 여러 곳에서 재사용될 수 있는 이미지 경로는 `SpriteConstants`라는 별도의 public 클래스에 `public static final String` 상수로 정의하여 중앙에서 관리하도록 했습니다. 이를 통해 책임이 분리되고 코드의 재사용성이 향상되었습니다.

**개선 전 코드 (핵심 부분):**
```java
// in MultiplayerGameCanvas.java
g.drawString("클라이언트: 원격 플레이어 ", 10, 30);
newEntity = new RemoteIceAttack(snapshot, "sprites/Boss_Attack/ice ball.gif");

// in MultiplayerInputManager.java
message = "스킬 포인트가 부족합니다! (필요: " + requiredPoints + ")";
```

**개선 후 코드 (핵심 부분):**
```java
// in MultiplayerGameCanvas.java
private static final String REMOTE_PLAYER_LABEL = "클라이언트: 원격 플레이어 ";
g.drawString(REMOTE_PLAYER_LABEL, 10, 30);
newEntity = new RemoteIceAttack(snapshot, SpriteConstants.ICE_BALL_GIF);

// in MultiplayerInputManager.java
private static final String SKILL_POINT_LACK_PREFIX = "스킬 포인트가 부족합니다! (필요: ";
message = SKILL_POINT_LACK_PREFIX + requiredPoints + ")";
```


### **직렬화 가능 클래스의 직렬화 불가능 필드 처리 - 7ffd2349 커밋**
**문제:** `Serializable`을 구현한 클래스가 직렬화할 수 없는 멤버 필드를 가짐
**냄새:** **직렬화 불가능 필드 (Non-Serializable Fields)**
**냄새에 대한 자세한 내용:**
`MultiplayerGameCanvas`는 `java.awt.Canvas`를 상속하여 `Serializable` 인터페이스를 구현하게 됩니다. 그러나 `ShipEntity`, `UserManager`, `GameNetworkAdapter` 등 직렬화가 불가능하거나 직렬화할 필요가 없는 객체들을 필드로 가지고 있어, 객체 직렬화 시 `NotSerializableException`이 발생할 위험이 있었습니다.

**대상:**
*   `MultiplayerGameCanvas.java` 내의 `ShipEntity`, `UserManager`, `ResolutionManager`, `MultiplayerGameStateManager` 등 직렬화 불가능한 모든 필드

**적용 기법:**
**transient 키워드 선언:** 직렬화 과정에서 제외되어야 할 모든 필드에 `transient` 키워드를 추가했습니다. 이를 통해 JVM의 기본 직렬화 메커니즘이 이 필드들을 무시하도록 하여 예외 발생을 방지하고, 이 객체들이 런타임에만 필요한 임시적 상태임을 명시했습니다.

**개선 전 코드 (핵심 부분):**
```java
public class MultiplayerGameCanvas extends Canvas { // Canvas는 Serializable을 구현
    private ShipEntity ship;
    private UserManager userManager;
    private GameNetworkAdapter networkAdapter;
    // ...
}
```

**개선 후 코드 (핵심 부분):**
```java
public class MultiplayerGameCanvas extends Canvas {
    private transient ShipEntity ship;
    private transient UserManager userManager;
    private transient GameNetworkAdapter networkAdapter;
    // ...
}
```


### **`switch` 문에 `default` 케이스 추가 - 7ffd2349 커밋**
**문제:** `switch` 문에 `default` 케이스가 없어 예외 상황 처리가 누락됨
**냄새:** **`switch` 문에 `default` 케이스 없음 (Missing 'default' case)**
**냄새에 대한 자세한 내용:**
`MultiplayerInputManager`의 스킬 선택 로직에서 `switch` 문이 모든 가능한 `case`를 다루고 있다고 가정하여 `default` 케이스가 없었습니다. 만약 예기치 않은 값이 입력될 경우 아무런 처리가 이루어지지 않아 오작동의 원인이 될 수 있었습니다.

**대상:**
*   `MultiplayerInputManager.java`의 `handleSkillSelection` 메서드 내 `switch (selectedSkill)`

**적용 기법:**
**`default` 케이스 추가:** `switch` 문에 `default` 케이스를 추가하여, 정의되지 않은 스킬 선택에 대해 "알 수 없는 스킬"이라는 메시지를 설정하도록 명시적으로 처리했습니다. 이를 통해 코드의 안정성을 높였습니다.

**개선 전 코드 (핵심 부분):**
```java
switch (selectedSkill) {
    case 1:
        result = "선택된 스킬: 미사일";
        break;
    case 2:
        result = "선택된 스킬: 보호막";
        break;
    // ... 다른 스킬들
}
```

**개선 후 코드 (핵심 부분):**
```java
switch (selectedSkill) {
    case 1:
        result = "선택된 스킬: 미사일";
        break;
    case 2:
        result = "선택된 스킬: 보호막";
        break;
    // ... 다른 스킬들
    default:
        result = "알 수 없는 스킬입니다.";
        break;
}
```

---

## 2. `common` 패키지 리팩토링

### **부모 필드 가리기(Field Shadowing) 해결 (ShotEntity) - f47826f6 커밋**
**문제:** 자식 클래스 `ShotEntity`가 부모 `Entity`의 필드를 중복 선언
**냄새:** **거부된 상속 (Refused Bequest)**
**냄새에 대한 자세한 내용:**
`ShotEntity` 클래스가 부모 `Entity`에 이미 존재하는 `spritePath` 필드를 다시 선언하고 있었습니다. 이는 부모의 상속을 올바르게 활용하지 못하는 것이며, 자체적으로 `setSpritePath`라는 중복 기능의 메서드까지 갖게 만들어 코드의 일관성을 해치고 혼란을 야기했습니다.

**대상:**
*   `ShotEntity.java`

**적용 기법:**
**필드 제거 및 메서드 통합:** `ShotEntity`의 중복 `spritePath` 필드와 불필요한 `setSpritePath` 메서드를 제거했습니다. 대신, 스킨을 변경해야 할 때 부모 클래스의 `changeSkin()` 메서드를 직접 호출하도록 수정하여 상속 구조를 명확히 하고 코드 중복을 제거했습니다.

**개선 전 코드 (핵심 부분):**
```java
public class ShotEntity extends Entity {
    protected String spritePath; // 부모 필드를 가리는 필드

    public ShotEntity(GameContext game, String sprite, int x, int y) {
        super(sprite, x, y);
        this.spritePath = sprite; // 불필요한 할당
    }

    public void setSpritePath(String spritePath) { // 중복 기능의 메서드
        this.spritePath = spritePath;
        if (spritePath != null && !spritePath.isEmpty()) {
            changeSkin(spritePath);
        }
    }
}
```

**개선 후 코드 (핵심 부분):**
```java
public class ShotEntity extends Entity {
    // spritePath 필드 제거

    public ShotEntity(GameContext game, String sprite, int x, int y) {
        super(sprite, x, y);
        // 불필요한 할당 제거
    }

    // setSpritePath 메서드 제거

    public void setNearMonsterShot(boolean nearMonsterShot, int round, String spritePath) {
        this.nearMonsterShot = nearMonsterShot;
        this.nearMonsterRound = round;
        changeSkin(spritePath); // 부모의 메서드 직접 사용
    }
}
```

---

## 3. `room` 패키지 리팩토링

### **중복 문자열 리터럴 상수화 - aa4cb740 커밋**
**문제:** 여러 파일에 걸쳐 동일한 문자열이 중복으로 하드코딩됨
**냄새:** **하드코딩된 리터럴 (Magic Numbers/Strings)**
**냄새에 대한 자세한 내용:**
"roomId", "single", "Arial"과 같은 문자열들이 `GameClient`, `RoomListCanvas`, `RoomLobbyCanvas` 등 여러 파일에서 반복적으로 사용되었습니다. 이는 일관성을 해치고, 향후 해당 문자열 변경 시 모든 파일을 찾아 수정해야 하는 번거로움을 유발했습니다.

**대상:**
*   `GameClient.java`
*   `RoomListCanvas.java`
*   `RoomLobbyCanvas.java`

**적용 기법:**
**상수 추출 (Extract Constant):** 각 클래스 내에서 중복되는 문자열을 `private static final` 상수로 추출하여 한 곳에서 관리하도록 했습니다. 이로써 코드의 가독성이 향상되고 유지보수가 용이해졌습니다.

**개선 전 코드 (핵심 부분):**
```java
// in GameClient.java
data.put("roomId", roomId);

// in RoomListCanvas.java
g.setFont(new Font("Arial", Font.BOLD, 20));
```

**개선 후 코드 (핵심 부분):**
```java
// in GameClient.java
private static final String KEY_ROOM_ID = "roomId";
data.put(KEY_ROOM_ID, roomId);

// in RoomListCanvas.java
private static final String FONT_NAME = "Arial";
g.setFont(new Font(FONT_NAME, Font.BOLD, 20));
```


### **직렬화 불가능 필드에 `transient` 키워드 추가 - aa4cb740 커밋**
**문제:** `Serializable` 클래스가 직렬화할 수 없는 필드를 포함
**냄새:** **직렬화 불가능 필드 (Non-Serializable Fields)**
**냄새에 대한 자세한 내용:**
`Canvas`를 상속하여 직렬화가 가능한 `RoomListCanvas`와 `RoomLobbyCanvas` 클래스가 `ScreenNavigator`, `GameClient` 등 UI와 네트워크 통신에 관련된, 직렬화가 불가능하거나 필요 없는 객체들을 필드로 가지고 있었습니다. 이는 객체 직렬화 시 예외를 유발할 수 있는 잠재적 위험 요소였습니다.

**대상:**
*   `RoomListCanvas.java`, `RoomLobbyCanvas.java` 내의 `navigator`, `client`, `backgroundImage` 등 직렬화 불가능 필드

**적용 기법:**
**transient 키워드 선언:** 직렬화 대상에서 제외해야 할 필드들에 `transient` 키워드를 명시적으로 추가하여, 해당 필드들이 객체의 영속적 상태에 포함되지 않음을 선언하고 직렬화 과정에서 무시되도록 했습니다.

**개선 전 코드 (핵심 부분):**
```java
public class RoomListCanvas extends Canvas {
    private ScreenNavigator navigator;
    private GameClient client;
    private BufferedImage backgroundImage;
    // ...
}
```

**개선 후 코드 (핵심 부분):**
```java
public class RoomListCanvas extends Canvas {
    private transient ScreenNavigator navigator;
    private transient GameClient client;
    private transient BufferedImage backgroundImage;
    // ...
}
```

## 4. `server` 패키지 리팩토링

### **서버 측 중복 리터럴 상수화 및 복잡도 감소 - a37bb1e1 커밋**
**문제:** 서버 코드에 중복 문자열이 많고, 일부 메서드가 지나치게 복잡함
**냄새:** **하드코딩된 리터럴 (Magic Numbers/Strings) 및 긴 메서드 (Long Method)**
**냄새에 대한 자세한 내용:**
서버 측 클래스들(`ClientConnection`, `ServerMultiplayerGame` 등)에서 프로토콜 키("roomId", "hostId"), 메시지, 파일 경로 등이 하드코딩되어 반복 사용되었습니다. 또한, `ServerGameSession.handleAction`, `ServerMultiplayerGame.update`와 같은 핵심 메서드들이 너무 많은 로직을 한 곳에서 처리하여 복잡도가 높고 유지보수가 어려웠습니다.

**대상:**
*   `ClientConnection.java`, `GameServer.java`, `ServerGameSession.java`, `ServerMultiplayerGame.java` 내의 중복 리터럴 및 복잡한 메서드들

**적용 기법:**
1.  **상수 추출 (Extract Constant):** 반복 사용되는 문자열들을 `private static final` 상수로 추출하여 코드의 일관성과 가독성을 높였습니다.
2.  **메서드 추출 (Extract Method):** 복잡한 메서드의 로직을 기능 단위로 분리하여 작은 private 메서드로 만들었습니다. 예를 들어, `handleAction`의 `switch`문 각 `case`의 로직을 `handleRoundReadyAction`, `handleChatAction` 등의 개별 메서드로 추출했습니다.

**개선 전 코드 (핵심 부분):**
```java
// in ServerGameSession.java
private void handleAction(ClientConnection client, Map<String, Object> action) {
    String type = (String) action.get("type");
    switch (type) {
        case "roundReady":
            // 라운드 준비 관련 로직이 여기에 길게 작성됨...
            break;
        case "chat":
            // 채팅 관련 로직이 여기에 길게 작성됨...
            break;
    }
}
```

**개선 후 코드 (핵심 부분):**
```java
// in ServerGameSession.java
private void handleAction(ClientConnection client, Map<String, Object> action) {
    String type = (String) action.get(KEY_TYPE); // 상수 사용
    switch (type) {
        case ACTION_ROUND_READY: // 상수 사용
            handleRoundReadyAction(client, action); // 메서드 추출
            break;
        case ACTION_CHAT: // 상수 사용
            handleChatAction(client, action); // 메서드 추출
            break;
    }
}

private void handleRoundReadyAction(ClientConnection client, Map<String, Object> action) { ... }
private void handleChatAction(ClientConnection client, Map<String, Object> action) { ... }
```
