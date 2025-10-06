# MULTYPLAY DESIGN (Phase 1 Draft)

## 0. 목표
- 싱글플레이(`gameplay`)와 **동일한 UX/라운드/보스/스킬/업그레이드**를 4인 협동으로 진행.
- 서버(authoritative)가 모든 전투/라운드/충돌/보상 로직을 판단.
- 클라이언트는 입력 전송 + 예측 + 스냅샷 기반 보간.
- 기존 싱글 코드 변경 금지: `org.newdawn.spaceinvaders.multyplay` 새 패키지에 fork.

## 1. 아키텍처 개요
```
Client(MultiGameCanvas)
  Input -> MultiNetworkAdapter -> GameClient -> Server
  Snapshot <- MultiNetworkAdapter <- GameClient <- Server(GameSession Tick)
  Predict/Interpolate -> Render

Server(GameSession per Room)
  Collect Inputs
  Run Tick (60Hz) - Move Entities, Spawn, Collision, HP/Score, Round
  Every 100ms -> Broadcast GAME_SNAPSHOT
```

## 2. 주요 컴포넌트 (클라이언트)
| 컴포넌트 | 역할 |
|----------|------|
| MultiGameCanvas | 싱글 Game fork. 입력 수집/예측/렌더/스냅 적용 |
| MultiGameState | PlayerState 집합 + RemoteEntity 맵 + 라운드/진행 상태 |
| MultiNetworkAdapter | GameClient 사용, 입력 전송 / 스냅샷 파싱 / 이벤트 큐 |
| InterpolationBuffer | 최근 스냅샷 N개 저장 후 t+α 보간 |
| PredictionSystem | 로컬 플레이어 위치/쿨타임 예측 (Fire, Move) |
| SnapshotApplier | 수신 스냅샷 → 상태/엔티티 diff 반영 |

## 3. 서버 확장 요소
| 항목 | 설명 |
|------|------|
| GameSession | Room 별 런타임 게임 컨텍스트 (tick thread) |
| EntityState | 서버 authoritative 엔티티 상태 구조 (id,type,x,y,dx,dy,hpOpt) |
| InputQueue | 플레이어별 최근 입력 (move,fire,timestamp) |
| RoundManager | 라운드/스폰/보스/속도증가 관리 |
| CollisionSystem | Ship vs Alien, Shot vs Alien/Boss, AlienShot vs Ship |
| SkillSystem | 드롭 결정, 스킬 포인트 지급, 업그레이드 적용 |

## 4. 패킷 포맷 (텍스트, 구분자 '|')
### 4.1 입력 전송 (클라이언트 -> 서버)
```
GAME_INPUT|sid=<sessionId>|dx=< -1|0|1 >|fire=<0|1>|t=<clientMillis>
```
- dx: 좌우 이동 의도 (동시 입력 처리 단순화)
- fire: 1이면 발사 요청(쿨타임은 서버가 판단)

### 4.2 스냅샷 (서버 -> 클라이언트)
```
GAME_SNAPSHOT|t=<serverTime>|round=<r>|players=<plist>|ents=<elist>
```
- players: `sid,hp,maxHp,atk,aspd,sp;...` (세미콜론 구분)
- ents: `id,type,x,y,dx,dy,ex` 목록 (ex=추가정보비트, 필요 시 HP 등)
  - type 예: P (PlayerShip), A (Alien), B (Boss), S (Shot), AS (AlienShot), M (Missile), X (Explosion)

### 4.3 이벤트
```
GAME_EVENT|t=...|etype=<WIN|LOSE|ROUND|DROP|MSG>|data=<escaped>
```
### 4.4 업그레이드 / 스킬 커맨드
```
GAME_UPGRADE|sid=...|kind=<ATK|ASPD|HP>
GAME_SKILL|sid=...|kind=<INV|PIERCE|TRIPLE|MISSILE>
```
서버 성공 시 다음 스냅샷에 수치/버프 반영.

### 4.5 히트(선택적 별도 브로드캐스트)
충돌은 스냅샷 수치로 충분; 별도 패킷은 디버그용 선택.

## 5. Tick & 빈도
| 항목 | 빈도 |
|------|------|
| 서버 GameSession tick | 60Hz (≈16.6ms) |
| Snapshot Broadcast | 10Hz (100ms) |
| 클라 입력 전송 | 60Hz (프레임 또는 변화 시) |
| 보간 타겟 지연 | ~100~120ms (2 스냅샷 사이) |

## 6. 예측 & 보간
- 로컬 Ship: dx 기반 즉시 위치 반영; 서버 스냅이 ±20px 이상 차이나면 스냅 보정, 아니면 서서히 보간.
- 원격 Ship & 적: 선형 보간 (이전/다음 스냅). 도달 안 된 엔티티는 스냅 위치로 순간 이동.
- 탄환: 예측하지 않고 서버 스냅 좌표만 사용 (life-cycle 짧음). 필요 시 클라 생성 시점에 시작 위치 예측 추가 가능.

## 7. 라운드 & 보스
- 서버 RoundManager가 스폰 → 스냅샷에 포함, 클라는 그리기만.
- 보스 HP 등 필요 시 `ents` 항목 ex 비트 또는 별도 타입 확장.

## 8. 업그레이드 & 스킬
- 클라 선택 → GAME_UPGRADE or GAME_SKILL 전송.
- 서버 검증 (포인트 충분 / 인벤토리 보유 / 쿨타임 등) → 상태 갱신 → 스냅샷 반영.
- 지속 효과 (무적/관통/트리플)는 서버가 만료 시간 관리, 스냅샷에 flag 비트 필요 시 확장:
  - players 필드 확장안: `sid,hp,maxHp,atk,aspd,sp,flags`
  - flags bit: 1=INV,2=PIERCE,4=TRIPLE

## 9. 엔티티 ID 규칙
- 서버: long 증가 id.
- 클라: 수신 없던 id는 새 RemoteEntity 생성.
- 제거: 스냅엔 존재하지 않고 이전 프레임 존재 시 제거 (또는 삭제 리스트 별도 제공).

## 10. 싱글과 동일해야 하는 요소 체크리스트 (최종 검증)
| 요소 | 기준 |
|------|------|
| 라운드 증가 메시지 | 동일 텍스트 / 타이밍 (waitingForKeyPress) |
| Boss 등장/처치 메시지 | 동일 문자열 (⚠️, 🎉 등) |
| 스킬 인벤토리 UI | 동일 아이콘, 수치 반영 딜레이 ≤ 1 스냅 주기 |
| 업그레이드 비용/성장 | 동일 공식 (Cost scaling) |
| 공격 속도/쿨타임 | 서버 계산이 싱글 공식과 일치 |
| Alien 속도 증가 로직 | 동일 퍼센트/라운드 보정 |
| Skill Drop 확률 | 동일 공식 (라운드 기반) |
| HP/바 표시 | 숫자 및 비율 동일 |

## 11. 단계별 Acceptance Criteria (요약)
1. Fork 구조만 생성, 컴파일 OK.
2. 서버 GameSession tick에서 Alien 이동 로그 출력.
3. 클라 2개 실행 시 서로 플레이어 위치 보임.
4. Alien/라운드 진행 & Boss 스폰/메시지 공유.
5. 발사/충돌/HP 감소 동기화.
6. 업그레이드/스킬 반영 & 지속시간 만료 정상.
7. 보간 후 시각적 튀는 현상 허용 오차 이하.
8. 체크리스트 전 항목 Pass.

## 12. 위험 & 대응
| 위험 | 완화 |
|------|------|
| 텍스트 패킷 크기 증가 | 간단한 필드명/구분자 사용, 필요 시 later 압축 |
| 스냅 드리프트 | 위치 threshold + 즉시 재동기화 |
| 서버 틱 지연 | 단순 로직 유지 / 컬렉션 동기화 최소화 |
| 명령 유실 | 주기적 전체 스냅으로 상태 복구 |

## 13. 향후 최적화 후보
- 증분 스냅샷 (변화 엔티티만 전송)
- 탄환 클라이언트 예측 & 서버 확인
- UDP 전환 (현재 TCP 기반 소켓 유지)

---
(Phase 1 Draft 완료: 이후 패키지/코드 생성 단계 진행)
