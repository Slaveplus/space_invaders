# Multiplayer Refactor Preparation

이 문서는 기존 싱글 전용 gameplay 구조를 멀티플레이 지원 구조로 전환하기 위한 1차 리팩토링 내용을 요약합니다.

## 1. 적용된 주요 변경 사항

### 패키지 포크 및 재구성
- 기존 `org.newdawn.spaceinvaders.gameplay` 패키지를 그대로 복제해 `org.newdawn.spaceinvaders.multyplay`로 분리.
- Canvas/루프/유틸을 `core`, 입력은 `input`, 상태는 `state`, UI 렌더링은 `ui`, 엔티티는 `entity`, 네트워크 DTO는 `net` 하위로 재배치.
- `Game` → `MultiplayerGameCanvas`, `GameStateManager` → `MultiplayerGameStateManager`, `InputManager` → `MultiplayerInputManager`, `SkillManager` → `MultiplayerSkillManager`, `UIRenderer` → `MultiplayerUIRenderer` 등 멀티 전용 클래스명으로 정리.

### PlayerState 추출
- `MultiplayerGameStateManager` 내부에 직접 존재하던 공격력/속도/HP/스킬포인트 수치를 `PlayerState` 클래스로 분리.
- 다수 플레이어 지원을 위해 `Map<String, PlayerState>` 구조 도입.
- 기존 단일 플레이어 getter/setter는 **레거시 호환용**으로 유지 (점진적 제거 예정).

### MultiplayerGameStateManager 확장
- `localPlayerId` 개념 추가 (현재 기본값 `"local"`).
- `ensurePlayer`, `getPlayerStates` API 제공.
- 라운드/엔티티 관련 기존 로직은 변경 최소화.

### Entity 확장
- 고유 `entityId` (AtomicLong) 및 `ownerId` 필드 추가.
- 스냅샷 직렬화용 `toSnapshot()`, `applySnapshot()` 제공.
- `EntitySnapshot` 별도 공개 클래스 분리 (네트워크 계층 DTO).

### 네트워크 추상 계층 (gameplay.net)
- `GameNetworkAdapter` 인터페이스: 서버/클라이언트/로컬 모드 공용.
- DTO: `PlayerInput`, `GameEvent`, `GameSnapshot` (+ 내부 PlayerScalarState).
- `LocalLoopbackNetworkAdapter`: 싱글/테스트 환경에서 네트워크 없는 모의 동작.

## 2. 앞으로의 추천 단계
1. `MultiplayerGameCanvas` 루프 내에서 `GameNetworkAdapter.tick()` 호출 지점 정리 (authoritative 스냅샷 생성 or 수신 적용).
2. 입력 처리: `MultiplayerInputManager`가 직접 ship 제어 대신 `PlayerInput` DTO 생성 -> 어댑터 전송 -> 서버 authoritative 반영 구조 도입.
3. 서버/클라이언트 분기:
   - 서버: 충돌/엔티티 스폰/라운드 진행 authoritative.
   - 클라이언트: 수신 스냅샷 기반 보간 렌더링, 로컬 예측(선택).
4. 동기화 전략:
   - 저빈도(초당 10~20회) 전체 스냅샷 + 입력 기반 보간.
   - 또는 이벤트 기반 증분 패킷 (Optimization 단계에서 고려).
5. 충돌 판정 분리: `CollisionSystem` 유틸 클래스로 추출 (서버 전용 권장).
6. 발사/스킬 사용: 현재 `MultiplayerGameCanvas`가 직접 수행 -> Command(Event) 객체화 후 서버 검증.
7. 룸 로비(`room` 패키지)와 멀티플레이 연결: 룸 진입 시 `localPlayerId`를 `MultiplayerGameStateManager`에 설정, 네트워크 어댑터를 방/서버 연결 정보로 교체.

## 3. 코드 전환 가이드
| 기존 | 변경 권장 | 비고 |
|------|-----------|------|
| MultiplayerGameStateManager.attackPower 등 필드 | PlayerState 통해 접근 | 레거시 메서드 제거 예정 |
| MultiplayerGameCanvas.tryToFire() 직접 발사 | 서버 Command 전송 -> 서버에서 생성 | 클라이언트는 예측 샷 선택적 |
| Entity 리스트 직접 공유 | authoritative 서버만 수정 | 클라: 수신 스냅샷 복제/보간 |
| InputManager가 바로 ship 이동 | PlayerInput 전송 + 예측 | latency hiding |

## 4. 보간/예측 기본 아이디어
- 클라이언트 로컬 이동: 입력 즉시 반영, 서버 스냅샷 도착 시 차이 보정 (position error threshold 적용).
- 투사체: 서버 생성 시간/위치 기준 클라 재생성. 필요시 latency 기반 time rewind.

## 5. 잠재적 새 컴포넌트 제안
- `AuthoritativeGameLogic` (서버만): update(delta) -> 엔티티 이동, 충돌, 라운드, 스폰.
- `ClientInterpolationBuffer`: 최근 N개 스냅샷 저장, 렌더 시 t+α 위치 보간.
- `Command` 계층: Fire, UseSkill, Move (continuous input는 별도), UpgradeSkill 등.

## 6. 추후 삭제 예정 (정리 대상)
- `MultiplayerGameStateManager`의 단일 플레이어 전용 getter/setter.
- `MultiplayerGameCanvas` 내부에서 상태 숫자 직접 조작하는 코드 (PlayerState 경유하도록 수정 예정).

## 7. 확장 포인트 요약
| 영역 | 확장 포인트 |
|------|-------------|
| 상태 | PlayerState Map, entityId/ownerId |
| 동기화 | GameNetworkAdapter 추상화 |
| 시각 | 로컬 보간/예측 레이어 추가 가능 |
| 명령 | 이벤트/커맨드 직렬화 구조 추가 예정 |

## 8. 다음 작업 체크리스트 (미완)
- [ ] Game loop에 network adapter hook 추가
- [ ] Fire/Skill/Upgrade -> Command 전환
- [ ] 서버/클라이언트 역할 분리 클래스 생성
- [ ] CollisionSystem 추출
- [ ] Snapshot 수신 시 로컬 엔티티 매핑/생성 로직 구현

---
문의/추가 요구사항이 있으면 이 문서 업데이트 후 진행할 수 있습니다.
