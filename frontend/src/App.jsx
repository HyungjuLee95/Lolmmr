import React, { useCallback, useEffect, useState } from 'react';
import axios from 'axios';
import MatchCard from './components/MatchCard';
import { Flame, Search, Trophy } from './components/icons';
import { MOCK_DATA } from './data/mmrMockData';
import { mapApiToUiData } from './utils/mmrMapper';

export default function App() {
  const [searchInput, setSearchInput] = useState('Hide on bush#KR1');
  const [data, setData] = useState(MOCK_DATA);
  const [expandedMatchId, setExpandedMatchId] = useState(null);
  const [isLoading, setIsLoading] = useState(false);
  const [errorMessage, setErrorMessage] = useState('');

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

      if (response.data?.error) {
        throw new Error(response.data.error);
      }

      setData(mapApiToUiData(response.data, 'solo'));
      setExpandedMatchId(null);
    } catch (error) {
      console.error(error);
      setData(MOCK_DATA);
      setErrorMessage(error?.message || 'API 호출 중 오류가 발생했습니다.');
    } finally {
      setIsLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchMmrData('Hide on bush#KR1');
  }, [fetchMmrData]);

  const getGradeColor = (grade = '') => {
    if (grade.startsWith('S')) {
      return 'text-yellow-400 drop-shadow-[0_0_8px_rgba(250,204,21,0.5)]';
    }
    if (grade.startsWith('A')) return 'text-purple-400';
    if (grade.startsWith('B')) return 'text-blue-400';
    return 'text-gray-400';
  };

  return (
    <div className="min-h-screen bg-[#121212] text-gray-200 font-sans pb-20">
      <header className="bg-[#18181b] border-b border-gray-800 sticky top-0 z-50">
        <div className="max-w-6xl mx-auto px-4 py-4 flex flex-col md:flex-row items-center justify-between gap-4">
          <div className="flex items-center gap-2 text-2xl font-bold text-blue-500">
            <Trophy className="w-8 h-8" />
            <span>LOLMMR</span>
          </div>

          <form
            className="w-full md:w-[400px] relative"
            onSubmit={(e) => {
              e.preventDefault();
              const trimmed = searchInput.trim();

              if (!trimmed) {
                setErrorMessage('소환사명을 입력해주세요.');
                return;
              }

              fetchMmrData(trimmed);
            }}
          >
            <input
              type="text"
              value={searchInput}
              onChange={(e) => setSearchInput(e.target.value)}
              placeholder="소환사명, 챔피언..."
              className="w-full bg-[#27272a] text-white px-4 py-2.5 pl-10 rounded-lg border border-gray-700 focus:outline-none focus:border-blue-500 text-sm"
            />
            <Search className="w-4 h-4 text-gray-400 absolute left-3 top-3.5" />
            <button
              type="submit"
              className="absolute right-2 top-1.5 bg-blue-600 hover:bg-blue-700 text-white px-3 py-1 rounded text-xs font-medium"
            >
              검색
            </button>
          </form>
        </div>

        {(isLoading || errorMessage) && (
          <div className="max-w-6xl mx-auto px-4 pb-4">
            {isLoading && (
              <div className="text-xs text-blue-300">데이터를 불러오는 중...</div>
            )}
            {errorMessage && (
              <div className="text-xs text-amber-300 mt-1">{errorMessage}</div>
            )}
          </div>
        )}
      </header>

      <main className="max-w-6xl mx-auto px-4 py-8">
        <div className="flex flex-col lg:flex-row gap-6">
          <div className="w-full lg:w-80 flex flex-col gap-4 flex-shrink-0">
            <div className="bg-[#1c1c1f] rounded-xl p-5 border border-gray-800 relative overflow-hidden">
              <div className="absolute top-0 left-0 w-full h-2 bg-gradient-to-r from-blue-600 to-purple-600" />

              <div className="flex items-start gap-4">
                <div className="relative">
                  <img
                    src={`https://ddragon.leagueoflegends.com/cdn/14.3.1/img/profileicon/${data?.summoner?.profileIconId}.png`}
                    alt="Profile"
                    className="w-20 h-20 rounded-xl border border-gray-700 object-cover"
                  />
                  <div className="absolute -bottom-2 left-1/2 -translate-x-1/2 bg-[#121212] border border-gray-700 text-[10px] px-2 py-0.5 rounded-full text-gray-300 whitespace-nowrap">
                    LV {data?.summoner?.summonerLevel}
                  </div>
                </div>

                <div>
                  <h1 className="text-xl font-bold text-white mb-2">
                    {data?.summoner?.name}
                  </h1>

                  <div className="flex gap-2">
                    <div className="bg-[#27272a] px-2 py-1 rounded flex flex-col items-center border border-gray-700/50">
                      <span className="text-[10px] text-gray-400">자체 평가</span>
                      <span
                        className={`text-sm font-black ${getGradeColor(
                          data?.summoner?.scoreDetails?.grade || ''
                        )}`}
                      >
                        {data?.summoner?.scoreDetails?.grade}
                      </span>
                    </div>

                    <div className="bg-[#27272a] px-2 py-1 rounded flex flex-col items-center border border-gray-700/50">
                      <span className="text-[10px] text-gray-400">종합 점수</span>
                      <span className="text-sm font-bold text-white">
                        {data?.summoner?.scoreDetails?.totalScore}
                      </span>
                    </div>
                  </div>
                </div>
              </div>
            </div>

            <div className="bg-[#1c1c1f] rounded-xl p-5 border border-gray-800 flex items-center justify-between">
              <div className="flex flex-col items-center">
                <span className="text-xs text-gray-400 mb-2">최근 20게임 승률</span>
                <div className="relative w-20 h-20 flex items-center justify-center">
                  <svg className="w-full h-full transform -rotate-90">
                    <circle
                      cx="40"
                      cy="40"
                      r="34"
                      fill="transparent"
                      stroke="#3f3f46"
                      strokeWidth="8"
                    />
                    <circle
                      cx="40"
                      cy="40"
                      r="34"
                      fill="transparent"
                      stroke="#3b82f6"
                      strokeWidth="8"
                      strokeDasharray={`${((data?.summary?.winRate || 0) / 100) * 213} 213`}
                    />
                  </svg>

                  <div className="absolute flex flex-col items-center">
                    <span className="text-sm font-bold text-white">
                      {data?.summary?.winRate}%
                    </span>
                    <span className="text-[10px] text-gray-400">
                      {data?.summary?.wins}승 {data?.summary?.losses}패
                    </span>
                  </div>
                </div>
              </div>

              <div className="h-16 w-px bg-gray-800" />

              <div className="flex flex-col justify-center text-center">
                <span className="text-xs text-gray-400 mb-1">KDA 평점</span>
                <div className="text-xl font-bold text-blue-400">
                  {data?.summary?.kda}
                </div>
                <div className="text-[10px] text-gray-500 mt-1">킬관여율 55%</div>
              </div>
            </div>

            <div className="bg-[#1c1c1f] rounded-xl border border-gray-800 overflow-hidden">
              <div className="px-4 py-3 border-b border-gray-800 text-xs font-bold text-gray-300 flex items-center gap-1.5">
                <Flame className="w-3.5 h-3.5 text-orange-500" />
                모스트 챔피언
              </div>

              <div className="p-1.5">
                {data?.summary?.recentChampions?.map((champ, idx) => (
                  <div
                    key={idx}
                    className="flex items-center gap-3 p-2.5 hover:bg-[#27272a] rounded-lg transition-colors cursor-default"
                  >
                    <img
                      src={`https://ddragon.leagueoflegends.com/cdn/14.3.1/img/champion/${champ.name}.png`}
                      alt={champ.name}
                      className="w-8 h-8 rounded-full bg-gray-700"
                    />

                    <div className="flex-1">
                      <div className="text-sm font-semibold text-gray-200 leading-tight">
                        {champ.name}
                      </div>
                      <div className="text-[10px] text-gray-400 mt-0.5">
                        {champ.games} 게임
                      </div>
                    </div>

                    <div className="text-right">
                      <div
                        className={`text-sm font-semibold ${
                          champ.winRate >= 60 ? 'text-red-400' : 'text-gray-300'
                        }`}
                      >
                        {champ.winRate}%
                      </div>
                      <div className="text-[10px] text-gray-400">
                        {champ.kda} 평점
                      </div>
                    </div>
                  </div>
                ))}
              </div>
            </div>
          </div>

          <div className="flex-1 flex flex-col gap-2">
            {data?.matches?.map((match) => (
              <MatchCard
                key={`${match.id}-${expandedMatchId === match.id ? 'open' : 'closed'}`}
                match={match}
                isExpanded={expandedMatchId === match.id}
                onToggle={() =>
                  setExpandedMatchId((prev) => (prev === match.id ? null : match.id))
                }
              />
            ))}

            <button className="w-full py-3 bg-[#1c1c1f] hover:bg-[#27272a] border border-gray-800 rounded-lg text-sm font-medium text-gray-300 transition-colors mt-2">
              더 보기
            </button>
          </div>
        </div>
      </main>
    </div>
  );
}
