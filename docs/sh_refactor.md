# 리펙토링 내용

## 1. 중복 클래스 통합
### 🛠 **중복 클래스 통합 - 151c1478, 774c3a79 커밋**

#### **냄새** : **중복 코드 (Duplicated Code)**

기존 싱글플레이(gameplay 패키지)와 멀티플레이(multyplay 패키지)는 같은 UI/UX를 멀티플레이와 싱글플레이로 구현하기 위해, 동일한 Entity 클래스의 코드가 조금 다른 상태로 복제되어 존재되었음.

- 이미지

![origin_gameplay](md_images/origin_gameplay.png)
![origin_multyplay](md_images/origin_multyplay.png)


**대상:** gameplay 패키지, multypaly 패키지



## 2. multyplay패키지
### 🛠 **필드 섀도잉 리팩토링 - c97e5dcd 커밋**

#### **냄새** : 부모 필드 가리기 (Field Shadowing)**

`RemoteShotEntity` 클래스의 `spritePath` 필드가 부모 클래스 `Entity`의 필드를 가리고 있어 코드 이해를 방해하고 잠재적 오류를 유발했습니다.

#### **대상** : `MultiplayerGameCanvas`의 내부 클래스 `RemoteShotEntity`

**적용기법** : `RemoteShotEntity`의 중복 `spritePath` 필드를 제거하고 부모 클래스의 필드를 사용하도록 상속 구조를 활용했습니다. 이로 인해 불필요한 초기화 및 중복된 스킨 변경 로직을 정리하여 코드를 단순화했습니다.

```java
// MultiplayerGameCanvas.java

// ...

    private static class RemoteShotEntity extends Entity {
        private static final Map<Integer, BufferedImage> SKILL_ICON_CACHE = new HashMap<>();
        private boolean isAlienShot;
        private boolean isSkillDrop;
        private int skillType;
        private int skillValue;
        private boolean hasPiercing;
        private boolean nearMonsterShot;

        RemoteShotEntity(EntitySnapshot snapshot, Map<String, String> meta) {
            super(resolveShotSprite(snapshot), (int) snapshot.x, (int) snapshot.y);
            apply(meta);
        }

        private static String resolveShotSprite(EntitySnapshot snapshot) {
            return snapshot.sprite != null && !snapshot.sprite.isEmpty() ? snapshot.sprite : "sprites/shot.gif";
        }

        private void apply(Map<String, String> meta) {
            if (meta == null) return;
            isAlienShot = "1".equals(meta.get("alien"));
            isSkillDrop = "1".equals(meta.get("skill"));
            try { skillType = Integer.parseInt(meta.getOrDefault("skillType", "-1")); }
            catch (NumberFormatException ignore) { skillType = -1; }
            try { skillValue = Integer.parseInt(meta.getOrDefault("skillValue", "0")); }
            catch (NumberFormatException ignore) { skillValue = 0; }
            hasPiercing = "1".equals(meta.get("pierce"));
            nearMonsterShot = "1".equals(meta.get("near"));
            String spriteOverride = meta.get("sprite");
            if (spriteOverride != null && !spriteOverride.isEmpty()) {
                changeSkin(spriteOverride);
            }
        }
// ...

```




### 🛠 **정적 분석 경고 수정 - 8e1244b9 커밋**

#### **냄새** : **빈 메서드 (Empty Methods) & 사용되지 않는 파라미터 (Unused Parameter)**

`MultiplayerGameCanvas.java` 파일 내 다수의 내부 클래스에서 `java:S1186` (메서드는 비어 있어서는 안 됩니다)와 `java:S1172` (사용되지 않는 메서드 매개변수 제거) 경고가 발생했습니다.

`Remote*` 시리즈 내부 클래스들의 `move()`, `collidedWith()` 메서드가 비어 있었습니다. 이 메서드들은 원격 엔티티의 시각적 표현만을 담당하므로, 로컬에서 로직을 처리할 필요가 없어 의도적으로 비워둔 것입니다.

`addScore()` 메서드가 내용 없이 주석만 있었습니다.

`RemoteRound4GreenSphere` 생성자가 사용되지 않는 `meta` 파라미터를 가지고 있었습니다.

#### **대상** : `MultiplayerGameCanvas.java` 내의 여러 내부 클래스

`RemoteIceAttack`, `RemoteIceBallAttack`, `RemoteMagneticField`, `RemoteRound2Laser`, `RemoteRound2Phase1`, `RemoteRound2Phase2`, `RemoteRound2Random`, `RemoteRound2Quad`, `RemoteRound2MachineGun`, `RemoteRound3Straight`, `RemoteRound3Random`, `RemoteRound3Pull`, `RemoteRound3BlackHole`, `RemoteRound4Heal`, `RemoteRound4GreenSphere`, `RemoteRound4PlayerLine`, `RemoteExplosionEntity`

`addScore(String playerId, int points)`

`RemoteRound4GreenSphere` 생성자 및 호출부

**적용 기법 :**

**주석 추가**: SonarLint 경고를 해결하기 위해, 의도적으로 비워둔 모든 `move()` 및 `collidedWith()`, `addScore()` 메서드에 `// Method is intentionally empty.` 주석을 추가하여 코드의 의도를 명확히 했습니다.

**파라미터 제거**: `RemoteRound4GreenSphere` 생성자에서 사용되지 않는 `meta` 파라미터를 제거하고, `createRemoteEntity` 메서드 내의 해당 생성자 호출 코드도 함께 수정하여 불필요한 코드를 정리했습니다.

```java
// RemoteIceAttack.java
@Override
public void move(long delta) {
    // Method is intentionally empty.
}

@Override
public void collidedWith(Entity other) {
    // Method is intentionally empty.
}

// RemoteRound4GreenSphere.java
RemoteRound4GreenSphere(EntitySnapshot snapshot) {
    super("sprites/Boss_Attack/5round1.gif", (int) Math.round(snapshot.x), (int) Math.round(snapshot.y));
    // ...
}

// createRemoteEntity
case "Round4GreenSphereAttack":
    return new RemoteRound4GreenSphere(snapshot);

```




### 🛠 **상수화 (MultiplayerGameCanvas.java) - be0112cd 커밋**

#### **냄새** : **중복 코드 (Duplicated Code)**

`MultiplayerGameCanvas.java` 파일 내에서 이미지 경로, UI 텍스트, 설정 키 등의 문자열 리터럴이 여러 곳에서 중복 사용되고 있었음. 이로 인해 유지보수 및 변경 시 오류 발생 가능성이 높았음.

#### **대상** :

"sprites/Boss_Attack/ice ball.gif"

"sprites/Skill/Heat.gif"

"sprites/ship.gif"

"sprites/Boss_Attack/2round1.gif"

"sprites/shot.gif"

"sprites/Skill/Explosion.png"

"클라이언트: 원격 플레이어 "

"Arial"

"radius"

"🏆 GAME COMPLETED! 🏆 Congratulations!"

#### **적용 기법** :

**클래스 내부 상수화**: `MultiplayerGameCanvas` 내에서만 사용되는 UI 텍스트 및 설정 키 ("클라이언트: 원격 플레이어 ", "Arial", "radius", "🏆 GAME COMPLETED! 🏆 Congratulations!")는 `private static final String` 상수로 정의하여 클래스 내에서 관리되도록 함.

**외부 상수 클래스 분리**: 이미지 파일 경로는 재사용 가능성이 높고 특정 도메인(스프라이트)에 속하므로, `org.newdawn.spaceinvaders.common.sprite.SpriteConstants`라는 새로운 상수 클래스를 생성하고 `public static final String` 상수로 정의하여 관리되도록 함. 이를 통해 이미지 경로의 중앙 집중식 관리가 가능해지고 가독성이 향상됨.

#### **변경 내용** :

`MultiplayerGameCanvas.java` 파일 상단에 `SpriteConstants` import 문 추가.

`MultiplayerGameCanvas.java` 클래스 내부에 `REMOTE_PLAYER_LABEL`, `FONT_ARIAL`, `RADIUS_KEY`, `GAME_COMPLETED_MESSAGE` 상수를 선언하고 중복된 문자열 리터럴을 해당 상수로 대체.

`org.newdawn.spaceinvaders.common.sprite.SpriteConstants.java` 파일을 생성하고 `ICE_BALL_GIF`, `HEAT_GIF`, `SHIP_GIF`, `ROUND_2_ATTACK_1_GIF`, `SHOT_GIF`, `EXPLOSION_PNG` 상수를 선언하고 `MultiplayerGameCanvas.java` 내의 중복된 이미지 경로 문자열 리터럴을 해당 상수로 대체.



---


### 🛠 **상수화 (MultiplayerInputManager.java) - be0112cd 커밋**

#### **냄새** : **중복 코드 (Duplicated Code)**

`MultiplayerInputManager.java` 파일 내에서 스킬 메시지 관련 문자열 리터럴이 여러 곳에서 중복 사용되고 있었음. 이로 인해 유지보수 및 변경 시 오류 발생 가능성이 높았음.

#### **대상** :

"스킬 포인트가 부족합니다! (필요: "

", 레벨: "

", 보유: "

#### **적용 기법** :

**클래스 내부 상수화**: `MultiplayerInputManager` 내에서만 사용되는 스킬 메시지 관련 텍스트는 `private static final String` 상수로 정의하여 클래스 내에서 관리되도록 함.

#### **변경 내용** :

`MultiplayerInputManager.java` 클래스 내부에 `SKILL_POINT_LACK_PREFIX`, `SKILL_LEVEL_SUFFIX`, `SKILL_POINT_HOLD_SUFFIX` 상수를 선언하고 중복된 문자열 리터럴을 해당 상수로 대체.




### 🛠 **생성자 복잡도 감소 (MultiplayerGameCanvas) - 3be08817 커밋**

#### **냄새** : **높은 인지 복잡도 (High Cognitive Complexity)**

`MultiplayerGameCanvas` 클래스의 생성자는 캔버스 설정, 컴포넌트 초기화, 입력 핸들러 등록 등 여러 역할을 동시에 수행하고 있어 인지 복잡도가 20으로 높았습니다. 이로 인해 코드의 가독성과 유지보수성이 저하되었습니다.

#### **대상** : `MultiplayerGameCanvas` 생성자

#### **적용 기법** : **메서드 추출 (Extract Method)**

생성자의 각기 다른 책임들을 별도의 private 메서드로 분리했습니다.

`initCanvas()`: 캔버스 관련 설정을 담당합니다.

`initComponents()`: `gameStateManager`, `skillManager`, `uiRenderer` 등 핵심 컴포넌트들의 초기화를 담당합니다.

`initInputHandlers()`: 키보드 및 마우스 입력 핸들러 등록을 담당합니다.

이를 통해 생성자의 복잡도를 낮추고, 각 초기화 단계의 역할을 명확히 하여 코드의 가독성과 유지보수성을 향상시켰습니다.

#### **변경 내용** :

`MultiplayerGameCanvas` 생성자의 내용을 `initCanvas()`, `initComponents()`, `initInputHandlers()`, `initEntities()` 메서드 호출로 변경.

`initCanvas()`, `initComponents()`, `initInputHandlers()` private 메서드를 새로 생성하여 기존 생성자의 초기화 로직을 분리.




### 🛠 **메서드 복잡도 감소 (MultiplayerGameCanvas, MultiplayerInputManager, SharedMultiplayerRoundCoordinator) - 7ffd2349 커밋**

#### **냄새** : **높은 인지 복잡도 (High Cognitive Complexity)**

`MultiplayerGameCanvas`, `MultiplayerInputManager`, `SharedMultiplayerRoundCoordinator` 클래스의 여러 메서드들이 너무 많은 역할을 수행하고 있어 인지 복잡도가 높았습니다. 이로 인해 코드의 가독성과 유지보수성이 저하되었습니다.

#### **대상** :

`MultiplayerGameCanvas`의 `update`, `applyRemoteSnapshot`, `createRemoteEntity`, `render`, `drawSpectatorOverlay`, `drawIntermissionOverlay` 메서드

`MultiplayerInputManager`의 `handleGameplayKeyPressed` 메서드

`SharedMultiplayerRoundCoordinator`의 `handleNearMonsterDestroyed` 메서드

#### **적용 기법** : **메서드 추출 (Extract Method)**

각 메서드의 책임들을 별도의 private 메서드로 분리하여 복잡도를 낮추고 가독성을 향상시켰습니다.

**`MultiplayerGameCanvas.update()`**:

- `updateRemote()`: 원격 모드 로직 처리
- `updateLocal()`: 로컬 모드 로직 처리
    - `moveEntities()`: 엔티티 이동 및 외계인 발사 로직
    - `updateShipReference()`: 플레이어 함선 참조 업데이트
    - `updateShipMovement()`: 함선 이동 로직
    - `checkCollisions()`: 충돌 감지 로직
    - `cleanupEntities()`: 엔티티 정리 로직

**`MultiplayerGameCanvas.applyRemoteSnapshot()`**:

- `applyEntitySnapshots()`: 엔티티 스냅샷 적용
- `applyPlayerStateSnapshots()`: 플레이어 상태 스냅샷 적용
    - `updateLocalPlayerSkills()`: 로컬 플레이어 스킬 업데이트
- `applyGamePhaseAndState()`: 게임 단계 및 상태 적용
- `updateLocalPlayerStatus()`: 로컬 플레이어 상태 업데이트

**`MultiplayerGameCanvas.createRemoteEntity()`**:

- 팩토리 패턴을 적용하여 `entityFactories` 맵을 사용해 엔티티 생성 로직을 분리.

**`MultiplayerGameCanvas.render()`**:

- `applyResolutionScaling()`: 해상도 스케일링 적용
- `drawGameElements()`: 게임 요소 렌더링
- `drawOverlays()`: 오버레이 렌더링

**`MultiplayerGameCanvas.drawSpectatorOverlay()`**:

- `drawSpectatorPanelBackground()`: 관전 패널 배경 렌더링
- `drawPlayerLists()`: 플레이어 목록 렌더링
    - `drawPlayerList()`: 개별 플레이어 목록 렌더링

**`MultiplayerGameCanvas.drawIntermissionOverlay()`**:

- `drawIntermissionPanel()`: 막간 패널 배경 렌더링
- `drawPlayerReadyStatus()`: 플레이어 준비 상태 렌더링
- `drawIntermissionChat()`: 채팅창 렌더링
- `drawIntermissionInfo()`: 정보 텍스트 렌더링

**`MultiplayerInputManager.handleGameplayKeyPressed()`**:

- `shouldIgnoreInput()`: 입력 무시 여부 확인
- `handleMenuKeys()`: 메뉴 키 처리
- `handleNavigationAndActivationKeys()`: 네비게이션 및 활성화 키 처리
    - `handleSkillActivationKeys()`: 스킬 활성화 키 처리
        - `activateSkill()`: 스킬 활성화 로직
- `handleMovementKeys()`: 이동 키 처리

**`SharedMultiplayerRoundCoordinator.handleNearMonsterDestroyed()`**:

- `givePlayerRewards()`: 플레이어 보상 지급
- `checkWaveClear()`: 웨이브 클리어 확인 및 보상 지급

#### **변경 내용** :

각 클래스의 복잡한 메서드들을 더 작고 관리하기 쉬운 여러 private 메서드로 분리하여 코드의 가독성과 유지보수성을 향상시켰습니다.




### 🛠 **Serializable 경고 수정 (MultiplayerGameCanvas) - 7ffd2349 커밋**

#### **냄새** : **직렬화 불가능 필드 (Non-Serializable Fields)**

`MultiplayerGameCanvas` 클래스는 `java.io.Serializable`을 구현하는 `java.awt.Canvas`를 상속받으므로 직렬화 가능하지만, 내부에 `Serializable`을 구현하지 않거나 직렬화할 수 없는 여러 필드들을 포함하고 있었습니다. 이로 인해 `NotSerializableException`이 발생할 수 있는 잠재적인 위험이 있었고, SonarQube `java:S1948` 경고가 발생했습니다.

#### **대상** : `MultiplayerGameCanvas.java` 내의 다음 필드

`ShipEntity ship`

`UserManager userManager`

`ResolutionManager resolutionManager`

`MultiplayerGameStateManager gameStateManager`

`MultiplayerInputManager inputManager`

`MultiplayerSkillManager skillManager`

`MultiplayerUIRenderer uiRenderer`

`BackgroundRenderer backgroundRenderer`

`GameNetworkAdapter networkAdapter`

#### **적용 기법** : **필드 transient 선언 (Declare Field as transient)**

`MultiplayerGameCanvas`의 직렬화 가능성을 유지하면서 내부의 직렬화 불가능한 필드들이 직렬화 과정에서 제외되도록 `transient` 키워드를 추가했습니다. 이는 해당 필드들이 객체 상태를 저장하거나 복원하는 데 직접적으로 관여하지 않음을 명시하고, JVM의 기본 직렬화 메커니즘이 이 필드들을 무시하도록 합니다. 게임 상태의 핵심인 `gameStateManager`는 현재 직렬화가 불가능하므로 `transient`로 처리되었으며, 추후 게임 저장/로드 기능을 구현하려면 `MultiplayerGameStateManager` 자체의 직렬화 또는 사용자 정의 직렬화 로직 구현이 필요할 수 있습니다.

#### **변경 내용** :

위에 명시된 `MultiplayerGameCanvas` 클래스의 모든 필드에 `transient` 키워드를 추가.




### 🛠 **Switch 문 default 케이스 추가 (MultiplayerInputManager) - 7ffd2349 커밋**

#### **냄새** : **Switch 문에 default 케이스 없음 (Missing 'default' case in switch statement)**

`MultiplayerInputManager.java` 내의 `handleSkillActivationKeys` 및 `handleSkillSelection` 메서드의 `switch` 문에 `default` 케이스가 없어 `java:S131` 경고가 발생했습니다. `default` 케이스가 없으면 예외적인 값에 대한 처리가 누락되어 예기치 않은 동작이 발생할 수 있습니다.

#### **대상** :

`MultiplayerInputManager.java`의 `handleSkillActivationKeys` 메서드 내 `switch (keyCode)`

`MultiplayerInputManager.java`의 `handleSkillSelection` 메서드 내 `switch (selectedSkill)`

#### **적용 기법** : **default 케이스 추가 (Add 'default' case)**

`switch` 문에 `default` 케이스를 추가하여 정의되지 않은 값에 대한 명시적인 처리 로직을 제공했습니다.

#### **변경 내용** :

`handleSkillActivationKeys` 메서드의 `switch (keyCode)`에 `default: return false;` 추가 (이전 커밋에서 이미 존재했음을 확인).

`handleSkillSelection` 메서드의 `switch (selectedSkill)`에 `default: result = "알 수 없는 스킬입니다."; break;` 추가.




### 🛠 **메서드 및 클래스 추출을 통한 인지 복잡도 감소 - 7ffd2349 커밋**

#### **냄새** : **높은 인지 복잡도 (High Cognitive Complexity) 및 중복 코드 (Duplicated Code)**

`MultiplayerGameCanvas`, `MultiplayerInputManager`, `SharedMultiplayerRoundCoordinator` 클래스의 여러 메서드들이 너무 많은 역할을 수행하고 있거나, 유사한 로직이 여러 곳에 분산되어 있어 인지 복잡도가 높았습니다. 특히, `MultiplayerGameCanvas.createRemoteEntity()` 메서드는 `switch` 문으로 인해 매우 긴 메서드였으며, `MultiplayerGameCanvas.render()`, `MultiplayerGameCanvas.drawSpectatorOverlay()`, `MultiplayerGameCanvas.drawIntermissionOverlay()` 등은 단일 메서드에서 너무 많은 UI 요소를 그리고 있었습니다. 또한, `SharedMultiplayerRoundCoordinator.handleNearMonsterDestroyed()`에서는 보상 지급 및 웨이브 클리어 로직이 한 곳에 묶여 있었습니다.

#### **대상** :

`MultiplayerGameCanvas`의 `applyRemoteSnapshot`, `processGameEvents`, `createRemoteEntity`, `render`, `drawSpectatorOverlay`, `drawIntermissionOverlay`, `collectTeammateNames` 메서드

`MultiplayerInputManager`의 `handleGameplayKeyPressed` 메서드

`SharedMultiplayerRoundCoordinator`의 `handleNearMonsterDestroyed` 메서드

`MultiplayerInputManager`의 `handlePauseMenuInput` 및 `handleSkillMenuInput` 메서드 내 `switch` 문

#### **적용 기법** : **메서드 추출 (Extract Method), 클래스 추출 (Extract Class), 팩토리 패턴 (Factory Pattern) 적용**

각 메서드의 책임들을 별도의 private 메서드로 분리하거나, 재사용 가능한 로직을 클래스로 추출하여 복잡도를 낮추고 가독성을 향상시켰습니다.

**`MultiplayerGameCanvas.applyRemoteSnapshot()`**:

- `updateLocalPlayerSkills()` 메서드를 추출하여 로컬 플레이어의 스킬 업데이트 로직을 캡슐화했습니다.
- `applyGamePhaseAndState()` 메서드를 추출하여 게임의 단계 및 상태 적용 로직을 분리했습니다.

**`MultiplayerGameCanvas.processGameEvents()`**:

- 이벤트 처리 로직을 `handleChatEvent()`, `handleSystemEvent()`, `handleSkinChangedEvent()`, `handleCoinPopupEvent()`와 같은 전용 메서드로 분리하여 각 이벤트 타입별 처리를 명확히 했습니다.

**`MultiplayerGameCanvas.createRemoteEntity()`**:

- `entityFactories` 맵을 활용한 팩토리 패턴을 도입하여 다양한 엔티티 타입에 대한 객체 생성 로직을 중앙 집중화하고 확장성을 높였습니다.
- `createNearEntityFromSnapshot()`, `createAlienEntityFromSnapshot()`, `createDefaultEntity()`와 같은 헬퍼 메서드를 추출하여 엔티티 생성의 세부 로직을 캡슐화했습니다.

**`MultiplayerGameCanvas.render()`**:

- `applyResolutionScaling()`, `drawGameElements()`, `drawOverlays()` 메서드를 추출하여 렌더링 과정을 논리적인 단계로 나누었습니다.
- `getHitboxColor()` 메서드를 추출하여 엔티티 타입별 히트박스 색상 결정 로직을 분리했습니다.

**`MultiplayerGameCanvas.drawSpectatorOverlay()`**:

- `drawSpectatorPanelBackground()`, `drawSpectatorText()`, `drawPlayerLists()` 메서드를 추출하여 관전자 오버레이의 각 부분을 개별적으로 처리하도록 했습니다.
- `PlayerCategorizationResult` 내부 클래스를 도입하여 플레이어 분류 결과를 구조화하고, `categorizePlayers()` 메서드를 추출하여 플레이어 분류 로직을 캡슐화했습니다.
- `drawPlayerList()` 메서드를 추출하여 특정 플레이어 목록을 그리는 로직을 일반화했습니다.

**`MultiplayerGameCanvas.drawIntermissionOverlay()`**:

- `drawIntermissionPanel()`, `drawIntermissionTitle()`, `drawIntermissionMessage()`, `drawPlayerReadyStatus()`, `drawIntermissionChat()`, `drawIntermissionInfo()` 메서드를 추출하여 막간 오버레이의 각 컴포넌트 렌더링을 분리했습니다.

**`MultiplayerGameCanvas.collectTeammateNames()`**:

- `addTeammateNamesFromDisplayNames()` 및 `addTeammateNamesFromPlayerStates()` 메서드를 추출하여 팀원 이름 수집 로직을 명확히 분리했습니다.

**`MultiplayerInputManager.handleGameplayKeyPressed()`**:

- `shouldIgnoreInput()`, `handleMenuKeys()`, `handleNavigationAndActivationKeys()`, `handleSkillActivationKeys()`, `activateSkill()`, `handleMovementKeys()`와 같은 메서드들을 추출하여 입력 처리 로직을 세분화하고 각 책임에 집중하도록 했습니다.

**`SharedMultiplayerRoundCoordinator.handleNearMonsterDestroyed()`**:

- `givePlayerRewards()`와 `checkWaveClear()` 메서드를 추출하여 보상 지급 및 웨이브 클리어 관련 로직을 분리했습니다.

**`MultiplayerInputManager`의 `handlePauseMenuInput` 및 `handleSkillMenuInput` 메서드 내 `switch` 문**:

- `default` 케이스를 추가하여 예상치 못한 값에 대한 처리를 명시적으로 정의했습니다.

**`MultiplayerGameStateManager`**:

- `isGamePaused()` 메서드를 추가하여 게임 일시 정지 상태를 확인하는 단일 진입점을 제공했습니다.

#### **변경 내용** :

위에서 설명된 모든 메서드들이 추출되거나, 새로운 클래스/내부 클래스가 도입되어 코드의 모듈성과 재사용성이 크게 향상되었습니다. 이를 통해 각 컴포넌트의 역할이 명확해지고, 향후 기능 추가 및 유지보수가 용이해졌습니다.




## 3. common 패키지.


### 🛠 **필드 섀도잉 리팩토링 (ShotEntity) - f47826f6 커밋**

#### **냄새** : **부모 필드 가리기 (Field Shadowing)**

`ShotEntity` 클래스의 `spritePath` 필드가 부모 클래스 `Entity`의 필드를 가리고 있어 코드 이해를 방해하고 잠재적 오류를 유발했습니다. (java:S2387)

#### **대상** : `ShotEntity.java`

#### **적용 기법** : `ShotEntity`의 중복 `spritePath` 필드를 제거하고 부모 클래스의 필드를 사용하도록 상속 구조를 활용했습니다. 이로 인해 불필요한 초기화 및 중복된 스킨 변경 로직(`setSpritePath`)을 정리하여 코드를 단순화했습니다.

```java
// ShotEntity.java (Before)
public class ShotEntity extends Entity {
    // ...
    protected String spritePath; // Shadowing field

    public ShotEntity(GameContext game, String sprite, int x, int y) {
        super(sprite, x, y);
        // ...
        this.spritePath = sprite; // Redundant assignment
    }

    public void setSpritePath(String spritePath) { // Redundant method
        this.spritePath = spritePath;
        if (spritePath != null && !spritePath.isEmpty()) {
            changeSkin(spritePath);
        }
    }
    // ...
}

// ShotEntity.java (After)
public class ShotEntity extends Entity {
    // ...
    // Removed spritePath field

    public ShotEntity(GameContext game, String sprite, int x, int y) {
        super(sprite, x, y);
        // ...
        // Removed redundant assignment
    }

    // Removed setSpritePath method

    public void setNearMonsterShot(boolean nearMonsterShot, int round, String spritePath) {
        this.nearMonsterShot = nearMonsterShot;
        this.nearMonsterRound = round;
        changeSkin(spritePath); // Used parent's method
    }
    // ...
}

```




### 🛠 **메서드 복잡도 감소 (Cognitive Complexity) - f47826f6 커밋**

#### **냄새** : **높은 인지 복잡도 (High Cognitive Complexity)**

`BaseAlienEntity`, `BaseSkillManager`, `NearEntity` 클래스의 일부 메서드들이 너무 많은 `if-else` 분기문과 중첩된 로직을 가지고 있어 인지 복잡도가 높았습니다(java:S3776). 이로 인해 코드의 가독성과 유지보수성이 저하되었습니다.

#### **대상** :

`BaseAlienEntity.java`의 `move()` 메서드

`BaseSkillManager.java`의 `getRandomSkillPoints()` 메서드

`NearEntity.java`의 `move()` 메서드

#### **적용 기법** : **메서드 추출 (Extract Method)**

각 메서드의 복잡한 로직을 역할에 따라 여러 개의 작은 private 메서드로 분리하여 복잡도를 낮추고 가독성을 향상시켰습니다.

**`BaseAlienEntity.move()`**:

- `updateFrame(long delta)`: 프레임 애니메이션 로직
- `updateDirection(long currentTime)`: 방향 전환 로직
- `handleHorizontalMovement(double deltaSeconds, long currentTime)`: 수평 이동 및 경계 처리 로직
    - `onHorizontalBoundaryCollision(long currentTime)`: 수평 경계 충돌 시 처리 로직
- `handleVerticalMovement(long delta)`: 수직 이동 로직
- `clampYPosition()`: Y 좌표 범위 제한 로직

**`BaseSkillManager.getRandomSkillPoints()`**:

- 라운드 범위에 따라 `getRandomSkillPointsForRound1to2(double random)`, `getRandomSkillPointsForRound3to4(double random)`, `getRandomSkillPointsForRound5plus(double random)` 메서드로 분리하여 각기 다른 확률 계산 로직을 캡슐화했습니다.

**`NearEntity.move()`**:

- `updateTargetVelocity()`: 목표 속도 업데이트 로직
- `updateVelocity(double deltaSeconds)`: 현재 속도 업데이트 로직
- `updatePosition(double deltaSeconds)`: 위치 업데이트 로직
- `clampPosition()`: 좌표 범위 제한 로직

#### **변경 내용** :

위의 대상 메서드들이 여러 개의 작고 명확한 책임을 가진 메서드들로 분리되어 코드의 구조가 개선되고 이해하기 쉬워졌습니다.



## 4. room 패키지


### 🛠 **중복 코드 (Duplicated Code) - aa4cb740 커밋**

#### **냄새** : **중복 코드 (Duplicated Code)**

`GameClient.java`, `RoomListCanvas.java`, `RoomLobbyCanvas.java` 파일 내에서 `"roomId"`, `"single"`, `"Arial"`과 같은 문자열 리터럴이 여러 곳에서 중복 사용되고 있었습니다(java:S1192).

#### **대상** :

`GameClient.java`

`RoomListCanvas.java`

`RoomLobbyCanvas.java`

#### **적용 기법** :

**상수화**: 중복된 문자열 리터럴을 `private static final` 상수로 추출하여 코드의 일관성과 유지보수성을 높였습니다.

#### **변경 내용** :

**`GameClient.java`**: `KEY_ROOM_ID` ("roomId"), `ROOM_TYPE_SINGLE` ("single") 상수를 추가하고 관련 코드를 수정했습니다.

**`RoomListCanvas.java`**: `FONT_NAME` ("Arial") 상수를 추가하고 관련 코드를 수정했습니다.

**`RoomLobbyCanvas.java`**: `FONT_NAME` ("Arial") 상수를 추가하고 관련 코드를 수정했습니다.




### 🛠 **메서드 복잡도 감소 (Cognitive Complexity) - aa4cb740 커밋**

#### **냄새** : **높은 인지 복잡도 (High Cognitive Complexity)**

`GameClient.java`, `RoomListCanvas.java`, `RoomLobbyCanvas.java`, `BaseAlienEntity.java`, `BaseSkillManager.java`, `NearEntity.java` 클래스의 일부 메서드들이 너무 많은 `if-else` 분기문과 중첩된 로직을 가지고 있어 인지 복잡도가 높았습니다(java:S3776).

#### **대상** :

`GameClient.java`: `getSelfId()`, `handleGameInit()`, `handleRoomState()`

`RoomListCanvas.java`: `render()`

`RoomLobbyCanvas.java`: `render()`

`BaseAlienEntity.java`: `move()`

`BaseSkillManager.java`: `getRandomSkillPoints()`

`NearEntity.java`: `move()`

#### **적용 기법** : **메서드 추출 (Extract Method)**

각 메서드의 복잡한 로직을 역할에 따라 여러 개의 작은 private 메서드로 분리하여 복잡도를 낮추고 가독성을 향상시켰습니다.

#### **변경 내용** :

**`GameClient.java`**: `getSelfId()`를 `findSelfIdBySessionId()`, `findSelfIdByUsername()`으로, `handleGameInit()`을 `processPlayersInGameInit()`, `updateCachedSelfId()`로, `handleRoomState()`를 `parsePlayers()`, `resolveSelfId()`로 분리했습니다.

**`RoomListCanvas.java`**: `render()` 메서드를 `drawBackground()`, `drawTitle()`, `drawRoomList()`, `drawBottomButtons()` 등 여러 드로잉 관련 메서드로 분리했습니다.

**`RoomLobbyCanvas.java`**: `render()` 메서드를 `drawBackground()`, `drawTitle()`, `drawSinglePlayerUI()`, `drawMultiplayerUI()` 등으로 분리하고, `drawMultiplayerUI()`는 다시 `drawPlayerList()`, `drawHelp()`, `drawChat()`으로 세분화했습니다.

**`BaseAlienEntity.java`**: `move()` 메서드를 `updateFrame()`, `updateDirection()`, `handleHorizontalMovement()`, `handleVerticalMovement()`, `clampYPosition()` 등으로 분리했습니다.

**`BaseSkillManager.java`**: `getRandomSkillPoints()` 메서드를 라운드 범위에 따라 `getRandomSkillPointsForRound1to2()`, `getRandomSkillPointsForRound3to4()`, `getRandomSkillPointsForRound5plus()`로 분리했습니다.

**`NearEntity.java`**: `move()` 메서드를 `updateTargetVelocity()`, `updateVelocity()`, `updatePosition()`, `clampPosition()`으로 분리했습니다.




### 🛠 **직렬화 경고 수정 (Serialization-related Warnings) - aa4cb740 커밋**

#### **냄새** : **직렬화 불가능 필드 (Non-Serializable Fields)**

`Canvas`를 상속하는 `RoomListCanvas`와 `RoomLobbyCanvas` 클래스가 `Serializable`을 구현하지 않는 필드를 멤버로 가져 `java:S1948` 경고가 발생했습니다.

#### **대상** :

`RoomListCanvas.java`: `navigator`, `client`, `backgroundImage`, `keyAdapter`, `mouseAdapter` 필드

`RoomLobbyCanvas.java`: `navigator`, `client`, `players`, `backgroundImage`, `keyAdapter` 필드

#### **적용 기법** :

**`transient` 키워드 추가**: 직렬화 과정에서 제외되어야 하는 UI 관련 필드 및 핸들러에 `transient` 키워드를 추가하여 `NotSerializableException` 발생 가능성을 제거했습니다.




### 🛠 **스타일 경고 수정 (Style-related Warnings) - aa4cb740 커밋**

#### **냄새** : **스타일 위반 (Style Violation)**

`RoomListCanvas.java`의 `onRoomsUpdated`와 `handleButtonClick` 메서드에서 한 줄에 여러 `if` 문이 있거나 `else if`가 새 줄에 없어 스타일 경고(java:S3972)가 발생했습니다.

#### **대상** :

`RoomListCanvas.java`: `onRoomsUpdated()`, `handleButtonClick()` 메서드

#### **적용 기법** :

**코드 포맷팅**: `if` 및 `else if` 문을 표준 코드 스타일에 맞게 여러 줄로 나누어 가독성을 향상시켰습니다.




### 🛠 **Switch 문 default 케이스 추가 - aa4cb740 커밋**

#### **냄새** : **`switch` 문에 `default` 케이스 없음 (Missing 'default' case)**

`RoomListCanvas.java`, `RoomLobbyCanvas.java`의 `keyPressed` 메서드 내 `switch` 문에 `default` 케이스가 없어 예외적인 값에 대한 처리가 누락될 수 있었습니다(java:S131).

#### **대상** :

`RoomListCanvas.java`: `keyPressed()` 메서드 내 `switch` 문

`RoomLobbyCanvas.java`: `keyPressed()` 메서드 내 `switch` 문

#### **적용 기법** :

**`default` 케이스 추가**: 각 `switch` 문에 비어 있는 `default` 케이스를 추가하여 모든 가능한 입력에 대해 명시적으로 처리하도록 하여 코드의 안정성을 높였습니다.



## 5. server 패키지


### 🛠 **서버 측 중복 리터럴 및 복잡도 리팩토링 - a37bb1e1 커밋**

#### **냄새** : **중복 코드 (Duplicated Code) 및 높은 인지 복잡도 (High Cognitive Complexity)**

`ClientConnection`, `GameServer`, `ServerGameSession`, `ServerMultiplayerGame` 클래스에서 여러 중복된 문자열 리터럴과 복잡한 메서드가 발견되었습니다. 이로 인해 코드의 가독성이 저하되고 유지보수가 어려워졌습니다.

#### **대상** :

`ClientConnection.java`: `handleGameReady`, `handleGameInput`, `handleStateAck`, `handleStateRequest`, `handleGameAction`, `handleCreate`, `handleJoin`, `leaveRoomInternal`, `startGame`, `broadcastRoomState`, `sendRoomList`

`GameServer.java`: `startMaintenance`

`ServerGameSession.java`: `handleAction`, `persistLeaderboardIfNeeded`

`ServerMultiplayerGame.java`: `setupPlayerShips`, `update`, `createSnapshot`, `addAimedAlienShot`, `createSkillDrop`, `handleSkillUpgrade`, `notifyAlienKilled`, `mapPlayerStatesToSnapshot`

#### **적용 기법** :

**상수 추출 (Extract Constant)**: `ClientConnection`과 `ServerMultiplayerGame`에서 반복적으로 사용되는 문자열 리터럴(프로토콜 키, 파일 경로, 메시지)을 상수로 추출하여 코드의 일관성과 가독성을 높였습니다.

**메서드 추출 (Extract Method)**: `GameServer`, `ServerGameSession`, `ServerMultiplayerGame`의 복잡한 메서드들을 더 작고 명확한 책임을 가진 여러 개의 private 메서드로 분리하여 인지 복잡도를 낮추고 코드 구조를 개선했습니다.

#### **변경 내용** :

**`ClientConnection.java`**: `roomId`, `hostId`, `single`과 같은 프로토콜 관련 문자열을 `KEY_ROOM_ID`, `KEY_HOST_ID`, `KEY_SINGLE` 상수로 대체했습니다. 로깅 메시지의 `") by "` 리터럴을 `LOG_BY_USERNAME` 상수로 대체하고, `sendRoomList` 메서드에서 `"single"` 문자열 리터럴을 `KEY_SINGLE` 상수로 대체했습니다.

**`GameServer.java`**: `startMaintenance` 메서드의 로직을 `cleanupStaleRooms`와 `cleanupInactiveSessions` 메서드로 분리했습니다.

**`ServerGameSession.java`**:

- `handleAction`의 `switch`문의 각 `case`를 `handleRoundReadyAction`, `handleChatAction` 등의 개별 메서드로 추출했습니다.
- `persistLeaderboardIfNeeded`에서 플레이어 이름 수집 로직을 `getPlayerNamesForLeaderboard` 메서드로 추출했습니다.

**`ServerMultiplayerGame.java`**:

- `"sprites/shot.gif"`를 `DEFAULT_WEAPON_SKIN` 상수로, `"스킬 포인트가 부족합니다! (필요: "`를 `INSUFFICIENT_SKILL_POINTS_MSG` 상수로 대체했습니다. `", 보유: "`를 `AVAILABLE_SKILL_POINTS_MSG_SUFFIX` 상수로, `", 레벨: "`를 `LEVEL_MSG_PREFIX` 상수로 대체했습니다.
- `setupPlayerShips` 메서드를 `cleanAndEnsurePlayerRuntimes`, `determineSpawnOrder`, `spawnShips`, `assignFallbackShip` 메서드로 분리했습니다.
- `update` 메서드를 `updateGameLogic`, `updatePlayerShipStates`, `performCollisionChecks`, `updatePrimaryShipReference` 메서드로 분리했습니다.
- `createSnapshot`에서 플레이어 상태를 스냅샷으로 변환하는 로직을 `mapPlayerStatesToSnapshot` 메서드로 추출했습니다.
- `notifyAlienKilled` 메서드를 `handleKillerRewardsAndDrops`, `updateAlienStateAndCheckWinCondition`, `countRemainingAliens`, `adjustAlienSpeed` 메서드로 분리했습니다.
- `mapPlayerStatesToSnapshot` 메서드의 인지 복잡도를 줄이기 위해 `getSkillValue` 및 `getSkillTimeRemaining` 헬퍼 메서드를 추출하고 이를 사용하여 플레이어 스킬 값을 검색합니다. `java.util.function.Function` import를 추가했습니다.
