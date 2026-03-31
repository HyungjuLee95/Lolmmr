import React, { useCallback, useEffect, useMemo, useState } from 'react';
import axios from 'axios';
import MatchCard from './components/MatchCard';
import { Flame, Search, Trophy } from './components/icons';
import { MOCK_DATA } from './data/mmrMockData';
import { mapApiToUiData } from './utils/mmrMapper';

const PROFILE_ICON_VERSION = '16.6.1';
const INITIAL_MATCH_RENDER_COUNT = 10;
const LOAD_MORE_STEP = 10;
const DEFAULT_QUEUE = 'solo';
const QUEUE_OPTIONS = [
  { key: 'solo', label: '솔로랭크' },
  { key: 'flex', label: '자유랭크' },
];

const safeNumber = (value, fallback = 0) => {
  const n = Number(value);
  return Number.isFinite(n) ? n : fallback;
};

const safeString = (value, fallback = '') => {
  if (value === null || value === undefined) return fallback;
  const text = String(value);
  return text.trim() ? text : fallback;
};

const formatSignedNumber = (value, digits = 1) => {
  const n = Number(value);
  if (!Number.isFinite(n)) return '0.0';
  if (n > 0) return `+${n.toFixed(digits)}`;
  return n.toFixed(digits);
};

const getGradeColor = (grade = '') => {
  if (grade.startsWith('S')) {
    return 'text-slate-100 drop-shadow-[0_0_8px_rgba(241,218,196,0.45)]';
  }
  if (grade.startsWith('A')) return 'text-slate-400';
  if (grade.startsWith('B')) return 'text-[#C8BAD0]';
  if (grade.startsWith('C')) return 'text-slate-400';
  return 'text-[#8B86A3]';
};

const getDeltaColor = (value) => {
  const n = Number(value);
  if (!Number.isFinite(n)) return 'text-[#8B86A3]';
  if (n > 0) return 'text-emerald-400';
  if (n < 0) return 'text-rose-400';
  return 'text-[#8B86A3]';
};

const normalizeQueue = (queue) => {
  return queue === 'flex' ? 'flex' : DEFAULT_QUEUE;
};

const readInitialRouteState = () => {
  if (typeof window === 'undefined') {
    return { name: '', queue: DEFAULT_QUEUE };
  }

  const params = new URLSearchParams(window.location.search);
  return {
    name: safeString(params.get('name'), ''),
    queue: normalizeQueue(params.get('queue')),
  };
};

const updateBrowserUrl = (name, queue) => {
  if (typeof window === 'undefined') return;

  const trimmedName = safeString(name, '');
  const normalizedQueue = normalizeQueue(queue);
  const nextUrl = trimmedName
    ? `${window.location.pathname}?name=${encodeURIComponent(trimmedName)}&queue=${normalizedQueue}`
    : window.location.pathname;

  window.history.replaceState({}, '', nextUrl);
};

const formatRankInfo = (rankInfo) => {
  if (!rankInfo?.tier) {
    return {
      tierText: 'UNRANKED',
      lpText: '',
    };
  }

  const tier = String(rankInfo.tier).toUpperCase();
  const rank = safeString(rankInfo.rank, '');
  const lp = safeNumber(rankInfo.leaguePoints, 0);

  const prettyTierMap = {
    IRON: 'Iron',
    BRONZE: 'Bronze',
    SILVER: 'Silver',
    GOLD: 'Gold',
    PLATINUM: 'Platinum',
    EMERALD: 'Emerald',
    DIAMOND: 'Diamond',
    MASTER: 'Master',
    GRANDMASTER: 'Grandmaster',
    CHALLENGER: 'Challenger',
  };

  return {
    tierText: `${prettyTierMap[tier] || tier}${rank ? ` ${rank}` : ''}`,
    lpText: `${lp} LP`,
  };
};

const buildVisibleRecordSummary = (matches = []) => {
  const countedMatches = matches.filter((match) => match?.isCountedGame);
  const wins = countedMatches.filter((match) => match?.win).length;
  const losses = countedMatches.length - wins;
  const remakes = matches.filter((match) => match?.isRemake).length;
  const invalid = matches.filter((match) => match?.isInvalid).length;

  const totalKills = countedMatches.reduce(
    (sum, match) => sum + safeNumber(match?.summary?.kills, 0),
    0
  );
  const totalDeaths = countedMatches.reduce(
    (sum, match) => sum + safeNumber(match?.summary?.deaths, 0),
    0
  );
  const totalAssists = countedMatches.reduce(
    (sum, match) => sum + safeNumber(match?.summary?.assists, 0),
    0
  );

  const winRate =
    countedMatches.length > 0 ? Math.round((wins * 100) / countedMatches.length) : 0;

  const kdaValue =
    countedMatches.length === 0
      ? 0
      : totalDeaths === 0
        ? totalKills + totalAssists
        : (totalKills + totalAssists) / totalDeaths;

  return {
    wins,
    losses,
    remakes,
    invalid,
    countedGames: countedMatches.length,
    totalGames: matches.length,
    winRate,
    kda: kdaValue.toFixed(2),
  };
};

export default function App() {
  const initialRouteState = useMemo(() => readInitialRouteState(), []);
  const [searchInput, setSearchInput] = useState(initialRouteState.name);
  const [lastSearchedName, setLastSearchedName] = useState(initialRouteState.name);
  const [selectedQueue, setSelectedQueue] = useState(initialRouteState.queue);
  const [rawApiData, setRawApiData] = useState(null);
  const [data, setData] = useState(MOCK_DATA);
  const [expandedMatchId, setExpandedMatchId] = useState(null);
  const [isLoading, setIsLoading] = useState(false);
  const [errorMessage, setErrorMessage] = useState('');
  const [hasSearched, setHasSearched] = useState(Boolean(initialRouteState.name));
  const [visibleMatchCount, setVisibleMatchCount] = useState(INITIAL_MATCH_RENDER_COUNT);

  const applyQueueData = useCallback((apiPayload, queueKey) => {
    const normalizedQueue = normalizeQueue(queueKey);
    const mapped = mapApiToUiData(apiPayload, normalizedQueue);

    setSelectedQueue(mapped?.activeQueue || normalizedQueue);
    setData(mapped);
    setExpandedMatchId(null);
    setVisibleMatchCount(INITIAL_MATCH_RENDER_COUNT);
  }, []);

  const fetchMmrData = useCallback(
    async (queryName, queueKey = DEFAULT_QUEUE, forceRefresh = false) => {
      const trimmedName = safeString(queryName, '');
      const normalizedQueue = normalizeQueue(queueKey);

      if (!trimmedName) {
        setErrorMessage('소환사명을 입력해주세요.');
        return false;
      }

      try {
        setIsLoading(true);
        setErrorMessage('');

        const response = await axios.get('/api/mmr', {
          params: {
            name: trimmedName,
            queue: normalizedQueue,
            forceRefresh: forceRefresh,
          },
        });

        const contentType = response.headers?.['content-type'] || '';
        if (!contentType.includes('application/json') || typeof response.data !== 'object') {
          throw new Error('API 응답 형식이 올바르지 않습니다. (프록시/백엔드 연결 확인)');
        }

        if (response.data?.error) {
          throw new Error(response.data.error);
        }

        setRawApiData(response.data);
        applyQueueData(response.data, normalizedQueue);
        setSearchInput(trimmedName);
        setLastSearchedName(trimmedName);
        setHasSearched(true);
        updateBrowserUrl(trimmedName, normalizedQueue);
        return true;
      } catch (error) {
        console.error(error);
        setErrorMessage(error?.message || 'API 호출 중 오류가 발생했습니다.');
        return false;
      } finally {
        setIsLoading(false);
      }
    },
    [applyQueueData]
  );

  useEffect(() => {
    if (!initialRouteState.name) return;
    fetchMmrData(initialRouteState.name, initialRouteState.queue);
  }, [fetchMmrData, initialRouteState]);

  const handleSearchSubmit = async (event) => {
    event.preventDefault();
    const trimmed = searchInput.trim();

    if (!trimmed) {
      setErrorMessage('소환사명을 입력해주세요.');
      return;
    }

    await fetchMmrData(trimmed, selectedQueue);
  };

  const handleQueueChange = (queueKey) => {
    const normalizedQueue = normalizeQueue(queueKey);
    setSelectedQueue(normalizedQueue);

    if (rawApiData) {
      applyQueueData(rawApiData, normalizedQueue);
      updateBrowserUrl(lastSearchedName || searchInput, normalizedQueue);
      return;
    }

    updateBrowserUrl(lastSearchedName || searchInput, normalizedQueue);
  };

  const handleResetSearch = () => {
    setHasSearched(false);
    setErrorMessage('');
    setExpandedMatchId(null);
    setVisibleMatchCount(INITIAL_MATCH_RENDER_COUNT);
    setRawApiData(null);
    setData(MOCK_DATA);
    setLastSearchedName('');
    setSearchInput('');
    setSelectedQueue(DEFAULT_QUEUE);
    updateBrowserUrl('', DEFAULT_QUEUE);
  };

  const profileIconId = safeNumber(data?.summoner?.profileIconId, 29);
  const summonerLevel = safeNumber(data?.summoner?.summonerLevel, 0);
  const summonerName = safeString(data?.summoner?.name, 'Unknown');
  const scoreDetails = data?.summoner?.scoreDetails || {};
  const averageDelta = safeNumber(scoreDetails?.averageDelta, 0);
  const averagePerfIndex = safeNumber(scoreDetails?.averagePerfIndex, 0);

  const activeRankInfo =
    data?.summoner?.currentRank ||
    (selectedQueue === 'flex' ? data?.summoner?.flexRank : data?.summoner?.soloRank);

  const currentRank = formatRankInfo(activeRankInfo);
  const tierName = activeRankInfo?.tier ? String(activeRankInfo.tier).toLowerCase() : null;
  const tierImageUrl = tierName && tierName !== 'unranked' ? `https://opgg-static.akamaized.net/images/medals_new/${tierName}.png` : null;

  const allMatches = useMemo(
    () => (Array.isArray(data?.matches) ? data.matches : []),
    [data]
  );

  const visibleMatches = useMemo(
    () => allMatches.slice(0, visibleMatchCount),
    [allMatches, visibleMatchCount]
  );

  const hasMoreMatches = visibleMatchCount < allMatches.length;
  const visibleSummary = useMemo(
    () => buildVisibleRecordSummary(visibleMatches),
    [visibleMatches]
  );

  const sampleSummary = data?.summary || {};
  const remakes = safeNumber(sampleSummary?.remakes, 0);
  const invalid = safeNumber(sampleSummary?.invalid, 0);
  const scoreSampleCount = safeNumber(
    sampleSummary?.scoreSampleCount,
    data?.meta?.scoreSampleCount ?? scoreDetails?.sampleCount ?? allMatches.length
  );

  const recentChampionRows = useMemo(
    () => (Array.isArray(data?.summary?.recentChampions) ? data.summary.recentChampions : []),
    [data]
  );

  const availableQueues = Array.isArray(data?.availableQueues)
    ? data.availableQueues
    : Array.isArray(data?.meta?.availableQueues)
      ? data.meta.availableQueues
      : QUEUE_OPTIONS.map((option) => option.key);

  const activeQueueLabel =
    data?.activeQueueLabel ||
    data?.meta?.activeQueueLabel ||
    QUEUE_OPTIONS.find((option) => option.key === selectedQueue)?.label ||
    '솔로랭크';

  if (!hasSearched) {
    return (
      <div className="min-h-screen bg-slate-900 text-slate-100 flex items-center justify-center px-4">
        <div className="w-full max-w-2xl bg-slate-800 border border-slate-700 rounded-2xl p-8 md:p-10 shadow-2xl">
          <div className="flex items-center justify-center gap-3 text-3xl font-black text-slate-100 mb-6">
            <Trophy className="w-9 h-9" />
            <span>LOLMMR</span>
          </div>

          <p className="text-center text-slate-400 text-sm md:text-base mb-6">
            소환사명을 검색하면 최근 전적과 최근 {INITIAL_MATCH_RENDER_COUNT}경기 표시,
            최근 {scoreSampleCount || 20}경기 점수 기준을 함께 보여주는 MMR 분석 화면으로 이동합니다.
          </p>

          <form className="relative" onSubmit={handleSearchSubmit}>
            <input
              type="text"
              value={searchInput}
              onChange={(event) => setSearchInput(event.target.value)}
              placeholder="예: 욕설멈춰i#KR1"
              className="w-full bg-slate-700 text-slate-100 px-4 py-3 pl-11 rounded-lg border border-slate-500/40 focus:outline-none focus:border-slate-300"
            />
            <Search className="w-4 h-4 text-slate-400 absolute left-4 top-3.5" />
            <button
              type="submit"
              disabled={isLoading}
              className="mt-3 w-full bg-slate-700 hover:bg-[#5C5D8A] disabled:bg-slate-700/60 text-slate-100 px-4 py-2.5 rounded-lg font-semibold transition-colors"
            >
              {isLoading ? '검색 중...' : '검색하기'}
            </button>
          </form>

          {errorMessage && (
            <div className="text-xs md:text-sm text-slate-100 mt-4 text-center">{errorMessage}</div>
          )}
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-slate-900 text-slate-100 font-sans pb-20">
      <header className="bg-slate-800 border-b border-slate-700 sticky top-0 z-50">
        <div className="max-w-6xl mx-auto px-4 py-4 flex flex-col md:flex-row items-center justify-between gap-4">
          <div className="flex items-center gap-2 text-2xl font-bold text-slate-100">
            <Trophy className="w-8 h-8" />
            <span onClick={handleResetSearch} className="cursor-pointer">
              LOLMMR
            </span>
          </div>

          <form className="w-full md:w-[430px] relative" onSubmit={handleSearchSubmit}>
            <input
              type="text"
              value={searchInput}
              onChange={(event) => setSearchInput(event.target.value)}
              placeholder="소환사명#태그"
              className="w-full bg-slate-700 text-slate-100 px-4 py-2.5 pl-10 rounded-lg border border-slate-500/40 focus:outline-none focus:border-slate-300 text-sm"
            />
            <Search className="w-4 h-4 text-slate-400 absolute left-3 top-3.5" />
            <button
              type="submit"
              disabled={isLoading}
              className="absolute right-2 top-1.5 bg-slate-700 hover:bg-[#5C5D8A] disabled:bg-slate-700/60 text-slate-100 px-3 py-1 rounded text-xs font-medium"
            >
              {isLoading ? '검색중' : '검색'}
            </button>
          </form>

          <button
            type="button"
            onClick={handleResetSearch}
            className="text-xs text-slate-400 border border-slate-700 rounded px-3 py-2 hover:bg-slate-700"
          >
            새 검색
          </button>
        </div>

        {(isLoading || errorMessage) && (
          <div className="max-w-6xl mx-auto px-4 pb-4">
            {isLoading && (
              <div className="text-xs text-slate-400">데이터를 불러오는 중...</div>
            )}
            {errorMessage && (
              <div className="text-xs text-slate-100 mt-1">{errorMessage}</div>
            )}
          </div>
        )}
      </header>

      <main className="max-w-6xl mx-auto px-4 py-8">
        {data?.activeGame && (
          <div className="bg-slate-700/40 border border-slate-300/50 rounded-xl p-3 mb-6 flex items-center justify-between shadow-[0_0_15px_rgba(241,218,196,0.1)] relative overflow-hidden animate-[pulse_3s_infinite]">
            <div className="absolute inset-0 bg-gradient-to-r from-transparent via-slate-300/5 to-transparent animate-[shimmer_2s_infinite]" />
            <div className="flex items-center gap-3 relative z-10">
              <div className="relative flex min-w-[12px] min-h-[12px]">
                <div className="absolute inline-flex w-full h-full rounded-full bg-green-400 opacity-75 animate-ping"></div>
                <div className="relative inline-flex w-3 h-3 rounded-full bg-green-500"></div>
              </div>
              <span className="text-sm font-bold text-slate-100">
                🟢 현재 게임 플레이 중 ({data.activeGame.gameMode})
              </span>
            </div>
            {data.activeGame.gameLength > 0 && (
              <span className="text-xs font-semibold text-slate-400 relative z-10 border border-slate-500/40 px-2 py-1 rounded bg-slate-900/50">
                {Math.floor(data.activeGame.gameLength / 60)}분 {(data.activeGame.gameLength % 60).toString().padStart(2, '0')}초 진행중
              </span>
            )}
          </div>
        )}

        <div className="flex flex-col lg:flex-row gap-6">
          <div className="w-full lg:w-80 flex flex-col gap-4 flex-shrink-0">
            <div className="bg-slate-800 rounded-xl p-5 border border-slate-700 relative overflow-hidden">
              <div className="absolute top-0 left-0 w-full h-2 bg-gradient-to-r from-slate-500 to-slate-700" />

              <div className="flex items-start gap-4">
                <div className="relative">
                  <img
                    src={`https://ddragon.leagueoflegends.com/cdn/${PROFILE_ICON_VERSION}/img/profileicon/${profileIconId}.png`}
                    alt="Profile"
                    className="w-20 h-20 rounded-xl border border-slate-700 object-cover"
                  />
                  <div className="absolute -bottom-2 left-1/2 -translate-x-1/2 bg-slate-900 border border-slate-700 text-[10px] px-2 py-0.5 rounded-full text-slate-400 whitespace-nowrap">
                    LV {summonerLevel}
                  </div>
                </div>

                <div className="flex-1 min-w-0">
                  <div className="flex items-center justify-between mb-2">
                    <h1 className="text-xl font-bold text-slate-100 truncate">
                      {summonerName}
                    </h1>
                    <button
                      type="button"
                      disabled={isLoading}
                      onClick={() => fetchMmrData(lastSearchedName, selectedQueue, true)}
                      className="bg-[#A69CAC] hover:bg-[#F1DAC4] text-slate-900 text-xs font-bold px-3 py-1.5 rounded-lg transition-colors flex items-center gap-1 shrink-0 disabled:opacity-50"
                    >
                      {isLoading ? '갱신 중...' : '전적 갱신'}
                    </button>
                  </div>

                  <div className="flex flex-wrap gap-2 mb-3">
                    {QUEUE_OPTIONS.map((option) => {
                      const isActive = selectedQueue === option.key;
                      const isAvailable = availableQueues.includes(option.key);

                      return (
                        <button
                          key={option.key}
                          type="button"
                          onClick={() => handleQueueChange(option.key)}
                          className={`px-3 py-1.5 rounded-lg text-xs border transition-colors ${
                            isActive
                              ? 'bg-[#A69CAC] text-slate-900 border-slate-500 font-semibold'
                              : 'bg-[#1A2040] text-slate-400 border-slate-700 hover:bg-slate-700'
                          } ${!isAvailable ? 'opacity-60' : ''}`}
                        >
                          {option.label}
                        </button>
                      );
                    })}
                  </div>

                  <div className="flex flex-wrap gap-2">
                    <div className="bg-slate-700 px-2 py-2 rounded flex flex-col items-center justify-center border border-slate-500/40 min-w-[82px]">
                      <span className="text-[10px] text-slate-400">현티어</span>
                      {tierImageUrl && (
                        <img src={tierImageUrl} alt={tierName} className="w-10 h-10 my-1 drop-shadow-[0_2px_4px_rgba(0,0,0,0.5)]" />
                      )}
                      <span className="text-sm font-bold text-slate-100 mb-0.5">{currentRank.tierText}</span>
                      <span className="text-[10px] text-slate-400">{currentRank.lpText}</span>
                    </div>

                    <div className="bg-slate-700 px-2 py-2 rounded flex flex-col items-center justify-center border border-slate-500/40 min-w-[78px]">
                      <span className="text-[10px] text-slate-400">기준점수</span>
                      <span className="text-sm font-bold text-slate-100">
                        {safeNumber(scoreDetails?.baseScore, 0)}
                      </span>
                    </div>

                    <div className="bg-slate-700 px-2 py-1 rounded flex flex-col items-center border border-slate-500/40 min-w-[78px]">
                      <span className="text-[10px] text-slate-400">종합점수</span>
                      <span className="text-sm font-bold text-slate-100">
                        {safeString(scoreDetails?.totalScore, '0.0')}
                      </span>
                    </div>

                    <div className="bg-slate-700 px-2 py-2 rounded flex flex-col items-center justify-center border border-slate-500/40 min-w-[82px]">
                      <span className="text-[10px] text-slate-400 mb-1">종합등급</span>
                      <span
                        className={`text-sm font-black ${getGradeColor(
                          safeString(scoreDetails?.grade, 'C')
                        )}`}
                      >
                        {safeString(scoreDetails?.grade, 'C')}
                      </span>
                    </div>
                  </div>
                </div>
              </div>
            </div>

            <div className="bg-slate-800 rounded-xl p-5 border border-slate-700 flex items-center justify-between">
              <div className="flex flex-col items-center">
                <span className="text-xs text-slate-400 mb-2">
                  {activeQueueLabel} {visibleMatches.length}경기 표시
                </span>
                <div className="relative w-20 h-20 flex items-center justify-center">
                  <svg className="w-full h-full transform -rotate-90">
                    <circle cx="40" cy="40" r="34" fill="transparent" stroke="#474973" strokeWidth="8" />
                    <circle
                      cx="40"
                      cy="40"
                      r="34"
                      fill="transparent"
                      stroke="#A69CAC"
                      strokeWidth="8"
                      strokeDasharray={`${(visibleSummary.winRate / 100) * 213} 213`}
                    />
                  </svg>

                  <div className="absolute flex flex-col items-center">
                    <span className="text-sm font-bold text-slate-100">
                      {visibleSummary.winRate}%
                    </span>
                    <span className="text-[10px] text-slate-400">
                      {visibleSummary.wins}승 {visibleSummary.losses}패
                    </span>
                    {visibleSummary.remakes > 0 && (
                      <span className="text-[10px] text-slate-100 mt-0.5">
                        다시하기 {visibleSummary.remakes}회
                      </span>
                    )}
                  </div>
                </div>
              </div>

              <div className="h-16 w-px bg-slate-700" />

              <div className="flex flex-col justify-center text-center">
                <span className="text-xs text-slate-400 mb-1">KDA 평점</span>
                <div className="text-xl font-bold text-slate-100">
                  {visibleSummary.kda}
                </div>
                <div className="text-[10px] text-slate-400/80 mt-1">
                  점수는 최근 {scoreSampleCount}경기 기준
                </div>
                {(remakes > 0 || invalid > 0) && (
                  <div className="text-[10px] text-slate-100 mt-1">
                    다시하기/제외 {remakes + invalid}경기
                  </div>
                )}
              </div>
            </div>

            <div className="bg-slate-800 rounded-xl border border-slate-700 p-4">
              <div className="text-xs font-bold text-slate-100 mb-3">최근 점수 흐름</div>

              <div className="grid grid-cols-2 gap-3">
                <div className="bg-slate-700 rounded-lg border border-slate-500/30 p-3">
                  <div className="text-[10px] text-slate-400 mb-1">평균 점수 변동</div>
                  <div className={`text-lg font-bold ${getDeltaColor(averageDelta)}`}>
                    {formatSignedNumber(averageDelta, 1)}
                  </div>
                </div>

                <div className="bg-slate-700 rounded-lg border border-slate-500/30 p-3">
                  <div className="text-[10px] text-slate-400 mb-1">평균 퍼포먼스</div>
                  <div className="text-lg font-bold text-slate-100">
                    {safeNumber(scoreDetails?.averagePerformance, 0).toFixed(1)}
                  </div>
                </div>

                <div className="bg-slate-700 rounded-lg border border-slate-500/30 p-3">
                  <div className="text-[10px] text-slate-400 mb-1">평균 PerfIndex</div>
                  <div className={`text-lg font-bold ${getDeltaColor(averagePerfIndex)}`}>
                    {formatSignedNumber(averagePerfIndex, 2)}
                  </div>
                </div>

                <div className="bg-slate-700 rounded-lg border border-slate-500/30 p-3">
                  <div className="text-[10px] text-slate-400 mb-1">집계 경기 수</div>
                  <div className="text-lg font-bold text-slate-100">
                    {safeNumber(scoreDetails?.countedGames, 0)}
                    <span className="text-xs text-slate-400 ml-1">/ {scoreSampleCount}</span>
                  </div>
                </div>
              </div>
            </div>

            <div className="bg-slate-800 rounded-xl border border-slate-700 overflow-hidden">
              <div className="px-4 py-3 border-b border-slate-700 text-xs font-bold text-slate-100 flex items-center gap-1.5">
                <Flame className="w-3.5 h-3.5 text-slate-400" />
                모스트 챔피언
              </div>

              <div className="p-1.5">
                {recentChampionRows.length ? (
                  recentChampionRows.map((champ, idx) => (
                    <div
                      key={`${champ.name}-${idx}`}
                      className="flex items-center gap-3 p-2.5 hover:bg-slate-700 rounded-lg transition-colors cursor-default"
                    >
                      <img
                        src={`https://ddragon.leagueoflegends.com/cdn/${PROFILE_ICON_VERSION}/img/champion/${champ.name}.png`}
                        alt={champ.name}
                        className="w-8 h-8 rounded-full bg-slate-700"
                      />

                      <div className="flex-1 min-w-0">
                        <div className="text-sm font-semibold text-slate-100 leading-tight truncate">
                          {champ.name}
                        </div>
                        <div className="text-[10px] text-slate-400 mt-0.5">
                          {champ.games} 게임
                        </div>
                      </div>

                      <div className="text-right">
                        <div
                          className={`text-sm font-semibold ${
                            safeNumber(champ.winRate, 0) >= 60 ? 'text-slate-100' : 'text-slate-400'
                          }`}
                        >
                          {safeNumber(champ.winRate, 0)}%
                        </div>
                        <div className="text-[10px] text-slate-400">
                          {safeString(champ.kda, '0.0')} 평점
                        </div>
                      </div>
                    </div>
                  ))
                ) : (
                  <div className="p-4 text-xs text-[#8B86A3] text-center">
                    표시할 챔피언 데이터가 없습니다.
                  </div>
                )}
              </div>
            </div>

            {/* Champion Masteries Top 3 */}
            {data?.championMasteries && data.championMasteries.length > 0 && (
              <div className="bg-slate-800 rounded-xl p-4 border border-slate-700 relative overflow-hidden">
                <h3 className="text-slate-400 text-[11px] font-bold mb-3 pb-2 border-b border-slate-700 uppercase tracking-wider text-center flex items-center justify-center gap-1.5">
                  <span className="text-xl">🏆</span> 상위 챔피언 숙련도 탑3
                </h3>
                <div className="flex flex-col gap-2">
                  {data.championMasteries.map((m, idx) => (
                    <div key={idx} className="flex items-center gap-3 bg-slate-900/60 rounded-lg p-2.5 border border-slate-700/50 hover:bg-slate-700/40 hover:border-slate-300/30 transition-all shadow-sm">
                      <div className="relative isolate group">
                        <img
                          src={`https://ddragon.leagueoflegends.com/cdn/16.6.1/img/champion/${m.championName}.png`}
                          alt={m.championName}
                          className="w-11 h-11 rounded border border-slate-700 group-hover:border-slate-300 transition-colors"
                        />
                        <div className="absolute -bottom-1 -right-1 bg-gradient-to-r from-slate-800 to-slate-900 border border-slate-300/80 shadow-md text-[9px] w-[18px] h-[18px] rounded-full flex items-center justify-center font-black text-slate-100 z-10">
                          {m.championLevel}
                        </div>
                      </div>
                      <div className="flex flex-col min-w-0">
                        <span className="text-slate-100 text-[13px] font-bold truncate leading-tight">{m.championName}</span>
                        <span className="text-slate-400 text-[10px] font-medium">{m.championPoints?.toLocaleString() || 0} pt</span>
                      </div>
                    </div>
                  ))}
                </div>
              </div>
            )}
          </div>

          <div className="flex-1 flex flex-col gap-2">
            <div className="bg-slate-800 border border-slate-700 rounded-xl p-3 flex items-center justify-between gap-3">
              <div>
                <div className="text-sm font-semibold text-slate-100">{activeQueueLabel}</div>
                <div className="text-[11px] text-slate-400">
                  새로고침해도 현재 소환사/큐 상태를 유지합니다.
                </div>
              </div>

              <div className="flex gap-2">
                {QUEUE_OPTIONS.map((option) => {
                  const isActive = selectedQueue === option.key;
                  const isAvailable = availableQueues.includes(option.key);

                  return (
                    <button
                      key={option.key}
                      type="button"
                      onClick={() => handleQueueChange(option.key)}
                      className={`px-3 py-2 rounded-lg text-xs border transition-colors ${
                        isActive
                          ? 'bg-[#A69CAC] text-slate-900 border-slate-500 font-semibold'
                          : 'bg-[#1A2040] text-slate-400 border-slate-700 hover:bg-slate-700'
                      } ${!isAvailable ? 'opacity-60' : ''}`}
                    >
                      {option.label}
                    </button>
                  );
                })}
              </div>
            </div>

            {visibleMatches.map((match) => (
              <MatchCard
                key={`${selectedQueue}-${match.id}-${expandedMatchId === match.id ? 'open' : 'closed'}`}
                match={match}
                isExpanded={expandedMatchId === match.id}
                onToggle={() =>
                  setExpandedMatchId((prev) => (prev === match.id ? null : match.id))
                }
              />
            ))}

            {hasMoreMatches && (
              <div className="flex justify-center pt-3">
                <button
                  type="button"
                  onClick={() =>
                    setVisibleMatchCount((prev) =>
                      Math.min(prev + LOAD_MORE_STEP, allMatches.length)
                    )
                  }
                  className="px-4 py-2 rounded-lg border border-slate-700 bg-slate-800 text-sm text-slate-100 hover:bg-slate-700 transition-colors"
                >
                  더보기 ({visibleMatches.length}/{allMatches.length})
                </button>
              </div>
            )}

            {!visibleMatches.length && (
              <div className="w-full py-10 bg-slate-800 border border-slate-700 rounded-lg text-sm text-slate-400 text-center">
                {activeQueueLabel} 최근 전적이 없습니다.
              </div>
            )}
          </div>
        </div>
      </main>
    </div>
  );
}