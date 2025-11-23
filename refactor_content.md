
### 문제 명
리터럴의 과도한 사용 (Excessive Use of Literals)
냄새: 리터럴의 과도한 사용 (Excessive Use of Literals)
냄새에 대한 자세한 내용: 코드 내에 마법의 숫자나 문자열 리터럴이 너무 많이 사용되어 가독성과 유지보수성을 저해하는 경우.
대상: 
- `LoginScreen.java`
- `SkillMenuRenderer.java`
- `MainMenuAssets.java`
- `ShotEntity.java`
- `NearEntity.java`
- `AlienEntity.java`
- `BaseBossShotEntity.java`
- `ExplosionEntity.java`
- `HeatEffectEntity.java`
- `BaseSkillEntity.java`
- `BossConfigFactory.java`
- `CoinDisplayEntity.java`
- `Round4GreenSphereAttack.java`
- `IceBallAttack.java`
- `Round3StraightAttack.java`
- `Round4PlayerLineAttack.java`
적용 기법: 문자열 리터럴을 상수로 추출하여 `SpriteConstants` 클래스에 정의하고, 해당 상수를 사용하도록 코드를 수정.
개선 전 코드(핵심부분)
```java
// LoginScreen.java
InputStream inputStream = getClass().getClassLoader().getResourceAsStream("sprites/backgrounds/Background-0.jpg");

// SkillMenuRenderer.java
imagePath = "sprites/Force/Force True.png";
```
개선 후 코드(핵심부분)
```java
// LoginScreen.java
InputStream inputStream = getClass().getClassLoader().getResourceAsStream(SpriteConstants.BACKGROUND_0_JPG);

// SkillMenuRenderer.java
imagePath = SpriteConstants.FORCE_TRUE_PNG;
```
