# Multiplayer (multyplay) Design

본 문서는 싱글플레이 기존 코드와 **완전 분리**된 `multyplay` 패키지 구조, 프로토콜, 현재 구현 상태, 그리고 향후 개선 포인트를 기술한다.

## 1. 목표
- 기존 싱글 플레이 코드 변경 없이 병렬 멀티플레이 구조 추가
- 서버 권위(Server Authoritative) + 클라이언트 단순 위치 예측 (v1)
- 최대 4인 확장 대비: 슬롯 기반 입력/상태 구조 (`slot = 0..3`)
- 텍스트 기반(사람이 읽기 쉬운) 파이프 구분 프로토콜 (디버그 용이성 우선)

## 2. 패키지 구조 개요
```
org.newdawn.spaceinvaders.multyplay
  core/        -> 메인 런타임 캔버스, 컨트롤러
  entity/      -> 멀티 전용 엔티티(Ship, Alien, Boss 등) 포크본
  net/         -> 네트워크 어댑터, 메시지 DTO, 스냅샷 직렬화
  state/       -> 진행/라운드/플레이어 런타임 스테이트 (MultiGameState, PlayerRuntimeState)
  system/      -> 입력 수집 (MultiInputManager)
  ui/          -> HUD, 배경 렌더러 (MultiUIRenderer, MultiBackgroundRenderer)
```

## 3. 핵심 클래스 요약
| 컴포넌트 | 역할 |
|----------|------|
| `MultiGameCanvas` | 멀티플레이 전용 Canvas + Screen 구현. 루프에서 controller.tick(), network.poll() 호출 및 렌더|
| `MultiGameController` | 엔티티 업데이트/충돌/스폰/라운드 전개. 스냅샷 생성 지원 |
| `MultiGameState` | 라운드, 보스 플래그, 스킬 포인트, 점수/업그레이드 비용 등 메타 진행 상태 |
| `MultiInputManager` | 슬롯별 키 입력 수집 후 컨트롤러에 반영 |
| `MultiServerAdapter` | (초기 형태) 클라이언트 소켓 관리 + 주기적 SNAP 전송 |
| `MultiClientAdapter` | SNAP 수신 / IN 전송 / 위치 예측 & 재동기화 |
| `LocalLoopbackMultiAdapter` | 단일 프로세스(개발용) 서버-클라이언트 최소 파이프라인 시뮬레이션 |
| `SnapshotCodec` | 텍스트 프로토콜 인코딩/디코딩 |

## 4. 메시지 / 프로토콜

### 4.1 IN (클라이언트 입력)
```
IN|<slot>|<seq>|<left>|<right>|<fire>
예: IN|0|15|1|0|0
```
- `slot`: 플레이어 슬롯 (0..3)
- `seq`: 클라이언트 로컬 입력 시퀀스 (현재 서버 Ack 미구현) 
- `left/right/fire`: 0 또는 1

현재 서버 어댑터는 seq를 소비하지 않고 단순 마지막 입력 상태로만 처리 (확장 포인트).

### 4.2 SNAP (서버 스냅샷)
멀티라인 블록: `SNAP` 헤더 한 줄 + 엔티티/플레이어/종료 마커.
```
SNAP|<tick>
E|<id>|<type>|<x>|<y>|<dx>|<dy>
...
P|<slot>|<x>|<y>|<hp>|<sp>
...
END
```
- `tick`: 서버 틱 카운터 (ms 기반이 아닌 논리 틱 증가를 예상)
- `E` 라인: 렌더/충돌 필요한 엔티티 (type은 스프라이트 or 식별 문자열)
- `P` 라인: 플레이어 상태

### 4.3 향후 예정 메시지
| 코드 | 목적 | 상태 |
|------|------|------|
| `EVT|...` | 게임 이벤트(아이템 드랍, 보스 페이즈 등) | 미구현 |
| `ACK|<lastSeq>` | 서버가 반영한 마지막 입력 seq 회신 | 미구현 |
| `JOIN/LEAVE` | 런타임 인구 변화 통지 | 미구현 |

## 5. 클라이언트 예측 (v1)
- 단일 축(수평) 이동만 예측.
- 입력 전송 직후 로컬에서 동일한 속도 상수(0.4f)로 `predictedX` 변경 → Ship x 반영.
- 스냅샷 수신 시 slot0의 서버 좌표와 예측 좌표 차이 계산:
  - 오차 < 0.01 → 무시
  - 그 외 → 서버 좌표로 강제 세팅 후 (현재 pending 입력 전체 재적용) 재시뮬
- Ack 미구현으로 인해 스냅샷 수신마다 pending 입력 목록 전체 비움.

### 5.1 예측 한계
- 사격, 스킬, 충돌 결과(HP 변화) 미예측 → 서버 반영 기다림.
- 다인 지원 시 슬롯별 별도 예측 매니저 필요.
- 속도/충돌 논리가 서버와 다르면 드리프트 발생.

### 5.2 향후 개선
| 개선 항목 | 내용 |
|-----------|------|
| 입력 Ack | `ACK|<seq>` 수신 → 해당 seq 이하 pending 제거 (정확 재시뮬) |
| 보간/스무딩 | 큰 오차 스냅, 작은 오차 선형 보정 (lerp) |
| 다중 슬롯 확장 | 슬롯별 `predictedX[]`, pending 큐 분리 |
| Delta Snapshot | 전체 스냅 대신 변경분만 전송하여 대역폭 절감 |
| 압축 | 길이 최적화 (Huffman, zstd, Bit packing) |

## 6. 라운드 / 보스 진행
`MultiGameState`:
- `round`, `maxRound`, `bossRound` (ex: 5 라운드마다 보스)
- Alien 처치 → 카운터 0 시 `tryAdvanceRound()` 호출
- 보스 스폰/처치 이벤트 훅: `onBossSpawn()`, `onBossDefeated()`
- 스킬/업그레이드 비용: 배열/상수 기반 → 추후 서버 동기 별도 메시지로 분리 가능

## 7. 의존성 / 분리 원칙
- 기존 싱글 플레이 `gameplay` 패키지 직접 참조 금지 (포크본만 사용)
- 공유 자원 (이미지, 폰트)은 리소스 경로만 재사용
- 싱글 코드 변경 대신 멀티에서 필요한 기능(예: 엔티티 위치 보정용 `setPosition`)만 포크본에 구현

## 8. 현재 구현 상태 체크리스트
| 항목 | 상태 | 비고 |
|------|------|------|
| 분리된 패키지 스켈레톤 | ✅ | `multyplay/` 완성 |
| 엔티티 포크 | ✅ | Ship/Alien/Boss 등 기본 |
| 컨트롤러 & Canvas | ✅ | 틱/렌더 분리 |
| 스냅샷 직렬화 | ✅ | 전체 스냅 (delta X) |
| 로컬 루프백 | ✅ | 개발/디버그용 |
| 서버/클라이언트 어댑터 | ✅ | 단순 Broadcast 형태 |
| 라운드/보스 이식 | ✅ | 기본 반복 진행 |
| 위치 예측 v1 | ✅ | 단일 슬롯, 수평 전용 |
| 로비 → 멀티 캔버스 진입 | ✅ | MainMenu M 키 임시 트리거 |
| 문서 | 작성중 | 본 파일 초안 |

## 9. 향후 작업 우선순위 제안
1. 입력 Ack + 정확 재적용
2. 다중 플레이어 슬롯 지원 (로비 인원→동적 Ship 생성)
3. Boss/아이템/스킬 이벤트 전파(`EVT` 메시지)
4. Delta Snapshot & Lerp 보정
5. 보안(입력 스팸/좌표 변조 필터링)
6. 벤치마크/프로파일링 (대역폭, 틱 타이밍)

## 10. 간단 사용 흐름(MVP)
1. 앱 실행 → 메인 메뉴
2. 키보드 `M` → `startMultiplayerGame()` → `MultiGameCanvas` 생성
3. 로컬 루프백 어댑터가 내부 틱 수행 & SNAP 공급
4. 입력 발생 시 IN 전송(실제로는 로컬 큐) → 예측 적용 → 스냅샷 수신 시 보정

---
(끝) 향후 변경 시 이 문서 업데이트 필수.
