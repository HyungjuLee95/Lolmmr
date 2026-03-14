# LOLMMR

리그 오브 레전드 전적 데이터를 기반으로, 소환사 검색 → 최근 랭크 경기 수집 → 자체 점수 계산 → 경기 상세 분석(팀/라인전/타임라인 지표)까지 제공하는 풀스택 프로젝트입니다.

---

## 1. 기술 스택

### Backend
- Java 21
- Spring Boot 3.4.2
- Spring Web (REST API)
- Spring Data JPA + H2 (in-memory)
- Spring Cache + Caffeine
- Spring Cloud OpenFeign
- Gradle

### Frontend
- React 19
- Vite 7
- Axios
- Recharts

### Infra / Runtime
- Riot Open API (`X-Riot-Token` 헤더 인증)
- Vite dev server proxy (`/api` → `http://localhost:8080`)

---

## 2. 외부 API 사용 범위

프로젝트에서 사용하는 Riot API 범주는 아래와 같습니다.

### 계정/소환사/리그 조회
- `GET /riot/account/v1/accounts/by-riot-id/{gameName}/{tagLine}`
- `GET /riot/account/v1/accounts/by-puuid/{puuid}`
- `GET /lol/summoner/v4/summoners/by-puuid/{puuid}`
- `GET /lol/league/v4/entries/by-puuid/{puuid}`

### 매치 조회
- `GET /lol/match/v5/matches/by-puuid/{puuid}/ids`
- `GET /lol/match/v5/matches/{matchId}`
- `GET /lol/match/v5/matches/{matchId}/timeline`

---

## 3. 전체 동작 흐름

1. 프론트에서 Riot ID(`gameName#tagLine`)로 `/api/mmr` 호출
2. 백엔드가 Riot API를 통해 `puuid`, 소환사 정보, 랭크 정보 조회
3. 최근 솔로/자랭 match id 조회 후 매치 상세 병렬 수집
4. `ScoreEngine`으로 자체 점수(`scoreResult`) 계산
5. 요약 지표(승/패/다시하기/제외, 승률, KDA) 생성
6. 프론트는 최근 표시 경기(기본 2경기) + 점수 산정 샘플(기본 10경기)을 함께 렌더링
7. 경기 카드 확장 시 `/api/matches/{matchId}/analysis`로 상세 분석 요청
8. 상세 분석에서 팀 오브젝트, 맞라인 비교, 지표 카드, 타임라인 버킷 그래프 데이터 생성

---

## 4. 핵심 API 명세

## 4.1 MMR 분석 조회
`GET /api/mmr?name={gameName}%23{tagLine}&queue=solo|flex|both`

### Request
- `name` (필수): Riot ID (`gameName#tagLine`)
- `queue` (선택): `solo`, `flex`, `both` (기본/비정상 값은 내부적으로 `both`)

### Response (요약)
- `summoner`: 소환사/랭크 정보
- `queues.solo`, `queues.flex`
  - `matchDetails`: 경기 리스트
  - `scoreResult`: 자체 점수 계산 결과
  - `summary`: 승/패/승률/KDA/집계 개수
  - `counts`: counted/remake/invalid/total
  - `standardMmr`: 티어 기반 기준 점수
- `meta`: 요청 queue / 해석 queue / counts

## 4.2 단일 경기 상세 분석
`GET /api/matches/{matchId}/analysis?puuid={puuid}&bucketMinutes=3|5|10`

### Response (요약)
- `summary`: 기본 경기 요약
- `blueTeamPlayers`, `redTeamPlayers`
- `blueTeamSummary`, `redTeamSummary` (킬/데스/총골드/오브젝트)
- `laneComparison`: 맞라인 지표 비교
- `metricCards`: KPI 카드
- `timelineBuckets`: 그래프용 시계열 데이터
- `coachingComments`: 텍스트 코멘트

---

## 5. 점수 계산 로직 (`ScoreEngine`)

> 기본점수 1000에서 시작, 경기별 델타를 누적해 최종 점수 산정

## 5.1 경기 반영 규칙
- `WIN`, `LOSS`만 점수 반영(`countedGame`)
- `REMAKE`, `INVALID`는 점수 계산 제외(델타 0)

## 5.2 경기별 점수 변화식
- 기본 델타
  - 승리: `+18`
  - 패배: `-18`
- 성과 점수(`performanceScore`, 0~100)
  - `KDA score = clamp((KDA / 3.5) * 100, 0, 100)`
  - `CS score = clamp((CS/min / role_cs_baseline) * 100, 0, 100)`
  - `GPM score = clamp((gold/min / role_gpm_baseline) * 100, 0, 100)`
  - `composite = 0.45*KDA + 0.30*CS + 0.25*GPM`
- 성과 델타
  - `centered = performanceScore - 50`
  - `normalized = centered / 5`
  - `weighted = round(normalized * roleWeight)`
  - 최종 범위 제한: `[-12, +12]`
- 최종 경기 델타
  - `finalDelta = baseDelta + performanceDelta`

## 5.3 포지션별 기준값
- Role Weight
  - TOP 1.00, JUNGLE 1.10, MIDDLE 1.00, BOTTOM 1.05, SUPPORT/UTILITY 0.95
- CS/min baseline
  - TOP 6.3, JUNGLE 5.8, MIDDLE 7.1, BOTTOM 7.7, SUPPORT/UTILITY 1.6
- Gold/min baseline
  - TOP 390, JUNGLE 410, MIDDLE 420, BOTTOM 435, SUPPORT/UTILITY 300

## 5.4 등급 기준
- `S+` ≥ 1280
- `S` ≥ 1200
- `A` ≥ 1120
- `B` ≥ 1040
- `C` ≥ 960
- `D` ≥ 880
- `F` < 880

---

## 6. 상세 분석 계산 로직 (`MatchAnalysisService`)

## 6.1 팀/라인전 비교
- 팀 요약: 팀별 K/D/총골드
- 오브젝트 집계
  - Dragon, Herald, Baron, Voidgrub, Tower
- 맞라인 비교
  - `csDiff = myCs - oppCs`
  - `goldDiff = myGold - oppGold`
  - `damageDiff = myDamage - oppDamage`

## 6.2 KPI 카드(`metricCards`) 산식
- 킬관여율(KP): `((킬+어시) / 팀킬) * 100`
- CS/min: `totalCs / 경기시간(분)`
- Gold/min: `goldEarned / 경기시간(분)`
- DPM: `damageToChampions / 경기시간(분)`
- 팀 내 딜 비중: `(내 챔피언딜 / 팀 챔피언딜) * 100`
- 시야 점수: Riot `visionScore`
- 죽은 시간: `totalTimeSpentDead`

각 카드에는 baseline과 0~100 스코어가 함께 계산됩니다.

## 6.3 타임라인 버킷(`timelineBuckets`) 생성
- 버킷 단위: 3/5/10분
- 우선순위
  1. 타임라인 API frame 기반 실제 버킷 계산
  2. 타임라인 미존재 시 최종 스탯 비율 분배로 추정 버킷 생성

### 성장 점수
- `growth = clamp(10 + goldC + csC + xpC + diffC, 0, 100)`
- `goldC = min(35, gold / 450)`
- `csC = min(25, cs / 6)`
- `xpC = min(20, xp / 280)`
- `diffC = clamp(goldDiff/180 + csDiff*0.9 + xpDiff/160, -15, 15)`

### 전투 점수
- `combat = clamp(40 + kills*10 + assists*5 - deaths*12, 0, 100)`

### 맵 점수
- `map = clamp(35 + wardsPlaced*2 + wardsKilled*3.5 + objectiveScore*5, 0, 100)`

### 생존 점수
- `survival = clamp(80 - deaths*12 + min(8, level*0.6), 0, 100)`

### 영향력 점수
- `impact = clamp(growth*0.35 + combat*0.30 + map*0.20 + survival*0.15, 0, 100)`

### 오브젝트 이벤트 점수(타임라인)
- DRAGON: 3
- RIFTHERALD: 4
- BARON_NASHOR: 6
- HORDE(공허 유충): 4
- 기타: 2
- 어시스트 참여 시 일부 가중치(절반/최소치) 반영

---

## 7. 그래프 항목 정의 (프론트 렌더링 기준)

## 7.1 레이더 차트
`metricCards`에서 아래 key를 사용
- `kp` (킬관여)
- `cspm` (CS)
- `gpm` (골드)
- `dpm` (딜량)
- `vision` (시야)
- `deadTime` (생존)

## 7.2 타임라인 라인차트
`timelineBuckets`에서 아래 항목을 시계열 비교
- 골드: `myGold` vs `opponentGold`
- 경험치: `myXp` vs `opponentXp`
- CS: `myCs` vs `opponentCs`
- 성장 점수: `myGrowthScore` vs `opponentGrowthScore`
- 전투 점수: `myCombatScore` vs `opponentCombatScore`
- 맵 점수: `myMapScore` vs `opponentMapScore`
- 생존 점수: `mySurvivalScore` vs `opponentSurvivalScore`
- 영향력 점수: `myImpactScore` vs `opponentImpactScore`

---

## 8. 설정 정보

## 8.1 현재 주요 설정 키
- `riot.api.key`: Riot API 인증키
- `dev.allowlist.enabled`: 개발 허용 목록 사용 여부
- `dev.allowlist.riotIds`: 개발 허용 Riot ID 목록
- `score.calibration-log-enabled`: 점수 보정 로그 출력 여부

## 8.2 캐시
- 캐시 대상: `summonerInfo`, `matchIds`, `matchRaw`, `matchDetail`, `matchTimelineRaw`
- Caffeine TTL: 10분
- `maximumSize`: 5000

---

## 9. Pull 이후 로컬 실행/테스트 가이드

아래 순서는 **코드 pull 직후 검증**을 기준으로 작성했습니다.

## 9.1 코드 최신화
```bash
git checkout <작업브랜치>
git pull origin <작업브랜치>
```

## 9.2 Backend 시작
```bash
./gradlew clean bootRun
```

> 기본 포트: `8080`

## 9.3 Frontend 시작
```bash
cd frontend
npm install
npm run dev
```

> 기본 포트: `5173` (Vite proxy로 `/api` 요청은 백엔드 `8080`으로 전달)

## 9.4 로컬 접속
- Frontend: `http://localhost:5173`
- Backend 헬스체크(간단): `http://localhost:8080/api/mmr?name=<RiotID>&queue=solo`

## 9.5 테스트 명령어

### Backend
```bash
./gradlew test
./gradlew build
```

### Frontend
```bash
cd frontend
npm run build
```

## 9.6 추천 테스트 시나리오 (수동 점검)

1. **기본 검색 플로우**
   - 프론트에서 `gameName#tagLine` 입력 후 조회
   - 기대결과: 소환사 카드, 점수, 최근 경기 목록 노출

2. **큐 파라미터 분기 확인**
   - `solo`, `flex`, `both` 각각 조회
   - 기대결과: `meta.requestedQueue`, `meta.resolvedQueues`가 요청과 일치

3. **매치 상세 분석 API 확인**
   - 최근 경기의 `matchId`, 검색한 유저의 `puuid` 확보
   - 호출:
     ```bash
     curl "http://localhost:8080/api/matches/<matchId>/analysis?puuid=<puuid>&bucketMinutes=5"
     ```
   - 기대결과: `laneComparison`, `metricCards`, `timelineBuckets` 데이터 존재

4. **버킷 분기 확인**
   - `bucketMinutes=3`, `5`, `10` 각각 호출
   - 기대결과: 응답의 시간 버킷 간격이 요청값에 맞게 변경

5. **예외 입력 확인**
   - 존재하지 않는 Riot ID 조회
   - 기대결과: 에러 메시지/상태코드가 프론트/백엔드에서 일관되게 처리

## 9.7 빠른 점검용 curl 예시

```bash
# 1) MMR 분석
curl "http://localhost:8080/api/mmr?name=Hide%20on%20bush%23KR1&queue=both"

# 2) 상세 분석
curl "http://localhost:8080/api/matches/KR_1234567890/analysis?puuid=<puuid>&bucketMinutes=5"
```

---

## 10. 현재 진행 상태

### 완료된 범위
- Riot ID 기반 소환사 검색/조회 API
- 솔로/자랭 최근 경기 수집 및 요약
- 자체 점수 계산 엔진(승패 + 성과 보정)
- REMAKE / INVALID 분리 처리
- 경기 상세 분석 API (팀/라인전/타임라인)
- 프론트 대시보드(검색, 카드, 레이더/라인 차트)
- 캐시 적용으로 반복 조회 응답 최적화

### 추가 예정/확장 과제
1. API 키/프로파일 관리 고도화 (`env` 중심으로 통일)
2. 에러 코드 표준화(401/404/429 등 세분화)
3. 레이트리밋 대응(백오프/재시도 정책)
4. 점수식 버전 관리 및 시즌별 보정치 분리
5. 통계 검증용 테스트 케이스 확장
6. queue `both` UI 동시 비교 화면 강화
7. 배포 환경 분리(dev/stage/prod) 및 관측성(로그/메트릭) 강화

---

## 11. 참고
- 이 문서는 현재 코드 기준으로 API/지표/산식/그래프 항목을 정리한 개발 문서입니다.
- README 외 파일은 변경하지 않았습니다.
