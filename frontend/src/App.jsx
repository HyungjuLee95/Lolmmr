import React, { useCallback, useMemo, useState } from 'react';
import axios from 'axios';
import MatchCard from './components/MatchCard';
import { Flame, Search, Trophy } from './components/icons';
import { MOCK_DATA } from './data/mmrMockData';
import { mapApiToUiData } from './utils/mmrMapper';

const PROFILE_ICON_VERSION = '14.3.1';

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
    return 'text-[#F1DAC4] drop-shadow-[0_0_8px_rgba(241,218,196,0.45)]';
  }
  if (grade.startsWith('A')) return 'text-[#A69CAC]';
  if (grade.startsWith('B')) return 'text-[#C8BAD0]';
  if (grade.startsWith('C')) return 'text-[#A69CAC]';
  return 'text-[#8B86A3]';
};

const getDeltaColor = (value) => {
  const n = Number(value);
  if (!Number.isFinite(n)) return 'text-[#8B86A3]';
  if (n > 0) return 'text-emerald-400';
  if (n < 0) return 'text-rose-400';
  return 'text-[#8B86A3]';
};

const getTierBadgeClass = (tier = '') => {
  const normalized = String(tier).toUpperCase();

  if (normalized === 'DIAMOND') {
    return 'bg-cyan-500/10 text-cyan-300 border-cyan-500/30';
  }
  if (normalized === 'EMERALD') {
    return 'bg-emerald-500/10 text-emerald-300 border-emerald-500/30';
  }
  if (normalized === 'PLATINUM') {
    return 'bg-sky-500/10 text-sky-300 border-sky-500/30';
  }
  if (normalized === 'GOLD') {
    return 'bg-amber-500/10 text-amber-300 border-amber-500/30';
  }
  if (normalized === 'SILVER') {
    return 'bg-slate-400/10 text-slate-300 border-slate-500/30';
  }
  return 'bg-orange-500/10 text-orange-300 border-orange-500/30';
};

export default function App() {
  const [searchInput, setSearchInput] = useState('');
  const [data, setData] = useState(MOCK_DATA);
  const [expandedMatchId, setExpandedMatchId] = useState(null);
  const [isLoading, setIsLoading] = useState(false);
  const [errorMessage, setErrorMessage] = useState('');
  const [hasSearched, setHasSearched] = useState(false);

  const fetchMmrData = useCallback(async (queryName) => {
    try {
      setIsLoading(true);
      setErrorMessage('');

      const response = await axios.get('/api/mmr', {
        params: {
          name: queryName,
          queue: 'solo',
        },
      });

      const contentType = response.headers?.['content-type'] || '';
      if (!contentType.includes('application/json') || typeof response.data !== 'object') {
        throw new Error('API 응답 형식이 올바르지 않습니다. (프록시/백엔드 연결 확인)');
      }

      if (response.data?.error) {
        throw new Error(response.data.error);
      }

      setData(mapApiToUiData(response.data, 'solo'));
      setExpandedMatchId(null);
      setHasSearched(true);
      return true;
    } catch (error) {
      console.error(error);
      setErrorMessage(error?.message || 'API 호출 중 오류가 발생했습니다.');
      return false;
    } finally {
      setIsLoading(false);
    }
  }, []);

  const handleSearchSubmit = async (event) => {
    event.preventDefault();
    const trimmed = searchInput.trim();

    if (!trimmed) {
      setErrorMessage('소환사명을 입력해주세요.');
      return;
    }

    await fetchMmrData(trimmed);
  };

  const remakes = safeNumber(data?.summary?.remakes, 0);
  const invalid = safeNumber(data?.summary?.invalid, 0);
  const scoreDetails = data?.summoner?.scoreDetails || {};
  const visibleMatchCount = safeNumber(data?.summary?.displayMatchCount, data?.matches?.length || 0);
  const scoreSampleCount = safeNumber(
    data?.summary?.scoreSampleCount,
    scoreDetails?.sampleCount || 0
  );

  const profileIconId = safeNumber(data?.summoner?.profileIconId, 29);
  const summonerLevel = safeNumber(data?.summoner?.summonerLevel, 0);
  const summonerName = safeString(data?.summoner?.name, 'Unknown');
  const scoreTier = safeString(scoreDetails?.scoreTier, '');
  const averageDelta = safeNumber(scoreDetails?.averageDelta, 0);
  const averagePerfIndex = safeNumber(scoreDetails?.averagePerfIndex, 0);

  const recentChampionRows = useMemo(
    () => (Array.isArray(data?.summary?.recentChampions) ? data.summary.recentChampions : []),
    [data]
  );

  if (!hasSearched) {
    return (
      <div className="min-h-screen bg-[#0D0C1D] text-[#F1DAC4] flex items-center justify-center px-4">
        <div className="w-full max-w-2xl bg-[#161B33] border border-[#474973] rounded-2xl p-8 md:p-10 shadow-2xl">
          <div className="flex items-center justify-center gap-3 text-3xl font-black text-[#F1DAC4] mb-6">
            <Trophy className="w-9 h-9" />
            <span>LOLMMR</span>
          </div>

          <p className="text-center text-[#A69CAC] text-sm md:text-base mb-6">
            소환사명을 검색하면 최근 2경기 표시와 최근 10경기 점수 산정 기준을 함께 보여주는 MMR 분석 대시보드로 이동합니다.
          </p>

          <form className="relative" onSubmit={handleSearchSubmit}>
            <input
              type="text"
              value={searchInput}
              onChange={(event) => setSearchInput(event.target.value)}
              placeholder="예: 욕설멈춰i#KR1"
              className="w-full bg-[#474973] text-[#F1DAC4] px-4 py-3 pl-11 rounded-lg border border-[#A69CAC]/40 focus:outline-none focus:border-[#F1DAC4]"
            />
            <Search className="w-4 h-4 text-[#A69CAC] absolute left-4 top-3.5" />
            <button
              type="submit"
              disabled={isLoading}
              className="mt-3 w-full bg-[#474973] hover:bg-[#5C5D8A] disabled:bg-[#474973]/60 text-[#F1DAC4] px-4 py-2.5 rounded-lg font-semibold transition-colors"
            >
              {isLoading ? '검색 중...' : '검색하기'}
            </button>
          </form>

          {errorMessage && (
            <div className="text-xs md:text-sm text-[#F1DAC4] mt-4 text-center">{errorMessage}</div>
          )}
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-[#0D0C1D] text-[#F1DAC4] font-sans pb-20">
      <header className="bg-[#161B33] border-b border-[#474973] sticky top-0 z-50">
        <div className="max-w-6xl mx-auto px-4 py-4 flex flex-col md:flex-row items-center justify-between gap-4">
          <div className="flex items-center gap-2 text-2xl font-bold text-[#F1DAC4]">
            <Trophy className="w-8 h-8" />
            <span>LOLMMR</span>
          </div>

          <form className="w-full md:w-[430px] relative" onSubmit={handleSearchSubmit}>
            <input
              type="text"
              value={searchInput}
              onChange={(event) => setSearchInput(event.target.value)}
              placeholder="소환사명#태그"
              className="w-full bg-[#474973] text-[#F1DAC4] px-4 py-2.5 pl-10 rounded-lg border border-[#A69CAC]/40 focus:outline-none focus:border-[#F1DAC4] text-sm"
            />
            <Search className="w-4 h-4 text-[#A69CAC] absolute left-3 top-3.5" />
            <button
              type="submit"
              disabled={isLoading}
              className="absolute right-2 top-1.5 bg-[#474973] hover:bg-[#5C5D8A] disabled:bg-[#474973]/60 text-[#F1DAC4] px-3 py-1 rounded text-xs font-medium"
            >
              {isLoading ? '검색중' : '검색'}
            </button>
          </form>

          <button
            type="button"
            onClick={() => {
              setHasSearched(false);
              setErrorMessage('');
              setExpandedMatchId(null);
            }}
            className="text-xs text-[#A69CAC] border border-[#474973] rounded px-3 py-2 hover:bg-[#474973]"
          >
            새 검색
          </button>
        </div>

        {(isLoading || errorMessage) && (
          <div className="max-w-6xl mx-auto px-4 pb-4">
            {isLoading && <div className="text-xs text-[#A69CAC]">데이터를 불러오는 중...</div>}
            {errorMessage && <div className="text-xs text-[#F1DAC4] mt-1">{errorMessage}</div>}
          </div>
        )}
      </header>

      <main className="max-w-6xl mx-auto px-4 py-8">
        <div className="flex flex-col lg:flex-row gap-6">
          <div className="w-full lg:w-80 flex flex-col gap-4 flex-shrink-0">
            <div className="bg-[#161B33] rounded-xl p-5 border border-[#474973] relative overflow-hidden">
              <div className="absolute top-0 left-0 w-full h-2 bg-gradient-to-r from-[#A69CAC] to-[#474973]" />

              <div className="flex items-start gap-4">
                <div className="relative">
                  <img
                    src={`https://ddragon.leagueoflegends.com/cdn/${PROFILE_ICON_VERSION}/img/profileicon/${profileIconId}.png`}
                    alt="Profile"
                    className="w-20 h-20 rounded-xl border border-[#474973] object-cover"
                  />
                  <div className="absolute -bottom-2 left-1/2 -translate-x-1/2 bg-[#0D0C1D] border border-[#474973] text-[10px] px-2 py-0.5 rounded-full text-[#A69CAC] whitespace-nowrap">
                    LV {summonerLevel}
                  </div>
                </div>

                <div className="min-w-0 flex-1">
                  <h1 className="text-xl font-bold text-[#F1DAC4] mb-2 truncate">{summonerName}</h1>

                  <div className="flex flex-wrap gap-2">
                    <div className="bg-[#474973] px-2 py-1 rounded flex flex-col items-center border border-[#A69CAC]/40 min-w-[64px]">
                      <span className="text-[10px] text-[#A69CAC]">자체 평가</span>
                      <span className={`text-sm font-black ${getGradeColor(safeString(scoreDetails?.grade, 'C'))}`}>
                        {safeString(scoreDetails?.grade, 'C')}
                      </span>
                    </div>

                    <div className="bg-[#474973] px-2 py-1 rounded flex flex-col items-center border border-[#A69CAC]/40 min-w-[72px]">
                      <span className="text-[10px] text-[#A69CAC]">종합 점수</span>
                      <span className="text-sm font-bold text-[#F1DAC4]">
                        {safeString(scoreDetails?.totalScore, '0.0')}
                      </span>
                    </div>

                    <div
                      className={`px-2 py-1 rounded flex flex-col items-center border min-w-[76px] ${getTierBadgeClass(scoreTier)}`}
                    >
                      <span className="text-[10px] opacity-80">자체 티어</span>
                      <span className="text-xs font-bold">{scoreTier || '-'}</span>
                    </div>
                  </div>
                </div>
              </div>
            </div>

            <div className="bg-[#161B33] rounded-xl p-5 border border-[#474973] flex items-center justify-between">
              <div className="flex flex-col items-center">
                <span className="text-xs text-[#A69CAC] mb-2">최근 {visibleMatchCount}경기 표시</span>
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
                      strokeDasharray={`${(safeNumber(data?.summary?.winRate, 0) / 100) * 213} 213`}
                    />
                  </svg>

                  <div className="absolute flex flex-col items-center">
                    <span className="text-sm font-bold text-[#F1DAC4]">{safeNumber(data?.summary?.winRate, 0)}%</span>
                    <span className="text-[10px] text-[#A69CAC]">
                      {safeNumber(data?.summary?.wins, 0)}승 {safeNumber(data?.summary?.losses, 0)}패
                    </span>
                    {remakes > 0 && (
                      <span className="text-[10px] text-[#F1DAC4] mt-0.5">다시하기 {remakes}회</span>
                    )}
                  </div>
                </div>
              </div>

              <div className="h-16 w-px bg-[#474973]" />

              <div className="flex flex-col justify-center text-center">
                <span className="text-xs text-[#A69CAC] mb-1">KDA 평점</span>
                <div className="text-xl font-bold text-[#F1DAC4]">{safeString(data?.summary?.kda, '0.00')}</div>
                <div className="text-[10px] text-[#8B86A3] mt-1">점수는 최근 {scoreSampleCount}경기 기준</div>
                {(remakes > 0 || invalid > 0) && (
                  <div className="text-[10px] text-[#F1DAC4] mt-1">다시하기/제외 {remakes + invalid}경기</div>
                )}
              </div>
            </div>

            <div className="bg-[#161B33] rounded-xl border border-[#474973] p-4">
              <div className="text-xs font-bold text-[#F1DAC4] mb-3">최근 점수 흐름</div>
              <div className="grid grid-cols-2 gap-3">
                <div className="bg-[#474973] rounded-lg border border-[#A69CAC]/40 p-3">
                  <div className="text-[10px] text-[#A69CAC] mb-1">평균 점수 변동</div>
                  <div className={`text-lg font-bold ${getDeltaColor(averageDelta)}`}>
                    {formatSignedNumber(averageDelta, 1)}
                  </div>
                </div>

                <div className="bg-[#474973] rounded-lg border border-[#A69CAC]/40 p-3">
                  <div className="text-[10px] text-[#A69CAC] mb-1">평균 퍼포먼스</div>
                  <div className="text-lg font-bold text-[#F1DAC4]">
                    {safeNumber(scoreDetails?.averagePerformance, 0).toFixed(1)}
                  </div>
                </div>

                <div className="bg-[#474973] rounded-lg border border-[#A69CAC]/40 p-3">
                  <div className="text-[10px] text-[#A69CAC] mb-1">평균 PerfIndex</div>
                  <div className={`text-lg font-bold ${getDeltaColor(averagePerfIndex)}`}>
                    {formatSignedNumber(averagePerfIndex, 2)}
                  </div>
                </div>

                <div className="bg-[#474973] rounded-lg border border-[#A69CAC]/40 p-3">
                  <div className="text-[10px] text-[#A69CAC] mb-1">집계 경기 수</div>
                  <div className="text-lg font-bold text-[#F1DAC4]">
                    {safeNumber(scoreDetails?.countedGames, 0)}
                    <span className="text-xs text-[#A69CAC] ml-1">/ {scoreSampleCount}</span>
                  </div>
                </div>
              </div>
            </div>

            <div className="bg-[#161B33] rounded-xl border border-[#474973] overflow-hidden">
              <div className="px-4 py-3 border-b border-[#474973] text-xs font-bold text-[#F1DAC4] flex items-center gap-1.5">
                <Flame className="w-3.5 h-3.5 text-[#A69CAC]" />
                모스트 챔피언
              </div>

              <div className="p-1.5">
                {recentChampionRows.length ? (
                  recentChampionRows.map((champ, idx) => (
                    <div
                      key={`${champ.name}-${idx}`}
                      className="flex items-center gap-3 p-2.5 hover:bg-[#474973] rounded-lg transition-colors cursor-default"
                    >
                      <img
                        src={`https://ddragon.leagueoflegends.com/cdn/${PROFILE_ICON_VERSION}/img/champion/${champ.name}.png`}
                        alt={champ.name}
                        className="w-8 h-8 rounded-full bg-[#474973]"
                      />

                      <div className="flex-1 min-w-0">
                        <div className="text-sm font-semibold text-[#F1DAC4] leading-tight truncate">{champ.name}</div>
                        <div className="text-[10px] text-[#A69CAC] mt-0.5">{champ.games} 게임</div>
                      </div>

                      <div className="text-right">
                        <div
                          className={`text-sm font-semibold ${
                            safeNumber(champ.winRate, 0) >= 60 ? 'text-[#F1DAC4]' : 'text-[#A69CAC]'
                          }`}
                        >
                          {safeNumber(champ.winRate, 0)}%
                        </div>
                        <div className="text-[10px] text-[#A69CAC]">{safeString(champ.kda, '0.0')} 평점</div>
                      </div>
                    </div>
                  ))
                ) : (
                  <div className="p-4 text-xs text-[#8B86A3] text-center">표시할 챔피언 데이터가 없습니다.</div>
                )}
              </div>
            </div>
          </div>

          <div className="flex-1 flex flex-col gap-2">
            {Array.isArray(data?.matches) &&
              data.matches.map((match) => (
                <MatchCard
                  key={`${match.id}-${expandedMatchId === match.id ? 'open' : 'closed'}`}
                  match={match}
                  isExpanded={expandedMatchId === match.id}
                  onToggle={() => setExpandedMatchId((prev) => (prev === match.id ? null : match.id))}
                />
              ))}

            {!data?.matches?.length && (
              <div className="w-full py-10 bg-[#161B33] border border-[#474973] rounded-lg text-sm text-[#A69CAC] text-center">
                최근 전적이 없습니다.
              </div>
            )}
          </div>
        </div>
      </main>
    </div>
  );
}