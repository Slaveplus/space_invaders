# **필드 섀도잉 리팩토링**

## **냄새 : 부모 필드 가리기 (Field Shadowing)**
`RemoteShotEntity` 클래스의 `spritePath` 필드가 부모 클래스 `Entity`의 필드를 가리고 있어 코드 이해를 방해하고 잠재적 오류를 유발했습니다.

## **대상** : `MultiplayerGameCanvas`의 내부 클래스 `RemoteShotEntity`

## **적용기법** : `RemoteShotEntity`의 중복 `spritePath` 필드를 제거하고 부모 클래스의 필드를 사용하도록 상속 구조를 활용했습니다. 이로 인해 불필요한 초기화 및 중복된 스킨 변경 로직을 정리하여 코드를 단순화했습니다.

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
---

### **정적 분석 경고 수정 (SonarLint)**

#### **냄새** : **빈 메서드 (Empty Methods) & 사용되지 않는 파라미터 (Unused Parameter)**

`MultiplayerGameCanvas.java` 파일 내 다수의 내부 클래스에서 `java:S1186` (메서드는 비어 있어서는 안 됩니다)와 `java:S1172` (사용되지 않는 메서드 매개변수 제거) 경고가 발생했습니다.
- `Remote*` 시리즈 내부 클래스들의 `move()`, `collidedWith()` 메서드가 비어 있었습니다. 이 메서드들은 원격 엔티티의 시각적 표현만을 담당하므로, 로컬에서 로직을 처리할 필요가 없어 의도적으로 비워둔 것입니다.
- `addScore()` 메서드가 내용 없이 주석만 있었습니다.
- `RemoteRound4GreenSphere` 생성자가 사용되지 않는 `meta` 파라미터를 가지고 있었습니다.

#### **대상** : `MultiplayerGameCanvas.java` 내의 여러 내부 클래스

- `RemoteIceAttack`, `RemoteIceBallAttack`, `RemoteMagneticField`, `RemoteRound2Laser`, `RemoteRound2Phase1`, `RemoteRound2Phase2`, `RemoteRound2Random`, `RemoteRound2Quad`, `RemoteRound2MachineGun`, `RemoteRound3Straight`, `RemoteRound3Random`, `RemoteRound3Pull`, `RemoteRound3BlackHole`, `RemoteRound4Heal`, `RemoteRound4GreenSphere`, `RemoteRound4PlayerLine`, `RemoteExplosionEntity`
- `addScore(String playerId, int points)`
- `RemoteRound4GreenSphere` 생성자 및 호출부

#### **적용 기법 :**
- **주석 추가**: SonarLint 경고를 해결하기 위해, 의도적으로 비워둔 모든 `move()` 및 `collidedWith()`, `addScore()` 메서드에 `// Method is intentionally empty.` 주석을 추가하여 코드의 의도를 명확히 했습니다.
- **파라미터 제거**: `RemoteRound4GreenSphere` 생성자에서 사용되지 않는 `meta` 파라미터를 제거하고, `createRemoteEntity` 메서드 내의 해당 생성자 호출 코드도 함께 수정하여 불필요한 코드를 정리했습니다.

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
---
### **상수화 (MultiplayerGameCanvas.java)**

#### **냄새**: **중복 코드 (Duplicated Code)**

`MultiplayerGameCanvas.java` 파일 내에서 이미지 경로, UI 텍스트, 설정 키 등의 문자열 리터럴이 여러 곳에서 중복 사용되고 있었음. 이로 인해 유지보수 및 변경 시 오류 발생 가능성이 높았음.

#### **대상**:
*   "sprites/Boss_Attack/ice ball.gif"
*   "sprites/Skill/Heat.gif"
*   "sprites/ship.gif"
*   "sprites/Boss_Attack/2round1.gif"
*   "sprites/shot.gif"
*   "sprites/Skill/Explosion.png"
*   "클라이언트: 원격 플레이어 "
*   "Arial"
*   "radius"
*   "🏆 GAME COMPLETED! 🏆 Congratulations!"

#### **적용 기법**:
*   **클래스 내부 상수화**: `MultiplayerGameCanvas` 내에서만 사용되는 UI 텍스트 및 설정 키 ("클라이언트: 원격 플레이어 ", "Arial", "radius", "🏆 GAME COMPLETED! 🏆 Congratulations!")는 `private static final String` 상수로 정의하여 클래스 내에서 관리되도록 함.
*   **외부 상수 클래스 분리**: 이미지 파일 경로는 재사용 가능성이 높고 특정 도메인(스프라이트)에 속하므로, `org.newdawn.spaceinvaders.common.sprite.SpriteConstants`라는 새로운 상수 클래스를 생성하고 `public static final String` 상수로 정의하여 관리되도록 함. 이를 통해 이미지 경로의 중앙 집중식 관리가 가능해지고 가독성이 향상됨.

#### **변경 내용**:
*   `MultiplayerGameCanvas.java` 파일 상단에 `SpriteConstants` import 문 추가.
*   `MultiplayerGameCanvas.java` 클래스 내부에 `REMOTE_PLAYER_LABEL`, `FONT_ARIAL`, `RADIUS_KEY`, `GAME_COMPLETED_MESSAGE` 상수를 선언하고 중복된 문자열 리터럴을 해당 상수로 대체.
*   `org.newdawn.spaceinvaders.common.sprite.SpriteConstants.java` 파일을 생성하고 `ICE_BALL_GIF`, `HEAT_GIF`, `SHIP_GIF`, `ROUND_2_ATTACK_1_GIF`, `SHOT_GIF`, `EXPLOSION_PNG` 상수를 선언하고 `MultiplayerGameCanvas.java` 내의 중복된 이미지 경로 문자열 리터럴을 해당 상수로 대체.
---
### **상수화 (MultiplayerInputManager.java)**

#### **냄새**: **중복 코드 (Duplicated Code)**

`MultiplayerInputManager.java` 파일 내에서 스킬 메시지 관련 문자열 리터럴이 여러 곳에서 중복 사용되고 있었음. 이로 인해 유지보수 및 변경 시 오류 발생 가능성이 높았음.

#### **대상**:
*   "스킬 포인트가 부족합니다! (필요: "
*   ", 레벨: "
*   ", 보유: "

#### **적용 기법**:
*   **클래스 내부 상수화**: `MultiplayerInputManager` 내에서만 사용되는 스킬 메시지 관련 텍스트는 `private static final String` 상수로 정의하여 클래스 내에서 관리되도록 함.

#### **변경 내용**:
*   `MultiplayerInputManager.java` 클래스 내부에 `SKILL_POINT_LACK_PREFIX`, `SKILL_LEVEL_SUFFIX`, `SKILL_POINT_HOLD_SUFFIX` 상수를 선언하고 중복된 문자열 리터럴을 해당 상수로 대체.
---
### **생성자 복잡도 감소 (MultiplayerGameCanvas)**

#### **냄새**: **높은 인지 복잡도 (High Cognitive Complexity)**

`MultiplayerGameCanvas` 클래스의 생성자는 캔버스 설정, 컴포넌트 초기화, 입력 핸들러 등록 등 여러 역할을 동시에 수행하고 있어 인지 복잡도가 20으로 높았습니다. 이로 인해 코드의 가독성과 유지보수성이 저하되었습니다.

#### **대상**: `MultiplayerGameCanvas` 생성자

#### **적용 기법**: **메서드 추출 (Extract Method)**

생성자의 각기 다른 책임들을 별도의 private 메서드로 분리했습니다.
-   `initCanvas()`: 캔버스 관련 설정을 담당합니다.
-   `initComponents()`: `gameStateManager`, `skillManager`, `uiRenderer` 등 핵심 컴포넌트들의 초기화를 담당합니다.
-   `initInputHandlers()`: 키보드 및 마우스 입력 핸들러 등록을 담당합니다.

이를 통해 생성자의 복잡도를 낮추고, 각 초기화 단계의 역할을 명확히 하여 코드의 가독성과 유지보수성을 향상시켰습니다.

#### **변경 내용**:
*   `MultiplayerGameCanvas` 생성자의 내용을 `initCanvas()`, `initComponents()`, `initInputHandlers()`, `initEntities()` 메서드 호출로 변경.
*   `initCanvas()`, `initComponents()`, `initInputHandlers()` private 메서드를 새로 생성하여 기존 생성자의 초기화 로직을 분리.
