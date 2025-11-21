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
