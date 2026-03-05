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

