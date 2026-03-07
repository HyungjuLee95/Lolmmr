# LolMMR

리그 오브 레전드 전적/랭크 데이터를 기반으로 MMR 성향을 분석하는 풀스택 프로젝트입니다.

---

## 1) 사용 기술 스택

### Frontend
- **React 19**
- **Vite 7**
- **Recharts** (Radar/Line 차트)
- **Tailwind CSS Browser Runtime** (`@tailwindcss/browser` CDN 스크립트 방식)
- **Axios**

### Backend
- **Java 21**
- **Spring Boot 3.4.x**
- **Spring Web / Spring Data JPA / Spring Cache**
- **Spring Cloud OpenFeign**
- **Caffeine Cache**
- **H2 (in-memory DB)**

### Infra / Tooling
- **Gradle**
- **ESLint**
- Riot Open API 연동 (`X-Riot-Token` 헤더 기반)

---

## 2) 현재까지 진행된 상태

### Frontend 진행 상태
- 기존 단순 화면에서 **대시보드형 UI**로 재구성 완료
- 주요 컴포넌트 구성:
  - `MatchCard`
  - `PlayerRow`
  - `ComparisonBar`
- mock 데이터 기반으로 다음 UI 흐름 구현:
  - 프로필 카드
  - 최근 전적 요약
  - 모스트 챔피언
  - 매치 카드 확장(종합 전적 / 상세 분석 탭)
  - Radar/Line 차트 시각화
- Tailwind 유틸리티 기반 스타일이 동작하도록 `index.html`에 런타임 스크립트 적용

### Backend 진행 상태
- `/api/mmr` 엔드포인트 제공 (`name`, `queue` 파라미터)
- `queue`(solo/flex/both) 기준으로 전적 수집/분석 분기 처리
- Riot API 호출 흐름 정리:
  1. Riot ID로 계정 조회
  2. puuid 조회 및 소환사/리그 정보 조회
  3. match id 목록 조회
  4. match detail 조회 및 요약 변환
- `RiotMatchService` 분리로 매치 수집 책임 분리
- 캐시 적용
  - `summonerInfo`
  - `matchIds`
  - `matchRaw`
- 개발 모드 allowlist 파싱/검증 로직 반영
- DTO/Entity의 명시적 getter/setter 구조 반영

---

## 3) 현재 확인된 이슈/주의사항

1. API 계약(요청/응답 스키마) 문서가 부족하여
   프론트에서 필요한 파라미터/필드 추적 비용이 큼
2. `/api/mmr` 단일 엔드포인트에 응답 책임이 집중되어
   확장 시 유지보수 부담이 증가할 수 있음
3. 개발 모드 allowlist가 활성화되어 있으면
   허용 계정 외 테스트가 제한될 수 있음

---

## 4) 다음 단계(권장 로드맵)

### Step 1. API 계약 고정 (최우선)
- 요청 파라미터 명세 고정
  - `name` 형식
  - `queue` 허용값 및 기본값/오류 정책
- 응답 스키마 명세 고정
  - 성공/실패 공통 포맷
  - `summoner`, `queues`, `matchDetails`, `scoreResult` 타입 정의
- 대표 샘플 JSON 문서화

### Step 2. 백엔드 응답 구조 개선
- `requestedQueue`, `resolvedQueue`, 수집 건수 등 메타 필드 추가
- 에러 구조 표준화 (code/message/data)
- 필요 시 `/api/mmr`를 역할별 endpoint로 분리 검토

### Step 3. 프론트 실데이터 연동
- mock ↔ real API 전환 가능한 API 계층 분리
- 응답 타입 기반 렌더링 안정화
- 로딩/에러/empty 상태 컴포넌트 보강

### Step 4. 테스트/검증 체계 강화
- 백엔드: 서비스 단위 테스트 + 컨트롤러 응답 스키마 테스트
- 프론트: 핵심 컴포넌트 렌더링/상호작용 테스트
- 통합: `queue=solo/flex/both` 시나리오 검증

### Step 5. 운영 준비
- 환경변수/프로파일(dev, prod) 분리
- rate limit/재시도/로그 전략 정리
- API 문서 자동화(Swagger/OpenAPI) 도입 검토

---

## 5) 로컬 실행 요약

### Backend
```bash
./gradlew bootRun
```

필수 환경변수:
- `RIOT_API_KEY`

### Frontend
```bash
cd frontend
npm install
npm run dev
```

---


## 6) 현재 API 요약

### Endpoint
- `GET /api/mmr?name={gameName}%23{tagLine}&queue=solo|flex|both`

### Request Parameters
- `name` (필수): Riot ID (`gameName#tagLine`)
- `queue` (선택): `solo`, `flex`, `both` (미지정/비정상 값은 내부적으로 `solo` 처리)

### Response Shape (요약)
```json
{
  "summoner": {
    "name": "...",
    "summonerLevel": 0,
    "soloRank": { "tier": "...", "rank": "...", "leaguePoints": 0 },
    "flexRank": { "tier": "...", "rank": "...", "leaguePoints": 0 }
  },
  "queues": {
    "solo": {
      "matchDetails": [],
      "lpChange": 0,
      "standardMmr": 0,
      "scoreResult": {}
    },
    "flex": {
      "matchDetails": [],
      "lpChange": 0,
      "standardMmr": 0,
      "scoreResult": {}
    }
  }
}
```

### Error Shape
```json
{
  "error": "조회 실패: ..."
}
```

# LOLMMR

리그 오브 레전드 전적 데이터를 기반으로,  
단순 승패/LP 조회를 넘어서 **자체 점수(MMR 대체 지표)** 와 **경기별 상세 분석**을 제공하는 풀스택 프로젝트입니다.

현재 프로젝트는 다음 2가지를 중심으로 동작합니다.

1. **최근 랭크 전적 요약**
   - 승/패/다시하기(REMAKE) 구분
   - 승률, KDA, 최근 챔피언 통계
   - 자체 점수 및 등급 표시

2. **경기별 상세 분석**
   - 맞라인 상대 비교
   - 지표 카드(킬 관여, CS/분, 골드/분, 딜량/분 등)
   - 시간대별 성장 추이
   - 시스템 코멘트

---

## 1. 프로젝트 목표

기존 전적 사이트의 단순 랭크/LP 표시만으로는  
플레이어의 실제 경기 기여도나 성장 흐름을 충분히 설명하기 어렵다고 판단해 시작한 프로젝트입니다.

이 프로젝트의 목표는 다음과 같습니다.

- **승패만이 아닌 경기 내용 기반 평가**
- **REMAKE(다시하기) 경기의 별도 처리**
- **MMR을 직접 표시할 수 없을 때 대체 가능한 자체 점수 체계 구축**
- **시간대별 성장 추이와 라인전/교전/시야 기여 등 세부 지표 시각화**
- **프론트와 백엔드를 분리한 구조로 점진적 확장 가능하게 설계**

---

## 2. 현재 구현 상태 요약

### 완료된 내용

- Riot ID 기준 검색
- 솔로/자랭/통합 큐 조회
- 경기 목록 수집 및 요약
- `WIN / LOSS / REMAKE / INVALID` 결과 구분
- **REMAKE를 패배로 집계하지 않도록 수정**
- 자체 점수 계산 로직 반영
- 프론트 UI 대시보드화
- 경기 카드 확장 UI
- **상세 분석 API 분리**
- 상세 분석 탭에서 실데이터 API 호출
- REMAKE / INVALID 경기 상세 분석 예외 처리

### 아직 남아 있는 내용

- Match Timeline API 기반 **실제 시간대별 이벤트 분석**
- 오브젝트 기여, 킬 관여 시간축 분석 고도화
- overview 탭의 팀원별 일부 수치를 완전한 실데이터로 교체
- 점수 공식 고도화 및 정규화
- 테스트 자동화
- API 문서화 자동화

---

## 3. 핵심 기능

### 3.1 전적 요약
- 소환사 프로필 아이콘 / 레벨
- 자체 평가 등급
- 종합 점수
- 최근 2게임 승률
- KDA 평점
- 모스트 챔피언

### 3.2 경기 결과 분류
현재 경기 결과는 아래 4종류로 분류됩니다.

- `WIN`
- `LOSS`
- `REMAKE`
- `INVALID`

특히 `REMAKE`는 다음 원칙으로 처리합니다.

- **패배로 집계하지 않음**
- 승률 분모에서 제외
- 자체 점수 계산에서 제외
- 상세 분석에서 성과 카드 / 타임라인 표시 제외

### 3.3 경기 상세 분석
경기 카드의 상세 분석 탭에서는 다음을 표시합니다.

- 분석 기준: 내 플레이
- 플레이 성향 레이더 차트
- 맞라인 상대 비교
- 핵심 지표 카드
- 시간대별 성장 추이
- 시스템 코멘트

---

## 4. 기술 스택

## Frontend
- React 19
- Vite 7
- Axios
- Recharts
- ESLint

## Backend
- Java 21
- Spring Boot 3.4.2
- Spring Web
- Spring Data JPA
- Spring Cache
- Spring Cloud OpenFeign
- Caffeine Cache
- H2 Database

## Tooling / Infra
- Gradle
- Vite Proxy (`/api -> http://localhost:8080`)
- Riot Open API

---

## 5. 프로젝트 구조

```text
LOLMMR/
├── frontend/
│   ├── src/
│   │   ├── components/
│   │   │   ├── MatchCard.jsx
│   │   │   ├── PlayerRow.jsx
│   │   │   ├── ComparisonBar.jsx
│   │   │   └── icons.jsx
│   │   ├── data/
│   │   │   └── mmrMockData.js
│   │   ├── utils/
│   │   │   └── mmrMapper.js
│   │   ├── App.jsx
│   │   └── main.jsx
│   ├── vite.config.js
│   └── package.json
│
├── src/main/java/com/example/mmrtest/
│   ├── config/
│   ├── controller/
│   │   ├── MatchController.java
│   │   └── ...
│   ├── dto/
│   │   ├── MatchSummary.java
│   │   ├── MatchResultType.java
│   │   ├── MatchAnalysisDetail.java
│   │   ├── MatchParticipantOverview.java
│   │   ├── LaneOpponentComparison.java
│   │   ├── MetricCard.java
│   │   ├── TimelineBucket.java
│   │   └── CoachingComment.java
│   ├── entity/
│   ├── repository/
│   ├── service/
│   │   ├── RiotMatchService.java
│   │   ├── MatchAnalysisService.java
│   │   ├── ScoreEngine.java
│   │   └── SummonerService.java
│   └── MmrTestApplication.java
│
├── src/main/resources/
│   └── application.yaml
│
├── build.gradle
├── settings.gradle
└── README.md

6. 아키텍처 개요
데이터 흐름

사용자가 프론트에서 Riot ID 검색

프론트가 /api/mmr 호출

백엔드가 Riot API를 통해 계정 / 소환사 / 리그 / 매치 목록 / 매치 상세 조회

백엔드가 요약 데이터를 가공해 프론트로 반환

프론트가 목록 카드 렌더링

사용자가 특정 경기의 상세 분석 탭 클릭

프론트가 /api/matches/{matchId}/analysis?puuid=... 호출

백엔드가 경기 단위 상세 분석 DTO 생성 후 반환

프론트가 상세 분석 UI 렌더링

7. 현재 내부 API
7.1 전적 요약 조회
Request
GET /api/mmr?name={gameName}%23{tagLine}&queue=solo|flex|both
Query Params

name : Riot ID (gameName#tagLine)

queue : solo, flex, both

Example
GET /api/mmr?name=guny%23kr1&queue=solo
Response 예시 구조
{
  "summoner": {
    "puuid": "....",
    "name": "Guny",
    "summonerLevel": 414,
    "profileIconId": 6001,
    "soloRank": {
      "tier": "DIAMOND",
      "rank": "III",
      "leaguePoints": 2
    },
    "flexRank": {
      "tier": "UNRANKED",
      "rank": "",
      "leaguePoints": 0
    }
  },
  "queues": {
    "solo": {
      "summary": {
        "wins": 8,
        "losses": 8,
        "remakes": 1,
        "invalid": 0,
        "countedGames": 16,
        "totalGames": 17,
        "winRate": 50,
        "kda": "2.10"
      },
      "scoreResult": {
        "currentScore": 980,
        "scoreHistory": [1000, 980, 960]
      },
      "matchDetails": [
        {
          "matchId": "KR_8121454645",
          "resultType": "WIN",
          "championName": "Jinx",
          "kills": 13,
          "deaths": 7,
          "assists": 15,
          "totalCs": 348,
          "goldEarned": 21372
        }
      ],
      "standardMmr": 1750,
      "lpChange": 0
    }
  },
  "meta": {
    "requestedQueue": "solo",
    "resolvedQueue": "solo",
    "counts": {
      "solo": {
        "total": 17,
        "counted": 16,
        "remakes": 1,
        "invalid": 0
      }
    }
  }
}
Error Response
{
  "error": "조회 실패: ..."
}
7.2 경기 상세 분석 조회
Request
GET /api/matches/{matchId}/analysis?puuid={puuid}
Example
GET /api/matches/KR_8121454645/analysis?puuid=tYNbl8L41EMPE_...
Response 예시 구조
{
  "matchId": "KR_8121454645",
  "puuid": "....",
  "resultType": "WIN",
  "summary": {
    "matchId": "KR_8121454645",
    "resultType": "WIN",
    "championName": "Jinx",
    "kills": 13,
    "deaths": 7,
    "assists": 15
  },
  "blueTeamPlayers": [],
  "redTeamPlayers": [],
  "laneComparison": {
    "myChampionName": "Jinx",
    "opponentChampionName": "Xayah",
    "myCs": 348,
    "opponentCs": 301,
    "goldDiff": 2800,
    "damageDiff": 5300
  },
  "metricCards": [
    {
      "key": "kp",
      "label": "킬 관여",
      "value": 63.6,
      "unit": "%",
      "score": 89
    }
  ],
  "timelineBuckets": [
    {
      "minute": 5,
      "totalGold": 3143,
      "totalCs": 51,
      "impactScore": 61.7
    }
  ],
  "coachingComments": [
    {
      "type": "good",
      "title": "승리 반영",
      "text": "이번 경기는 승리로 집계되며 기본 점수 상승 대상입니다."
    }
  ]
}
8. Riot API 사용 내역

현재 프로젝트에서 사용하는 외부 Riot API 범주는 아래와 같습니다.

8.1 Account-V1

Riot ID(gameName#tagLine)를 기준으로 계정 식별용 puuid를 찾습니다.

주요 목적:

Riot ID → PUUID 변환

예시:

/account/v1/accounts/by-riot-id/{gameName}/{tagLine}
8.2 Summoner-V4

puuid 기반 소환사 정보를 조회합니다.

주요 목적:

레벨

프로필 아이콘

내부 summoner 식별값 조회

예시:

/lol/summoner/v4/summoners/by-puuid/{puuid}
8.3 League-V4

랭크 정보를 조회합니다.

주요 목적:

솔로랭크 / 자유랭크 티어

랭크

LP

예시:

/lol/league/v4/entries/by-summoner/{summonerId}
8.4 Match-V5 - Match ID 목록

특정 소환사의 랭크 경기 ID 목록을 조회합니다.

주요 목적:

최근 랭크 경기 목록 수집

예시:

/lol/match/v5/matches/by-puuid/{puuid}/ids
8.5 Match-V5 - Match Detail

각 경기의 상세 정보를 조회합니다.

주요 목적:

K/D/A

챔피언

아이템

팀 구성

골드

CS

시야

딜량

경기 결과

상세 분석용 raw data

예시:

/lol/match/v5/matches/{matchId}
9. 점수 / 집계 로직 핵심 원칙
9.1 REMAKE 처리

REMAKE는 일반 패배로 집계하지 않습니다.

원칙:

resultType = REMAKE

wins/losses 집계 제외

countedGames 제외

scoreResult 영향 제외

상세 분석 지표 카드 / 타임라인 제외

9.2 INVALID 처리

비정상 종료 또는 분석 불가 경기로 분류되면:

resultType = INVALID

일반 경기와 같은 기준으로 비교하지 않음

상세 분석 지표 생략 가능

9.3 현재 상세 지표

현재 상세 분석 지표는 다음을 포함합니다.

킬 관여

CS / 분

골드 / 분

딜량 / 분

팀 내 딜 비중

시야 점수

죽은 시간

9.4 현재 타임라인 상태

현재 타임라인은 최종 경기 결과를 기반으로 만든 임시 버킷 추정치입니다.
Riot Timeline API를 아직 직접 붙이지 않았기 때문에,
분 단위 이벤트/오브젝트/교전 흐름을 완전히 반영한 것은 아닙니다.

10. 프론트 구현 상태
10.1 현재 실데이터 기반인 부분

검색 결과 목록

결과 타입 표시

REMAKE 분리 표시

최근 승률 / KDA / 모스트 챔피언

상세 분석 탭의 경기별 API 호출 결과

10.2 아직 완전 실데이터가 아닌 부분

현재 overview 탭의 일부 팀원 행 / 팀 전투 수치 등은
프론트 매퍼에서 생성한 보정/추정값이 일부 섞여 있습니다.

즉, 현재 상태는 다음과 같습니다.

analysis 탭: 백엔드 상세 분석 API 기반

overview 탭: 일부는 요약 데이터, 일부는 프론트 생성값

이 부분은 이후 단계에서 전부 실데이터로 교체할 예정입니다.

11. 캐시 전략

백엔드에서는 Riot API 호출 비용과 응답 지연을 줄이기 위해 캐시를 사용합니다.

현재 주요 캐시 대상:

matchIds

matchRaw

기타 소환사/검색 결과 관련 캐시

사용 라이브러리:

Spring Cache

Caffeine

12. 로컬 실행 방법
12.1 사전 준비

JDK 21

Node.js / npm

Riot API Key

12.2 환경 변수
Windows PowerShell
$env:RIOT_API_KEY="여기에_라이엇_API_KEY"
macOS / Linux / Git Bash
export RIOT_API_KEY="여기에_라이엇_API_KEY"
12.3 Backend 실행
./gradlew bootRun

Windows:

.\gradlew.bat bootRun

기본 포트:

http://localhost:8080
12.4 Frontend 실행
cd frontend
npm install
npm run dev

기본 포트:

http://localhost:5173

Vite proxy 설정으로 /api 요청은 자동으로 localhost:8080으로 전달됩니다.

12.5 빌드 확인
Backend
./gradlew build
Frontend
cd frontend
npm run build
13. 설정 파일

현재 백엔드 주요 설정 파일은 아래입니다.

src/main/resources/application.yaml

주요 설정:

H2 in-memory DB

JPA auto ddl

H2 console

Riot API key

개발용 allowlist

UTF-8 응답 인코딩

14. H2 콘솔

H2 콘솔이 활성화되어 있습니다.

경로:

http://localhost:8080/h2-console

기본 JDBC URL:

jdbc:h2:mem:testdb;MODE=PostgreSQL
15. 개발 중 테스트 팁
15.1 전적 요약 API 테스트
Invoke-RestMethod -Uri "http://localhost:8080/api/mmr?name=guny%23kr1&queue=solo"
15.2 상세 분석 API 테스트
$matchId = "KR_8121454645"
$puuid = "..."
Invoke-RestMethod -Uri "http://localhost:8080/api/matches/$matchId/analysis?puuid=$puuid"
15.3 확인해야 할 핵심 시나리오

일반 WIN 경기

일반 LOSS 경기

REMAKE 경기

잘못된 Riot ID

큐 solo / flex / both

16. 현재 알려진 한계

Timeline API 미연동

시간대별 성장 추이는 아직 임시 추정치입니다.

overview 탭 일부 수치가 프론트 생성값

analysis 탭은 백엔드 API 기반이지만 overview는 일부 보정값이 섞여 있습니다.

Riot API Rate Limit

테스트 중 429 Too Many Requests가 발생할 수 있습니다.

인코딩/콘솔 출력 차이

PowerShell / 터미널에서 한글이 깨져 보일 수 있으나 브라우저 렌더링은 정상일 수 있습니다.

점수 공식은 아직 고도화 중

현재 자체 점수는 초기 버전이며, 오브젝트 기여/타임라인/교전 지표를 더 반영할 계획입니다.

17. 앞으로의 개선 방향
17.1 백엔드

Match Timeline API 연동

오브젝트 기여 분석 추가

킬 관여 시간축 분석

점수 공식 정규화

DTO 및 에러 응답 표준화

테스트 코드 보강

17.2 프론트

overview 탭 실데이터 전환

상세 분석 skeleton UI 개선

차트/지표 필터 추가

queue 전환 UI

더 정교한 empty/error UI

17.3 운영

프로파일 분리(dev/prod)

로그 전략 정비

Swagger/OpenAPI 문서화

캐시 TTL 세분화

18. 권장 Git 관리 팁

테스트 중 생성된 임시 JSON 파일은 커밋하지 않는 것을 권장합니다.

예:

solo.json

flex.json

both.json

phase1-solo.json

필요하면 .gitignore에 추가해 관리하세요.

19. 요약

이 프로젝트는 현재 다음 단계까지 와 있습니다.

전적 조회 가능

REMAKE 구분 및 집계 반영 완료

자체 점수 계산 가능

경기별 상세 분석 API 분리 완료

프론트 상세 분석 탭 연동 진행

즉 지금은
“전적 기반 자체 점수 + 경기별 분석”의 1차 동작 구조는 완성되었고,
앞으로는 실제 타임라인/오브젝트/기여도 분석을 붙여 정교화하는 단계라고 보면 됩니다.