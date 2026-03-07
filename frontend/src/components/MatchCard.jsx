import React, { useEffect, useMemo, useRef, useState } from 'react';
import axios from 'axios';
import {
  LineChart,
  Line,
  XAxis,
  YAxis,
  Tooltip as RechartsTooltip,
  ResponsiveContainer,
  CartesianGrid,
  Legend,
  Radar,
  RadarChart,
  PolarGrid,
  PolarAngleAxis,
} from 'recharts';
import {
  Activity,
  AlertCircle,
  ChevronDown,
  ChevronRight,
  Swords,
  Target,
  ThumbsUp,
  Users,
} from './icons';
import ComparisonBar from './ComparisonBar';
import PlayerRow from './PlayerRow';

const RESULT_STYLE = {
  WIN: {
    cardBorder: 'border-l-blue-500 hover:bg-[#1e293b]/60',
    resultText: 'text-blue-400',
    blueBox: 'bg-[#1e293b]/20 border border-blue-900/30',
    blueHeader: 'bg-[#1e293b]/50 border-b border-blue-900/30 text-blue-400',
    redBox: 'bg-[#3f1d24]/20 border border-red-900/30',
    redHeader: 'bg-[#3f1d24]/50 border-b border-red-900/30 text-red-400',
  },
  LOSS: {
    cardBorder: 'border-l-red-500 hover:bg-[#3f1d24]/60',
    resultText: 'text-red-400',
    blueBox: 'bg-[#1e293b]/20 border border-blue-900/30',
    blueHeader: 'bg-[#1e293b]/50 border-b border-blue-900/30 text-blue-400',
    redBox: 'bg-[#3f1d24]/20 border border-red-900/30',
    redHeader: 'bg-[#3f1d24]/50 border-b border-red-900/30 text-red-400',
  },
  REMAKE: {
    cardBorder: 'border-l-yellow-500 hover:bg-[#3b3220]/60',
    resultText: 'text-yellow-400',
    blueBox: 'bg-[#27272a]/30 border border-yellow-800/20',
    blueHeader: 'bg-[#27272a]/50 border-b border-yellow-800/20 text-yellow-300',
    redBox: 'bg-[#27272a]/30 border border-yellow-800/20',
    redHeader: 'bg-[#27272a]/50 border-b border-yellow-800/20 text-yellow-300',
  },
  INVALID: {
    cardBorder: 'border-l-gray-500 hover:bg-[#27272a]/60',
    resultText: 'text-gray-400',
    blueBox: 'bg-[#27272a]/30 border border-gray-700/40',
    blueHeader: 'bg-[#27272a]/50 border-b border-gray-700/40 text-gray-300',
    redBox: 'bg-[#27272a]/30 border border-gray-700/40',
    redHeader: 'bg-[#27272a]/50 border-b border-gray-700/40 text-gray-300',
  },
};

const getResultStyle = (resultType) => RESULT_STYLE[resultType] || RESULT_STYLE.INVALID;

const getTeamHeaderLabel = (team, fallbackTeamName) => {
  if (!team?.hasWinner) {
    return `다시하기 (${fallbackTeamName})`;
  }
  return `${team.isWin ? '승리' : '패배'} (${fallbackTeamName})`;
};

const findMetricCard = (metricCards, key) => metricCards.find((card) => card.key === key);

const buildRadarData = (metricCards) => {
  const mapping = [
    { key: 'kp', subject: '킬관여' },
    { key: 'cspm', subject: 'CS' },
    { key: 'gpm', subject: '골드' },
    { key: 'dpm', subject: '딜량' },
    { key: 'vision', subject: '시야' },
    { key: 'deadTime', subject: '생존' },
  ];

  return mapping.map(({ key, subject }) => {
    const metric = findMetricCard(metricCards, key);
    return {
      subject,
      score: metric?.score || 0,
    };
  });
};

const buildTimelineData = (timelineBuckets = []) =>
  timelineBuckets.map((bucket) => ({
    time: String(bucket.minute),
    gold: bucket.totalGold || 0,
    cs: bucket.totalCs || 0,
    impact: bucket.impactScore || 0,
  }));

const formatMetricValue = (metric) => {
  if (!metric) return '-';
  if (metric.unit) {
    return `${metric.value}${metric.unit}`;
  }
  return String(metric.value);
};

const MatchCard = ({ match, isExpanded, onToggle }) => {
  const [activeTab, setActiveTab] = useState('overview');
  const [analysisDetail, setAnalysisDetail] = useState(null);
  const [analysisStatus, setAnalysisStatus] = useState('idle'); // idle | loading | success | error
  const [analysisError, setAnalysisError] = useState('');
  const analysisRequestedRef = useRef(false);

  const resultType = match.resultType || (match.win ? 'WIN' : 'LOSS');
  const resultLabel = match.resultLabel || (match.win ? '승리' : '패배');
  const resultStyle = useMemo(() => getResultStyle(resultType), [resultType]);

  useEffect(() => {
    setAnalysisDetail(null);
    setAnalysisStatus('idle');
    setAnalysisError('');
    setActiveTab('overview');
    analysisRequestedRef.current = false;
  }, [match.matchId]);

  useEffect(() => {
    if (!isExpanded || activeTab !== 'analysis') {
      return;
    }

    if (analysisRequestedRef.current) {
      return;
    }

    if (!match.matchId || !match.puuid) {
      setAnalysisError('상세 분석 요청에 필요한 matchId 또는 puuid가 없습니다.');
      setAnalysisStatus('error');
      return;
    }

    analysisRequestedRef.current = true;
    setAnalysisStatus('loading');
    setAnalysisError('');

    axios
      .get(`/api/matches/${match.matchId}/analysis`, {
        params: { puuid: match.puuid },
      })
      .then((response) => {
        if (response.data?.error) {
          throw new Error(response.data.error);
        }

        setAnalysisDetail(response.data);
        setAnalysisStatus('success');
      })
      .catch((error) => {
        setAnalysisError(error?.message || '상세 분석을 불러오지 못했습니다.');
        setAnalysisStatus('error');
        analysisRequestedRef.current = false;
      });
  }, [isExpanded, activeTab, match.matchId, match.puuid]);

  const metricCards = useMemo(() => analysisDetail?.metricCards ?? [], [analysisDetail]);
  const comments = useMemo(() => analysisDetail?.coachingComments ?? [], [analysisDetail]);
  const laneComparison = useMemo(() => analysisDetail?.laneComparison ?? null, [analysisDetail]);
  const timelineData = useMemo(
    () => buildTimelineData(analysisDetail?.timelineBuckets ?? []),
    [analysisDetail],
  );
  const radarData = useMemo(() => buildRadarData(metricCards), [metricCards]);

  const isExcludedAnalysis = useMemo(
    () =>
      Boolean(
        analysisDetail &&
          (analysisDetail.resultType === 'REMAKE' || analysisDetail.resultType === 'INVALID'),
      ),
    [analysisDetail],
  );

  return (
    <div className="flex flex-col rounded-xl border border-gray-800 bg-[#1c1c1f] overflow-hidden">
      <div
        onClick={onToggle}
        className={`flex flex-col sm:flex-row cursor-pointer border-l-4 ${resultStyle.cardBorder} transition-colors`}
      >
        <div className="w-full sm:w-28 p-3 flex flex-col justify-center text-xs text-gray-400 border-b sm:border-b-0 sm:border-r border-gray-800/50">
          <div className={`font-bold mb-1 ${resultStyle.resultText}`}>{resultLabel}</div>
          <div className="mb-1">{match.gameType}</div>
          <div>{match.timeAgo}</div>
          <div className="border-t border-gray-700 my-1" />
          <div>{match.gameDuration}</div>
        </div>

        <div className="flex-1 p-3 flex items-center gap-4">
          <div className="flex items-center gap-2">
            <img
              src={`https://ddragon.leagueoflegends.com/cdn/14.3.1/img/champion/${match.summary.champion}.png`}
              alt={match.summary.champion}
              className="w-10 h-10 sm:w-12 sm:h-12 rounded-full border border-gray-700"
            />
            <div className="flex flex-col gap-1 hidden sm:flex">
              <div className="w-4 h-4 sm:w-5 sm:h-5 bg-gray-700 rounded text-[8px] flex items-center justify-center border border-gray-600">
                D
              </div>
              <div className="w-4 h-4 sm:w-5 sm:h-5 bg-gray-700 rounded text-[8px] flex items-center justify-center border border-gray-600">
                F
              </div>
            </div>
          </div>

          <div className="flex flex-col items-center flex-1">
            <div className="font-bold text-gray-200 text-base sm:text-lg tracking-wide">
              {match.summary.kills} <span className="text-gray-500 font-normal">/</span>{' '}
              <span className="text-red-400">{match.summary.deaths}</span>{' '}
              <span className="text-gray-500 font-normal">/</span> {match.summary.assists}
            </div>
            <div className="text-xs text-gray-400 mt-0.5">
              {match.isCountedGame ? `${match.summary.kda} 평점` : '집계 제외 경기'}
            </div>
          </div>

          <div className="flex flex-col items-center justify-center w-16 sm:w-20 text-xs text-gray-400">
            <div>CS {match.summary.cs}</div>
          </div>
        </div>

        <div className="w-full sm:w-auto p-3 flex items-center justify-center bg-black/10">
          <div className="grid grid-cols-4 sm:grid-cols-3 gap-1">
            {match.summary.items.map((item, idx) => (
              <div
                key={idx}
                className={`w-6 h-6 rounded ${item === 0 ? 'bg-gray-800/50' : 'bg-gray-700'}`}
              >
                {item !== 0 && (
                  <img
                    src={`https://ddragon.leagueoflegends.com/cdn/14.3.1/img/item/${item}.png`}
                    alt="item"
                    className="w-full h-full rounded"
                  />
                )}
              </div>
            ))}
          </div>
        </div>

        <div className="hidden sm:flex w-10 items-center justify-center bg-black/20 text-gray-500 hover:text-gray-300 transition-colors">
          {isExpanded ? <ChevronDown className="w-5 h-5" /> : <ChevronRight className="w-5 h-5" />}
        </div>
      </div>

      {isExpanded && (
        <div className="border-t border-gray-800 bg-[#121215] flex flex-col animate-in slide-in-from-top-2 duration-200">
          <div className="flex border-b border-gray-800 bg-[#18181b] overflow-x-auto">
            <button
              onClick={() => setActiveTab('overview')}
              className={`px-4 py-3 text-xs md:text-sm font-semibold transition-colors border-b-2 flex items-center gap-1 md:gap-2 whitespace-nowrap ${
                activeTab === 'overview'
                  ? 'border-blue-500 text-blue-400'
                  : 'border-transparent text-gray-400 hover:text-gray-200'
              }`}
            >
              <Swords className="w-4 h-4" /> 종합 전적
            </button>
            <button
              onClick={() => setActiveTab('analysis')}
              className={`px-4 py-3 text-xs md:text-sm font-semibold transition-colors border-b-2 flex items-center gap-1 md:gap-2 whitespace-nowrap ${
                activeTab === 'analysis'
                  ? 'border-purple-500 text-purple-400'
                  : 'border-transparent text-gray-400 hover:text-gray-200'
              }`}
            >
              <Activity className="w-4 h-4" /> 상세 분석 및 코칭
            </button>
          </div>

          {activeTab === 'overview' && (
            <div className="flex flex-col gap-3 p-3 md:p-4">
              <div className={`rounded-lg ${resultStyle.blueBox}`}>
                <div className={`px-3 py-2 flex justify-between items-center ${resultStyle.blueHeader}`}>
                  <span className="text-xs font-bold">
                    {getTeamHeaderLabel(match.overview.blueTeam, '블루팀')}
                  </span>
                  <span className="text-[10px] md:text-xs text-gray-400">
                    총 킬: {match.overview.blueTeam.kills}
                  </span>
                </div>
                <div className="flex flex-col">
                  {match.overview.blueTeam.players.map((p) => (
                    <PlayerRow key={p.id} player={p} isBlueTeam />
                  ))}
                </div>
              </div>

              <div className={`rounded-lg ${resultStyle.redBox}`}>
                <div className={`px-3 py-2 flex justify-between items-center ${resultStyle.redHeader}`}>
                  <span className="text-xs font-bold">
                    {getTeamHeaderLabel(match.overview.redTeam, '레드팀')}
                  </span>
                  <span className="text-[10px] md:text-xs text-gray-400">
                    총 킬: {match.overview.redTeam.kills}
                  </span>
                </div>
                <div className="flex flex-col">
                  {match.overview.redTeam.players.map((p) => (
                    <PlayerRow key={p.id} player={p} isBlueTeam={false} />
                  ))}
                </div>
              </div>
            </div>
          )}

          {activeTab === 'analysis' && (
            <div className="flex flex-col p-3 md:p-4 gap-4">
              <div className="bg-[#18181b] px-4 py-3 flex items-center justify-between border border-gray-800 rounded-xl">
                <div className="flex items-center gap-2 text-xs text-gray-300">
                  <Users className="w-4 h-4 text-purple-400" />
                  <span className="font-semibold">분석 기준: 내 플레이</span>
                </div>
                <div className="text-[11px] text-gray-500">
                  {analysisDetail?.summary?.championName || match.summary.champion}
                </div>
              </div>

              {analysisStatus === 'error' && (
                <div className="bg-[#1c1c1f] rounded-xl border border-red-900/40 p-6 text-sm text-red-300 text-center">
                  {analysisError}
                </div>
              )}

              {analysisStatus === 'success' && analysisDetail && (
                <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                  <div className="bg-[#1c1c1f] rounded-xl border border-gray-800/60 p-3 flex flex-col items-center col-span-1">
                    <h3 className="text-xs font-bold text-gray-300 mb-2">플레이 성향 지표</h3>
                    {metricCards.length > 0 ? (
                      <div className="w-full h-40">
                        <ResponsiveContainer width="100%" height="100%">
                          <RadarChart cx="50%" cy="50%" outerRadius="65%" data={radarData}>
                            <PolarGrid stroke="#3f3f46" />
                            <PolarAngleAxis dataKey="subject" tick={{ fill: '#a1a1aa', fontSize: 10 }} />
                            <Radar name="지표" dataKey="score" stroke="#a855f7" fill="#a855f7" fillOpacity={0.4} />
                          </RadarChart>
                        </ResponsiveContainer>
                      </div>
                    ) : (
                      <div className="w-full h-40 flex items-center justify-center text-xs text-gray-500">
                        집계 제외 경기라 지표를 표시하지 않습니다.
                      </div>
                    )}
                  </div>

                  <div className="bg-[#1c1c1f] rounded-xl border border-gray-800/60 p-3 flex flex-col col-span-1">
                    <h3 className="text-xs font-bold text-gray-300 mb-3 flex items-center gap-1">
                      <Users className="w-3 h-3 text-orange-400" /> 맞라인 상대 비교
                    </h3>
                    {laneComparison ? (
                      <div className="flex flex-col justify-center px-1">
                        <div className="flex justify-between items-center mb-3">
                          <div className="flex items-center gap-2">
                            <img
                              src={`https://ddragon.leagueoflegends.com/cdn/14.3.1/img/champion/${laneComparison.myChampionName}.png`}
                              className="w-7 h-7 rounded-full border border-blue-500"
                              alt=""
                            />
                            <span className="text-[10px] font-bold text-blue-400 hidden sm:block">
                              {laneComparison.myChampionName}
                            </span>
                          </div>
                          <span className="text-[10px] text-gray-500 font-bold px-2 py-0.5 bg-gray-800 rounded-full">
                            VS
                          </span>
                          <div className="flex items-center gap-2">
                            <span className="text-[10px] font-bold text-red-400 hidden sm:block">
                              {laneComparison.opponentChampionName}
                            </span>
                            <img
                              src={`https://ddragon.leagueoflegends.com/cdn/14.3.1/img/champion/${laneComparison.opponentChampionName}.png`}
                              className="w-7 h-7 rounded-full border border-red-500"
                              alt=""
                            />
                          </div>
                        </div>
                        <ComparisonBar
                          label="CS"
                          myValue={laneComparison.myCs}
                          oppValue={laneComparison.opponentCs}
                        />
                        <ComparisonBar
                          label="골드"
                          myValue={laneComparison.myGoldEarned}
                          oppValue={laneComparison.opponentGoldEarned}
                        />
                        <ComparisonBar
                          label="딜량"
                          myValue={laneComparison.myDamageToChampions}
                          oppValue={laneComparison.opponentDamageToChampions}
                        />
                      </div>
                    ) : (
                      <div className="text-xs text-gray-500 text-center mt-8">상대를 찾을 수 없습니다.</div>
                    )}
                  </div>

                  <div className="bg-[#1c1c1f] rounded-xl border border-gray-800/60 p-3 md:col-span-2">
                    <h3 className="text-xs font-bold text-gray-300 mb-3">핵심 지표 카드</h3>
                    {metricCards.length > 0 ? (
                      <div className="grid grid-cols-2 md:grid-cols-4 gap-2">
                        {metricCards.map((metric) => (
                          <div key={metric.key} className="rounded-lg border border-gray-800 bg-[#18181b] p-3">
                            <div className="text-[10px] text-gray-400">{metric.label}</div>
                            <div className="text-sm font-bold text-white mt-1">{formatMetricValue(metric)}</div>
                            <div className="text-[10px] text-purple-300 mt-1">점수 {metric.score}</div>
                            <div className="text-[10px] text-gray-500 mt-1 leading-tight">
                              {metric.description}
                            </div>
                          </div>
                        ))}
                      </div>
                    ) : (
                      <div className="text-xs text-gray-500 text-center py-4">
                        집계 제외 경기라 성과 카드를 표시하지 않습니다.
                      </div>
                    )}
                  </div>

                  <div className="bg-[#1c1c1f] rounded-xl border border-gray-800/60 p-3 md:col-span-2">
                    <h3 className="text-xs font-bold text-gray-300 mb-2">시간대별 성장 추이</h3>
                    {timelineData.length > 0 ? (
                      <div className="w-full h-44">
                        <ResponsiveContainer width="100%" height="100%">
                          <LineChart
                            data={timelineData}
                            margin={{ top: 5, right: 10, bottom: 0, left: -25 }}
                          >
                            <CartesianGrid strokeDasharray="3 3" stroke="#27272a" vertical={false} />
                            <XAxis
                              dataKey="time"
                              stroke="#71717a"
                              tick={{ fill: '#71717a', fontSize: 10 }}
                              tickFormatter={(val) => `${val}분`}
                            />
                            <YAxis
                              yAxisId="left"
                              stroke="#eab308"
                              tick={{ fill: '#eab308', fontSize: 10 }}
                              tickFormatter={(val) => `${Math.round(val / 1000)}k`}
                            />
                            <YAxis
                              yAxisId="right"
                              orientation="right"
                              stroke="#60a5fa"
                              tick={{ fill: '#60a5fa', fontSize: 10 }}
                            />
                            <RechartsTooltip
                              contentStyle={{
                                backgroundColor: '#18181b',
                                borderColor: '#3f3f46',
                                borderRadius: '8px',
                                fontSize: '11px',
                              }}
                            />
                            <Legend
                              verticalAlign="top"
                              height={24}
                              wrapperStyle={{ fontSize: '10px', color: '#a1a1aa' }}
                            />
                            <Line
                              yAxisId="left"
                              name="골드"
                              type="monotone"
                              dataKey="gold"
                              stroke="#eab308"
                              strokeWidth={2}
                              dot={{ r: 2 }}
                            />
                            <Line
                              yAxisId="right"
                              name="CS"
                              type="monotone"
                              dataKey="cs"
                              stroke="#60a5fa"
                              strokeWidth={2}
                              dot={{ r: 2 }}
                            />
                          </LineChart>
                        </ResponsiveContainer>
                      </div>
                    ) : (
                      <div className="w-full h-44 flex items-center justify-center text-xs text-gray-500">
                        {isExcludedAnalysis
                          ? '집계 제외 경기라 시간대별 성장 추이를 표시하지 않습니다.'
                          : '타임라인 데이터가 없습니다.'}
                      </div>
                    )}
                  </div>

                  <div className="bg-[#1c1c1f] rounded-xl border border-gray-800/60 p-3 md:col-span-2">
                    <h3 className="text-xs font-bold text-gray-300 mb-2 flex items-center gap-1">
                      <Target className="w-3 h-3 text-purple-400" /> 시스템 코멘트
                    </h3>
                    <div className="grid grid-cols-1 sm:grid-cols-2 gap-2">
                      {comments.map((item, idx) => (
                        <div
                          key={idx}
                          className={`p-2 rounded-lg border text-[11px] flex items-start gap-2 ${
                            item.type === 'good'
                              ? 'bg-blue-900/10 border-blue-500/20 text-blue-100'
                              : item.type === 'warning'
                                ? 'bg-yellow-900/10 border-yellow-500/20 text-yellow-100'
                                : 'bg-red-900/10 border-red-500/20 text-red-100'
                          }`}
                        >
                          {item.type === 'good' && (
                            <ThumbsUp className="w-3 h-3 text-blue-400 mt-0.5 flex-shrink-0" />
                          )}
                          {item.type === 'warning' && (
                            <AlertCircle className="w-3 h-3 text-yellow-400 mt-0.5 flex-shrink-0" />
                          )}
                          {item.type === 'bad' && (
                            <AlertCircle className="w-3 h-3 text-red-400 mt-0.5 flex-shrink-0" />
                          )}
                          <div className="leading-tight">
                            {item.title && <div className="font-semibold mb-0.5">{item.title}</div>}
                            <div>{item.text}</div>
                          </div>
                        </div>
                      ))}
                      {!comments.length && (
                        <div className="text-xs text-gray-500">표시할 코멘트가 없습니다.</div>
                      )}
                    </div>
                  </div>
                </div>
              )}

              {analysisStatus === 'idle' && (
                <div className="text-xs text-gray-500 text-center py-4">
                  상세 분석을 준비 중입니다.
                </div>
              )}
            </div>
          )}
        </div>
      )}
    </div>
  );
};

export default MatchCard;