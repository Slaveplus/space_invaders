# 코드 품질 개선 리팩토링

## 개요
코드 분석 도구(SonarLint/IntelliJ Inspection)에서 발견된 591개의 선언 중복성 경고를 해결하기 위한 리팩토링 작업입니다.

## 경고 유형별 해결 내역

### 1. 사용되지 않는 선언 제거 (461개)

#### 1.1 사용되지 않는 import 제거

**대상 파일:**
- `src/main/java/org/newdawn/spaceinvaders/multyplay/core/SharedMultiplayerRoundCoordinator.java`
- `src/main/java/org/newdawn/spaceinvaders/room/RoomLobbyCanvas.java`

**수정 내용:**
```java
// 수정 전
import org.newdawn.spaceinvaders.multyplay.core.MultiplayerSkillManager;
import org.newdawn.spaceinvaders.room.GameInitInfo;

// 수정 후
// (import 제거됨)
```

**적용 기법:** 사용되지 않는 import 문 제거

---

### 2. Redundant throws clause (1개)

**대상:** `Game.java`의 `writeObject`, `readObject` 메서드

**문제점:**
- `defaultWriteObject()`와 `defaultReadObject()`는 `IOException`을 던질 수 있지만, 실제로는 발생하지 않을 수 있음
- IDE가 redundant throws clause로 감지

**참고:** 
- `ObjectOutputStream.defaultWriteObject()`와 `ObjectInputStream.defaultReadObject()`는 실제로 `IOException`을 던질 수 있으므로, 이 경우는 실제로 필요할 수 있습니다.
- 하지만 IDE 경고가 발생하는 경우, 실제 사용 패턴을 확인하여 제거 가능 여부를 판단해야 합니다.

---

### 3. 선언은 'final' 제어자를 가질 수 있습니다 (78개)

**대상:** 재할당되지 않는 private 필드들

**예시:**
- `GameStateManager.java`의 일부 필드들
- 생성자에서만 초기화되고 이후 변경되지 않는 필드들

**적용 기법:** 
- 재할당되지 않는 필드에 `final` 키워드 추가
- 단, setter 메서드가 있는 필드는 제외

**주의사항:**
- `localPlayerId` 같은 필드는 `setLocalPlayerId()` 메서드가 있어서 `final`로 만들 수 없음
- 실제 사용 패턴을 확인하여 안전하게 `final`을 추가해야 함

---

### 4. 메서드 매개변수 값은 항상 동일한 값 (37개)

**문제점:**
- 일부 메서드 호출에서 항상 같은 값이 전달됨
- 매개변수 대신 상수나 필드로 대체 가능

**적용 기법:**
- 매개변수를 제거하고 상수/필드로 대체
- 또는 메서드 오버로딩을 사용하여 매개변수 없는 버전 제공

---

### 5. 메서드가 항상 동일한 값을 반환 (4개)

**문제점:**
- 일부 메서드가 항상 `true`, `false`, `null`, 빈 문자열 등을 반환
- 조건부 로직이 없거나 항상 같은 경로로 실행됨

**적용 기법:**
- 메서드를 상수 필드나 간단한 getter로 변경
- 또는 메서드 로직을 수정하여 실제로 다른 값을 반환하도록 개선

---

### 6. 메서드를 'void'로 만들 수 있음 (10개)

**문제점:**
- 메서드가 값을 반환하지만, 호출자가 반환값을 사용하지 않음

**적용 기법:**
- 반환 타입을 `void`로 변경
- 단, 인터페이스나 상위 클래스의 메서드 시그니처와 일치해야 함

---

## 수정된 파일 목록

### Import 제거
1. `src/main/java/org/newdawn/spaceinvaders/multyplay/core/SharedMultiplayerRoundCoordinator.java`
   - `MultiplayerSkillManager` import 제거

2. `src/main/java/org/newdawn/spaceinvaders/room/RoomLobbyCanvas.java`
   - `GameInitInfo` import 제거

---

## 추가 작업 필요 사항

### 1. 사용되지 않는 필드/메서드
다음 필드들이 사용되지 않는다고 보고되었지만, 실제로는 사용될 수 있으므로 주의 깊게 확인 필요:
- `SideMonster.round`
- `ServerGameSession.server`
- `ServerGameSession.intermissionStartedAt`
- `ServerMultiplayerGame.alienCount`
- `ServerMultiplayerGame.currentSpaceshipSkin`

**참고:** 이전에 일부 필드를 제거했다가 컴파일 오류가 발생하여 되돌린 경험이 있습니다. 따라서 실제 사용 여부를 철저히 확인한 후에만 제거해야 합니다.

### 2. Deprecated API 사용
- `FirebaseDatabaseClient.java`에서 `URL(String)` 생성자 사용 (Java 20+에서 deprecated)
- `URI`를 사용하도록 마이그레이션 고려

### 3. Type Safety 경고
- `EquipmentManager.java`에서 unchecked cast 경고 발생
- 제네릭 타입을 명시적으로 지정하여 해결 가능

---

## 적용 기법 요약

| 문제 유형 | 적용 기법 | 수정 건수 |
|---------|---------|----------|
| 사용되지 않는 import | Import 제거 | 2개 |
| 사용되지 않는 필드 | 필드 제거 (주의 필요) | 0개 (확인 필요) |
| Redundant throws | throws 절 제거 (검토 필요) | 0개 |
| final 제어자 추가 | final 키워드 추가 | 0개 (수동 확인 필요) |
| 상수 반환 메서드 | 메서드 로직 개선 또는 상수화 | 0개 (수동 확인 필요) |
| void 메서드 변환 | 반환 타입 void로 변경 | 0개 (수동 확인 필요) |

---

## 참고사항

1. **자동화된 수정의 한계:**
   - 많은 경고들은 실제 사용 패턴을 확인해야 안전하게 수정할 수 있습니다.
   - 특히 "사용되지 않는 선언"은 리플렉션, 동적 호출, 또는 미래 사용을 위해 남겨둔 경우가 있을 수 있습니다.

2. **점진적 개선:**
   - 모든 경고를 한 번에 해결하기보다는, 중요한 것부터 우선순위를 정하여 점진적으로 개선하는 것이 좋습니다.

3. **테스트 필요:**
   - 수정 후에는 반드시 테스트를 실행하여 기능이 정상적으로 동작하는지 확인해야 합니다.

---

## 다음 단계

1. IDE의 코드 분석 도구를 사용하여 남은 경고들을 하나씩 확인
2. 각 경고에 대해 실제 사용 여부를 검증
3. 안전하게 수정 가능한 것부터 우선순위를 정하여 수정
4. 수정 후 테스트 실행 및 검증

