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