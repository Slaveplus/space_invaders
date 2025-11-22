### **서버 측 중복 리터럴 및 복잡도 리팩토링**

#### **냄새**: **중복 코드 (Duplicated Code) 및 높은 인지 복잡도 (High Cognitive Complexity)**

`ClientConnection`, `GameServer`, `ServerGameSession`, `ServerMultiplayerGame` 클래스에서 여러 중복된 문자열 리터럴과 복잡한 메서드가 발견되었습니다. 이로 인해 코드의 가독성이 저하되고 유지보수가 어려워졌습니다.

#### **대상**:
*   `ClientConnection.java`: `handleGameReady`, `handleGameInput`, `handleStateAck`, `handleStateRequest`, `handleGameAction`, `handleCreate`, `handleJoin`, `leaveRoomInternal`, `startGame`, `broadcastRoomState`, `sendRoomList`
*   `GameServer.java`: `startMaintenance`
*   `ServerGameSession.java`: `handleAction`, `persistLeaderboardIfNeeded`
*   `ServerMultiplayerGame.java`: `setupPlayerShips`, `update`, `createSnapshot`, `addAimedAlienShot`, `createSkillDrop`, `handleSkillUpgrade`, `notifyAlienKilled`, `mapPlayerStatesToSnapshot`

#### **적용 기법**:
*   **상수 추출 (Extract Constant)**: `ClientConnection`과 `ServerMultiplayerGame`에서 반복적으로 사용되는 문자열 리터럴(프로토콜 키, 파일 경로, 메시지)을 상수로 추출하여 코드의 일관성과 가독성을 높였습니다.
*   **메서드 추출 (Extract Method)**: `GameServer`, `ServerGameSession`, `ServerMultiplayerGame`의 복잡한 메서드들을 더 작고 명확한 책임을 가진 여러 개의 private 메서드로 분리하여 인지 복잡도를 낮추고 코드 구조를 개선했습니다.

#### **변경 내용**:
*   **`ClientConnection.java`**: `roomId`, `hostId`, `single`과 같은 프로토콜 관련 문자열을 `KEY_ROOM_ID`, `KEY_HOST_ID`, `KEY_SINGLE` 상수로 대체했습니다. 로깅 메시지의 `") by "` 리터럴을 `LOG_BY_USERNAME` 상수로 대체하고, `sendRoomList` 메서드에서 `"single"` 문자열 리터럴을 `KEY_SINGLE` 상수로 대체했습니다.
*   **`GameServer.java`**: `startMaintenance` 메서드의 로직을 `cleanupStaleRooms`와 `cleanupInactiveSessions` 메서드로 분리했습니다.
*   **`ServerGameSession.java`**:
    *   `handleAction`의 `switch`문의 각 `case`를 `handleRoundReadyAction`, `handleChatAction` 등의 개별 메서드로 추출했습니다.
    *   `persistLeaderboardIfNeeded`에서 플레이어 이름 수집 로직을 `getPlayerNamesForLeaderboard` 메서드로 추출했습니다.
*   **`ServerMultiplayerGame.java`**:
    *   `"sprites/shot.gif"`를 `DEFAULT_WEAPON_SKIN` 상수로, `"스킬 포인트가 부족합니다! (필요: "`를 `INSUFFICIENT_SKILL_POINTS_MSG` 상수로 대체했습니다. `", 보유: "`를 `AVAILABLE_SKILL_POINTS_MSG_SUFFIX` 상수로, `", 레벨: "`를 `LEVEL_MSG_PREFIX` 상수로 대체했습니다.
    *   `setupPlayerShips` 메서드를 `cleanAndEnsurePlayerRuntimes`, `determineSpawnOrder`, `spawnShips`, `assignFallbackShip` 메서드로 분리했습니다.
    *   `update` 메서드를 `updateGameLogic`, `updatePlayerShipStates`, `performCollisionChecks`, `updatePrimaryShipReference` 메서드로 분리했습니다.
    *   `createSnapshot`에서 플레이어 상태를 스냅샷으로 변환하는 로직을 `mapPlayerStatesToSnapshot` 메서드로 추출했습니다.
    *   `notifyAlienKilled` 메서드를 `handleKillerRewardsAndDrops`, `updateAlienStateAndCheckWinCondition`, `countRemainingAliens`, `adjustAlienSpeed` 메서드로 분리했습니다.
    *   `mapPlayerStatesToSnapshot` 메서드의 인지 복잡도를 줄이기 위해 `getSkillValue` 및 `getSkillTimeRemaining` 헬퍼 메서드를 추출하고 이를 사용하여 플레이어 스킬 값을 검색합니다. `java.util.function.Function` import를 추가했습니다.