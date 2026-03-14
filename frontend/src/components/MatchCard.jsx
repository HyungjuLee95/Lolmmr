import React, { useEffect, useMemo, useState } from 'react';
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
    cardBorder: 'border-l-[#F1DAC4] hover:bg-[#474973]/35',
    resultText: 'text-[#F1DAC4]',
    blueBox: 'bg-[#474973]/25 border border-[#A69CAC]/35',
    blueHeader: 'bg-[#474973]/55 border-b border-[#A69CAC]/35 text-[#F1DAC4]',
    redBox: 'bg-[#161B33]/30 border border-[#A69CAC]/30',
    redHeader: 'bg-[#161B33]/55 border-b border-[#A69CAC]/30 text-[#A69CAC]',
  },
  LOSS: {
    cardBorder: 'border-l-[#A69CAC] hover:bg-[#161B33]/55',
    resultText: 'text-[#A69CAC]',
    blueBox: 'bg-[#474973]/25 border border-[#A69CAC]/35',
    blueHeader: 'bg-[#474973]/55 border-b border-[#A69CAC]/35 text-[#F1DAC4]',
    redBox: 'bg-[#161B33]/30 border border-[#A69CAC]/30',
    redHeader: 'bg-[#161B33]/55 border-b border-[#A69CAC]/30 text-[#A69CAC]',
  },
  REMAKE: {
    cardBorder: 'border-l-[#F1DAC4] hover:bg-[#474973]/35',
    resultText: 'text-[#F1DAC4]',
    blueBox: 'bg-[#474973]/30 border border-[#A69CAC]/25',
    blueHeader: 'bg-[#474973]/55 border-b border-[#A69CAC]/30 text-[#F1DAC4]',
    redBox: 'bg-[#474973]/30 border border-[#A69CAC]/25',
    redHeader: 'bg-[#474973]/55 border-b border-[#A69CAC]/30 text-[#F1DAC4]',
  },
  INVALID: {
    cardBorder: 'border-l-[#474973] hover:bg-[#474973]/60',
    resultText: 'text-[#A69CAC]',
    blueBox: 'bg-[#474973]/30 border border-[#A69CAC]/30',
    blueHeader: 'bg-[#474973]/55 border-b border-[#A69CAC]/30 text-[#F1DAC4]',
    redBox: 'bg-[#474973]/30 border border-[#A69CAC]/30',
    redHeader: 'bg-[#474973]/55 border-b border-[#A69CAC]/30 text-[#F1DAC4]',
  },
};

const TIMELINE_METRIC_OPTIONS = [
  {
    key: 'gold',
    label: '골드',
    myDataKey: 'myGold',
    opponentDataKey: 'opponentGold',
    unit: '골드',
    formatter: (value) => Number(value || 0).toLocaleString(),
  },
  {
    key: 'xp',
    label: '경험치',
    myDataKey: 'myXp',
    opponentDataKey: 'opponentXp',
    unit: 'XP',
    formatter: (value) => Number(value || 0).toLocaleString(),
  },
  {
    key: 'cs',
    label: 'CS',
    myDataKey: 'myCs',
    opponentDataKey: 'opponentCs',
    unit: 'CS',
    formatter: (value) => Number(value || 0).toLocaleString(),
  },
  {
    key: 'growth',
    label: '성장 점수',
    myDataKey: 'myGrowth',
    opponentDataKey: 'opponentGrowth',
    unit: '점',
    formatter: (value) => Number(value || 0).toFixed(1),
  },
  {
    key: 'combat',
    label: '전투 점수',
    myDataKey: 'myCombat',
    opponentDataKey: 'opponentCombat',
    unit: '점',
    formatter: (value) => Number(value || 0).toFixed(1),
  },
  {
    key: 'map',
    label: '맵 점수',
    myDataKey: 'myMap',
    opponentDataKey: 'opponentMap',
    unit: '점',
    formatter: (value) => Number(value || 0).toFixed(1),
  },
  {
    key: 'survival',
    label: '생존 점수',
    myDataKey: 'mySurvival',
    opponentDataKey: 'opponentSurvival',
    unit: '점',
    formatter: (value) => Number(value || 0).toFixed(1),
  },
  {
    key: 'impact',
    label: '영향력 점수',
    myDataKey: 'myImpact',
    opponentDataKey: 'opponentImpact',
    unit: '점',
    formatter: (value) => Number(value || 0).toFixed(1),
  },
];

const BUCKET_OPTIONS = [3, 5, 10];

const getResultStyle = (resultType) => RESULT_STYLE[resultType] || RESULT_STYLE.INVALID;

const safeNumber = (value, fallback = 0) => {
  const n = Number(value);
  return Number.isFinite(n) ? n : fallback;
};

const safeString = (value, fallback = '') => {
  if (value === null || value === undefined) return fallback;
  const text = String(value);
  return text.trim() ? text : fallback;
};

const clamp = (value, min, max) => Math.max(min, Math.min(max, value));

const formatSigned = (value, digits = 0) => {
  const n = Number(value);
  if (!Number.isFinite(n)) return digits > 0 ? '0.0' : '0';
  if (n > 0) return `+${n.toFixed(digits)}`;
  return n.toFixed(digits);
};

const normalizePosition = (position) => {
  if (!position) return 'UNKNOWN';

  const raw = String(position).toUpperCase();

  switch (raw) {
    case 'TOP':
      return 'TOP';
    case 'JUNGLE':
      return 'JG';
    case 'MIDDLE':
    case 'MID':
      return 'MID';
    case 'BOTTOM':
    case 'ADC':
      return 'ADC';
    case 'UTILITY':
    case 'SUPPORT':
    case 'SUP':
      return 'SUP';
    default:
      return raw;
  }
};

const getTeamHeaderLabel = (team, fallbackTeamName) => {
  if (!team?.hasWinner) {
    return `다시하기 (${fallbackTeamName})`;
  }
  return `${team.isWin ? '승리' : '패배'} (${fallbackTeamName})`;
};

const formatGold = (value) => `${(safeNumber(value, 0) / 1000).toFixed(1)}k`;

const formatObjectiveSummary = (objectives = {}) => {
  const dragons = objectives.dragons || 0;
  const heralds = objectives.heralds || 0;
  const barons = objectives.barons || 0;
  const towers = objectives.towers || 0;
  const voidgrubs = objectives.voidgrubs || 0;

  return `드 ${dragons} · 전 ${heralds} · 바 ${barons} · 타 ${towers} · 그 ${voidgrubs}`;
};

const toOverviewPlayer = (player, maxDamage) => ({
  id: player.participantId,
  name: player.riotId || 'Unknown',
  champion: player.championName || 'Ahri',
  position: normalizePosition(player.teamPosition),
  kills: player.kills || 0,
  deaths: player.deaths || 0,
  assists: player.assists || 0,
  damage: player.damageToChampions || 0,
  gold: player.goldEarned || 0,
  cs: player.totalCs || 0,
  items: player.items || [],
  isMe: Boolean(player.me),
  maxDamage,
});

const buildOverviewFromAnalysis = (analysisDetail, fallbackOverview) => {
  if (!analysisDetail?.blueTeamPlayers?.length || !analysisDetail?.redTeamPlayers?.length) {
    return fallbackOverview;
  }

  const rawBluePlayers = analysisDetail.blueTeamPlayers || [];
  const rawRedPlayers = analysisDetail.redTeamPlayers || [];
  const allPlayers = [...rawBluePlayers, ...rawRedPlayers];
  const maxDamage = Math.max(...allPlayers.map((player) => player?.damageToChampions || 0), 1);

  const bluePlayers = rawBluePlayers.map((player) => toOverviewPlayer(player, maxDamage));
  const redPlayers = rawRedPlayers.map((player) => toOverviewPlayer(player, maxDamage));

  const countedGame = !['REMAKE', 'INVALID'].includes(analysisDetail.resultType);
  const meInBlue = rawBluePlayers.some((p) => p.me);
  const meInRed = rawRedPlayers.some((p) => p.me);

  let blueWin = false;
  if (countedGame) {
    if (meInBlue) {
      blueWin = analysisDetail.resultType === 'WIN';
    } else if (meInRed) {
      blueWin = analysisDetail.resultType === 'LOSS';
    } else {
      blueWin = fallbackOverview?.blueTeam?.isWin ?? false;
    }
  }

  const blueSummary = analysisDetail.blueTeamSummary || {};
  const redSummary = analysisDetail.redTeamSummary || {};

  return {
    blueTeam: {
      isWin: blueWin,
      hasWinner: countedGame,
      kills: Number(blueSummary.kills ?? bluePlayers.reduce((sum, p) => sum + (p.kills || 0), 0)),
      deaths: Number(blueSummary.deaths ?? bluePlayers.reduce((sum, p) => sum + (p.deaths || 0), 0)),
      gold: formatGold(
        blueSummary.totalGold ?? bluePlayers.reduce((sum, p) => sum + (p.gold || 0), 0),
      ),
      objectives: blueSummary.objectives || {},
      players: bluePlayers,
    },
    redTeam: {
      isWin: countedGame ? !blueWin : false,
      hasWinner: countedGame,
      kills: Number(redSummary.kills ?? redPlayers.reduce((sum, p) => sum + (p.kills || 0), 0)),
      deaths: Number(redSummary.deaths ?? redPlayers.reduce((sum, p) => sum + (p.deaths || 0), 0)),
      gold: formatGold(
        redSummary.totalGold ?? redPlayers.reduce((sum, p) => sum + (p.gold || 0), 0),
      ),
      objectives: redSummary.objectives || {},
      players: redPlayers,
    },
  };
};

const buildTimelineData = (timelineBuckets = []) =>
  timelineBuckets.map((bucket) => ({
    time: String(bucket.minute ?? 0),

    myGold: bucket.myGold ?? bucket.totalGold ?? 0,
    opponentGold: bucket.opponentGold ?? 0,

    myXp: bucket.myXp ?? 0,
    opponentXp: bucket.opponentXp ?? 0,

    myCs: bucket.myCs ?? bucket.totalCs ?? 0,
    opponentCs: bucket.opponentCs ?? 0,

    myGrowth: bucket.myGrowthScore ?? bucket.growthScore ?? 0,
    opponentGrowth: bucket.opponentGrowthScore ?? 0,

    myCombat: bucket.myCombatScore ?? bucket.combatScore ?? 0,
    opponentCombat: bucket.opponentCombatScore ?? 0,

    myMap: bucket.myMapScore ?? bucket.mapScore ?? 0,
    opponentMap: bucket.opponentMapScore ?? 0,

    mySurvival: bucket.mySurvivalScore ?? bucket.survivalScore ?? 0,
    opponentSurvival: bucket.opponentSurvivalScore ?? 0,

    myImpact: bucket.myImpactScore ?? bucket.impactScore ?? 0,
    opponentImpact: bucket.opponentImpactScore ?? 0,
  }));

const getTimelineMetricMeta = (metricKey) =>
  TIMELINE_METRIC_OPTIONS.find((option) => option.key === metricKey) || TIMELINE_METRIC_OPTIONS[0];

const formatTooltipValue = (value, unit, formatter) => {
  const display = formatter ? formatter(value) : String(value);
  return [`${display} ${unit}`, ''];
};

const centeredScoreToPercent = (value) => clamp(Math.round((safeNumber(value, 0) + 1) * 50), 0, 100);
const ratioToPercent = (value, max = 1) =>
  clamp(Math.round((safeNumber(value, 0) / Math.max(max, 0.0001)) * 100), 0, 100);
const capToPercent = (value, cap) =>
  clamp(Math.round((safeNumber(value, 0) / Math.max(cap, 1)) * 100), 0, 100);
//const inverseCapToPercent = (value, cap) =>
 // clamp(100 - Math.round((safeNumber(value, 0) / Math.max(cap, 1)) * 100), 0, 100);

const findMetricCard = (metricCards, keys) =>
  metricCards.find((card) => keys.includes(card?.key));

const buildRadarData = (metricCards, match) => {
  const scoring = match?.scoring || {};
  const metrics = match?.metrics || {};

  const fallback = {
    line: centeredScoreToPercent(scoring.growthScore),
    kp: ratioToPercent(metrics.killParticipation, 0.7),
    objective: capToPercent(metrics.objectiveParticipationScore, 10),
    vision: ratioToPercent(metrics.visionPerMinute, 2.5),
    efficiency: centeredScoreToPercent(scoring.efficiencyScore),
    death: centeredScoreToPercent(scoring.survivalScore),
  };

  return [
    {
      subject: '라인전',
      score:
        safeNumber(
          findMetricCard(metricCards, ['gd15', 'line', 'growth', 'lane'])?.score,
          fallback.line,
        ) || 0,
    },
    {
      subject: '합류',
      score:
        safeNumber(
          findMetricCard(metricCards, ['kp', 'killParticipation', 'participation'])?.score,
          fallback.kp,
        ) || 0,
    },
    {
      subject: '오브젝트',
      score:
        safeNumber(
          findMetricCard(metricCards, ['objective', 'objectives'])?.score,
          fallback.objective,
        ) || 0,
    },
    {
      subject: '시야',
      score:
        safeNumber(findMetricCard(metricCards, ['vision'])?.score, fallback.vision) || 0,
    },
    {
      subject: '효율',
      score:
        safeNumber(
          findMetricCard(metricCards, ['damageEff', 'efficiency', 'damageConversion'])?.score,
          fallback.efficiency,
        ) || 0,
    },
    {
      subject: '데스관리',
      score:
        safeNumber(
          findMetricCard(metricCards, ['throwDeath', 'deadTime', 'survival'])?.score,
          fallback.death,
        ) || 0,
    },
  ];
};

const buildFallbackMetricCards = (match) => {
  const scoring = match?.scoring || {};
  const metrics = match?.metrics || {};

  return [
    {
      key: 'gd15',
      label: '라인전',
      displayValue: `GD15 ${formatSigned(metrics.goldDiff15, 0)} / CSD15 ${formatSigned(
        metrics.csDiff15,
        0,
      )}`,
      score: centeredScoreToPercent(scoring.growthScore),
      description: '15분 기준 골드·CS·경험치 차이를 합산한 성장 우위입니다.',
    },
    {
      key: 'kp',
      label: '합류',
      displayValue: `${Math.round(safeNumber(metrics.killParticipation, 0) * 100)}%`,
      score: ratioToPercent(metrics.killParticipation, 0.7),
      description: '팀 킬에 얼마나 자주 관여했는지 보여줍니다.',
    },
    {
      key: 'objective',
      label: '오브젝트',
      displayValue: `${safeNumber(metrics.objectiveParticipationScore, 0)}점`,
      score: capToPercent(metrics.objectiveParticipationScore, 10),
      description: '드래곤·전령·바론·타워 등 주요 오브젝트 기여 점수입니다.',
    },
    {
      key: 'vision',
      label: '시야',
      displayValue: `${safeNumber(metrics.visionPerMinute, 0).toFixed(2)}/분`,
      score: ratioToPercent(metrics.visionPerMinute, 2.5),
      description: '분당 시야 점수와 와드 기여를 반영합니다.',
    },
    {
      key: 'damageEff',
      label: '효율',
      displayValue: `${safeNumber(metrics.damageConversion, 0).toFixed(2)}x`,
      score: centeredScoreToPercent(scoring.efficiencyScore),
      description: '먹은 골드 대비 딜 기여 효율과 생존 효율을 반영합니다.',
    },
    {
      key: 'throwDeath',
      label: '데스관리',
      displayValue: `패널티 ${safeNumber(metrics.throwDeathPenalty, 0)}`,
      score: centeredScoreToPercent(scoring.survivalScore),
      description: '후반·오브젝트 직전 사망 등 패배 유발 데스를 감점합니다.',
    },
  ];
};

const buildFallbackComments = (match) => {
  const scoring = match?.scoring || {};
  const metrics = match?.metrics || {};
  const comments = [];

  if (match?.isRemake) {
    comments.push({
      type: 'warning',
      title: '다시하기 경기',
      text: '다시하기 경기는 점수 집계에서 제외됩니다.',
    });
    return comments;
  }

  if (!match?.isCountedGame) {
    comments.push({
      type: 'warning',
      title: '집계 제외 경기',
      text: '이 경기는 내부 점수 계산에 반영되지 않습니다.',
    });
    return comments;
  }

  if (safeNumber(scoring.finalDelta, 0) >= 24) {
    comments.push({
      type: 'good',
      title: '높은 점수 상승',
      text: `이번 경기는 ${formatSigned(scoring.finalDelta, 0)}점으로 크게 반영됐습니다.`,
    });
  } else if (safeNumber(scoring.finalDelta, 0) <= -24) {
    comments.push({
      type: 'bad',
      title: '큰 점수 하락',
      text: `이번 경기는 ${formatSigned(scoring.finalDelta, 0)}점으로 크게 하락했습니다.`,
    });
  }

  if (safeNumber(metrics.killParticipation, 0) >= 0.55) {
    comments.push({
      type: 'good',
      title: '팀 합류 우수',
      text: `킬 관여율이 ${Math.round(metrics.killParticipation * 100)}%로 높습니다.`,
    });
  } else if (safeNumber(metrics.killParticipation, 0) <= 0.30) {
    comments.push({
      type: 'warning',
      title: '합류 부족',
      text: '팀 전투 관여가 낮아 기여 점수가 제한됐습니다.',
    });
  }

  if (safeNumber(metrics.goldDiff15, 0) >= 400) {
    comments.push({
      type: 'good',
      title: '초반 우세',
      text: `15분 골드 차이가 ${formatSigned(metrics.goldDiff15, 0)}로 라인전 우위가 컸습니다.`,
    });
  } else if (safeNumber(metrics.goldDiff15, 0) <= -400) {
    comments.push({
      type: 'warning',
      title: '초반 열세',
      text: `15분 골드 차이가 ${formatSigned(metrics.goldDiff15, 0)}로 초반 손해가 있었습니다.`,
    });
  }

  if (safeNumber(metrics.objectiveParticipationScore, 0) >= 6) {
    comments.push({
      type: 'good',
      title: '오브젝트 기여',
      text: '드래곤·전령·타워 등 오브젝트 관여가 높게 반영됐습니다.',
    });
  }

  if (safeNumber(metrics.throwDeathPenalty, 0) >= 6) {
    comments.push({
      type: 'bad',
      title: '패배 유발 데스 주의',
      text: '후반 또는 오브젝트 타이밍 직전 사망이 감점으로 작용했습니다.',
    });
  }

  if (safeNumber(metrics.visionPerMinute, 0) >= 1.8) {
    comments.push({
      type: 'good',
      title: '시야 관리 양호',
      text: '분당 시야 점수가 높아 운영 기여가 반영됐습니다.',
    });
  }

  if (!comments.length) {
    comments.push({
      type: 'warning',
      title: '보통 수준 경기',
      text: '큰 가감점 요소 없이 기본 승패 점수 중심으로 반영된 경기입니다.',
    });
  }

  return comments.slice(0, 4);
};

const formatMetricValue = (metric) => {
  if (!metric) return '-';
  if (metric.displayValue) return metric.displayValue;
  if (metric.unit) return `${metric.value}${metric.unit}`;
  return String(metric.value ?? '-');
};

const getDeltaTextColor = (value) => {
  const n = Number(value);
  if (!Number.isFinite(n)) return 'text-gray-400';
  if (n > 0) return 'text-emerald-400';
  if (n < 0) return 'text-rose-400';
  return 'text-gray-400';
};

const MatchCard = ({ match, isExpanded, onToggle }) => {
  const [activeTab, setActiveTab] = useState('overview');
  const [analysisDetail, setAnalysisDetail] = useState(null);
  const [analysisError, setAnalysisError] = useState('');
  const [analysisErrorKey, setAnalysisErrorKey] = useState('');
  const [bucketMinutes, setBucketMinutes] = useState(3);
  const [timelineMetric, setTimelineMetric] = useState('gold');

  const resultType = match.resultType || (match.win ? 'WIN' : 'LOSS');
  const resultLabel = match.resultLabel || (match.win ? '승리' : '패배');
  const resultStyle = useMemo(() => getResultStyle(resultType), [resultType]);

  const analysisRequestKey = `${match.matchId || 'unknown'}:${bucketMinutes}`;
  const hasFreshAnalysis = Boolean(
    analysisDetail &&
      analysisDetail.matchId === match.matchId &&
      Number(analysisDetail.bucketMinutes || 3) === bucketMinutes,
  );

  useEffect(() => {
    if (!isExpanded) return;
    if (!match.matchId || !match.puuid) return;
    if (hasFreshAnalysis) return;

    let cancelled = false;

    axios
      .get(`/api/matches/${match.matchId}/analysis`, {
        params: {
          puuid: match.puuid,
          bucketMinutes,
        },
      })
      .then((response) => {
        if (cancelled) return;

        if (response.data?.error) {
          throw new Error(response.data.error);
        }

        setAnalysisError('');
        setAnalysisErrorKey('');
        setAnalysisDetail(response.data);
      })
      .catch((error) => {
        if (cancelled) return;
        setAnalysisError(error?.message || '상세 분석을 불러오지 못했습니다.');
        setAnalysisErrorKey(analysisRequestKey);
      });

    return () => {
      cancelled = true;
    };
  }, [isExpanded, match.matchId, match.puuid, bucketMinutes, hasFreshAnalysis, analysisRequestKey]);

  const activeAnalysisError = analysisErrorKey === analysisRequestKey ? analysisError : '';
  const isAnalysisLoading =
    isExpanded &&
    Boolean(match.matchId && match.puuid) &&
    !hasFreshAnalysis &&
    !activeAnalysisError;

  const overviewData = useMemo(
    () => buildOverviewFromAnalysis(analysisDetail, match.overview),
    [analysisDetail, match.overview],
  );

  const metricCards = useMemo(() => {
    const apiCards = analysisDetail?.metricCards ?? [];
    return apiCards.length > 0 ? apiCards : buildFallbackMetricCards(match);
  }, [analysisDetail, match]);

  const comments = useMemo(() => {
    const apiComments = analysisDetail?.coachingComments ?? [];
    return apiComments.length > 0 ? apiComments : buildFallbackComments(match);
  }, [analysisDetail, match]);

  const laneComparison = useMemo(() => analysisDetail?.laneComparison ?? null, [analysisDetail]);
  const timelineData = useMemo(
    () => buildTimelineData(analysisDetail?.timelineBuckets ?? []),
    [analysisDetail],
  );
  const radarData = useMemo(() => buildRadarData(metricCards, match), [metricCards, match]);
  const timelineMeta = useMemo(() => getTimelineMetricMeta(timelineMetric), [timelineMetric]);

  const isExcludedAnalysis = useMemo(() => {
    const sourceResult = analysisDetail?.resultType || resultType;
    return sourceResult === 'REMAKE' || sourceResult === 'INVALID';
  }, [analysisDetail, resultType]);

  const hasOverviewPlayers = useMemo(
    () =>
      Boolean(overviewData?.blueTeam?.players?.length || overviewData?.redTeam?.players?.length),
    [overviewData],
  );

  const finalDelta = safeNumber(match?.scoring?.finalDelta, 0);
  const performanceScore = safeNumber(match?.scoring?.performanceScore, 0);
  const perfIndex = safeNumber(match?.scoring?.perfIndex, 0);
  const matchScoreTier = safeString(match?.scoring?.scoreTier, '');

  return (
    <div className="flex flex-col rounded-xl border border-[#474973] bg-[#161B33] overflow-hidden">
      <div
        onClick={onToggle}
        className={`flex flex-col sm:flex-row cursor-pointer border-l-4 ${resultStyle.cardBorder} transition-colors`}
      >
        <div className="w-full sm:w-28 p-3 flex flex-col justify-center text-xs text-[#A69CAC] border-b sm:border-b-0 sm:border-r border-[#474973]/70">
          <div className={`font-bold mb-1 ${resultStyle.resultText}`}>{resultLabel}</div>
          <div className="mb-1">{match.gameType}</div>
          <div>{match.timeAgo}</div>
          <div className="border-t border-[#474973] my-1" />
          <div>{match.gameDuration}</div>
        </div>

        <div className="flex-1 p-3 flex items-center gap-4">
          <div className="flex items-center gap-2">
            <img
              src={`https://ddragon.leagueoflegends.com/cdn/14.3.1/img/champion/${match.summary.champion}.png`}
              alt={match.summary.champion}
              className="w-10 h-10 sm:w-12 sm:h-12 rounded-full border border-[#474973]"
            />
            <div className="flex flex-col gap-1 hidden sm:flex">
              <div className="w-4 h-4 sm:w-5 sm:h-5 bg-[#474973] rounded text-[8px] flex items-center justify-center border border-[#474973]">
                D
              </div>
              <div className="w-4 h-4 sm:w-5 sm:h-5 bg-[#474973] rounded text-[8px] flex items-center justify-center border border-[#474973]">
                F
              </div>
            </div>
          </div>

          <div className="flex flex-col items-center flex-1">
            <div className="font-bold text-[#F1DAC4] text-base sm:text-lg tracking-wide">
              {match.summary.kills} <span className="text-[#8B86A3] font-normal">/</span>{' '}
              <span className="text-[#A69CAC]">{match.summary.deaths}</span>{' '}
              <span className="text-[#8B86A3] font-normal">/</span> {match.summary.assists}
            </div>
            <div className="text-xs text-[#A69CAC] mt-0.5">
              {match.isCountedGame ? `${match.summary.kda} 평점` : '집계 제외 경기'}
            </div>
            <div className="mt-1 text-[10px] text-[#8B86A3]">
              {match.summary.position && match.summary.position !== 'UNKNOWN'
                ? `${match.summary.position} · ${match.summary.champion}`
                : match.summary.champion}
            </div>

            <div className="mt-2 flex flex-wrap justify-center gap-1.5">
              <span
                className={`px-2 py-0.5 rounded-full text-[10px] border ${
                  finalDelta > 0
                    ? 'bg-emerald-500/10 border-emerald-500/30 text-emerald-300'
                    : finalDelta < 0
                      ? 'bg-rose-500/10 border-rose-500/30 text-rose-300'
                      : 'bg-gray-500/10 border-gray-500/30 text-gray-300'
                }`}
              >
                점수 {formatSigned(finalDelta, 0)}
              </span>
              <span className="px-2 py-0.5 rounded-full text-[10px] border bg-purple-500/10 border-purple-500/30 text-purple-300">
                PERF {performanceScore}
              </span>
              {matchScoreTier && (
                <span className="px-2 py-0.5 rounded-full text-[10px] border bg-blue-500/10 border-blue-500/30 text-blue-300">
                  {matchScoreTier}
                </span>
              )}
            </div>
          </div>

          <div className="flex flex-col items-center justify-center w-16 sm:w-20 text-xs text-[#A69CAC]">
            <div>CS {match.summary.cs}</div>
            <div className="mt-1 text-[10px] text-[#8B86A3]">
              {match.summary.gold ? `${Math.round(match.summary.gold / 100) / 10}k 골드` : '-'}
            </div>
            <div className={`mt-1 text-[10px] ${getDeltaTextColor(perfIndex)}`}>
              PI {formatSigned(perfIndex, 2)}
            </div>
          </div>
        </div>

        <div className="w-full sm:w-auto p-3 flex items-center justify-center bg-black/10">
          <div className="grid grid-cols-4 sm:grid-cols-3 gap-1">
            {(match.summary.items || []).map((item, idx) => (
              <div
                key={idx}
                className={`w-6 h-6 rounded ${item === 0 ? 'bg-[#474973]/50' : 'bg-[#474973]'}`}
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

        <div className="hidden sm:flex w-10 items-center justify-center bg-[#0D0C1D]/35 text-[#8B86A3] hover:text-[#F1DAC4] transition-colors">
          {isExpanded ? <ChevronDown className="w-5 h-5" /> : <ChevronRight className="w-5 h-5" />}
        </div>
      </div>

      {isExpanded && (
        <div className="border-t border-[#474973] bg-[#0D0C1D] flex flex-col animate-in slide-in-from-top-2 duration-200">
          <div className="flex border-b border-[#474973] bg-[#161B33] overflow-x-auto">
            <button
              onClick={() => setActiveTab('overview')}
              className={`px-4 py-3 text-xs md:text-sm font-semibold transition-colors border-b-2 flex items-center gap-1 md:gap-2 whitespace-nowrap ${
                activeTab === 'overview'
                  ? 'border-[#F1DAC4] text-[#F1DAC4]'
                  : 'border-transparent text-[#A69CAC] hover:text-[#F1DAC4]'
              }`}
            >
              <Swords className="w-4 h-4" /> 종합 전적
            </button>
            <button
              onClick={() => setActiveTab('analysis')}
              className={`px-4 py-3 text-xs md:text-sm font-semibold transition-colors border-b-2 flex items-center gap-1 md:gap-2 whitespace-nowrap ${
                activeTab === 'analysis'
                  ? 'border-[#A69CAC] text-[#A69CAC]'
                  : 'border-transparent text-[#A69CAC] hover:text-[#F1DAC4]'
              }`}
            >
              <Activity className="w-4 h-4" /> 상세 분석
            </button>
          </div>

          {activeTab === 'overview' && (
            <div className="flex flex-col gap-3 p-3 md:p-4">
              {activeAnalysisError && (
                <div className="bg-[#161B33] rounded-xl border border-[#A69CAC]/40 p-4 text-sm text-[#C8BAD0] text-center">
                  {activeAnalysisError}
                </div>
              )}

              {!activeAnalysisError && isAnalysisLoading && !hasOverviewPlayers && (
                <div className="bg-[#161B33] rounded-xl border border-[#474973]/70 p-6 text-xs text-[#A69CAC] text-center">
                  실제 팀 전적 데이터를 불러오는 중입니다.
                </div>
              )}

              {!activeAnalysisError && !isAnalysisLoading && !hasOverviewPlayers && (
                <div className="bg-[#161B33] rounded-xl border border-[#474973]/70 p-6 text-xs text-[#8B86A3] text-center">
                  종합 전적 데이터가 아직 준비되지 않았습니다.
                </div>
              )}

              {hasOverviewPlayers && (
                <>
                  <div className={`rounded-lg ${resultStyle.blueBox}`}>
                    <div className={`px-3 py-2 ${resultStyle.blueHeader}`}>
                      <div className="flex flex-col gap-1 md:flex-row md:items-center md:justify-between">
                        <span className="text-xs font-bold">
                          {getTeamHeaderLabel(overviewData.blueTeam, '블루팀')}
                        </span>
                        <span className="text-[10px] md:text-xs text-[#A69CAC]">
                          총 킬: {overviewData.blueTeam.kills}
                          <span className="mx-2 text-[#8B86A3]">|</span>
                          총 데스: {overviewData.blueTeam.deaths}
                          <span className="mx-2 text-[#8B86A3]">|</span>
                          총 골드: {overviewData.blueTeam.gold || '0.0k'}
                        </span>
                      </div>
                      <div className="mt-1 text-[10px] md:text-xs text-[#8B86A3]">
                        오브젝트: {formatObjectiveSummary(overviewData.blueTeam.objectives)}
                      </div>
                    </div>
                    <div className="flex flex-col">
                      {overviewData.blueTeam.players.map((p) => (
                        <PlayerRow key={p.id} player={p} isBlueTeam />
                      ))}
                    </div>
                  </div>

                  <div className={`rounded-lg ${resultStyle.redBox}`}>
                    <div className={`px-3 py-2 ${resultStyle.redHeader}`}>
                      <div className="flex flex-col gap-1 md:flex-row md:items-center md:justify-between">
                        <span className="text-xs font-bold">
                          {getTeamHeaderLabel(overviewData.redTeam, '레드팀')}
                        </span>
                        <span className="text-[10px] md:text-xs text-[#A69CAC]">
                          총 킬: {overviewData.redTeam.kills}
                          <span className="mx-2 text-[#8B86A3]">|</span>
                          총 데스: {overviewData.redTeam.deaths}
                          <span className="mx-2 text-[#8B86A3]">|</span>
                          총 골드: {overviewData.redTeam.gold || '0.0k'}
                        </span>
                      </div>
                      <div className="mt-1 text-[10px] md:text-xs text-[#8B86A3]">
                        오브젝트: {formatObjectiveSummary(overviewData.redTeam.objectives)}
                      </div>
                    </div>
                    <div className="flex flex-col">
                      {overviewData.redTeam.players.map((p) => (
                        <PlayerRow key={p.id} player={p} isBlueTeam={false} />
                      ))}
                    </div>
                  </div>
                </>
              )}
            </div>
          )}

          {activeTab === 'analysis' && (
            <div className="flex flex-col p-3 md:p-4 gap-4">
              <div className="bg-[#161B33] px-4 py-3 flex items-center justify-between border border-[#474973] rounded-xl">
                <div className="flex items-center gap-2 text-xs text-[#F1DAC4]">
                  <Users className="w-4 h-4 text-[#A69CAC]" />
                  <span className="font-semibold">분석 기준: 내 플레이</span>
                </div>
                <div className="text-[11px] text-[#8B86A3]">
                  {analysisDetail?.summary?.championName || match.summary.champion}
                  {match.summary.position && match.summary.position !== 'UNKNOWN'
                    ? ` · ${match.summary.position}`
                    : ''}
                </div>
              </div>

              <div className="bg-[#161B33] px-4 py-3 border border-[#474973] rounded-xl flex flex-col gap-3 md:flex-row md:items-center md:justify-between">
                <div className="flex items-center gap-2 text-xs text-[#F1DAC4]">
                  <Activity className="w-4 h-4 text-[#F1DAC4]" />
                  <span className="font-semibold">버킷 단위</span>
                </div>
                <div className="flex gap-2">
                  {BUCKET_OPTIONS.map((option) => (
                    <button
                      key={option}
                      onClick={() => setBucketMinutes(option)}
                      className={`px-3 py-1.5 rounded-lg text-xs border transition-colors ${
                        bucketMinutes === option
                          ? 'bg-[#F1DAC4]/15 border-[#F1DAC4]/40 text-[#F1DAC4]'
                          : 'bg-[#0D0C1D] border-[#474973] text-[#A69CAC] hover:text-[#F1DAC4]'
                      }`}
                    >
                      {option}분
                    </button>
                  ))}
                </div>
              </div>

              {activeAnalysisError && (
                <div className="bg-[#161B33] rounded-xl border border-[#A69CAC]/40 p-6 text-sm text-[#C8BAD0] text-center">
                  {activeAnalysisError}
                </div>
              )}

              {!activeAnalysisError && isAnalysisLoading && !hasFreshAnalysis && (
                <div className="bg-[#161B33] rounded-xl border border-[#474973]/70 p-6 text-xs text-[#A69CAC] text-center">
                  상세 분석 데이터를 불러오는 중입니다.
                </div>
              )}

              {!activeAnalysisError && (
                <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                  <div className="bg-[#161B33] rounded-xl border border-[#474973]/70 p-3 flex flex-col items-center col-span-1">
                    <h3 className="text-xs font-bold text-[#F1DAC4] mb-2">플레이 성향 지표</h3>
                    {metricCards.length > 0 ? (
                      <div className="w-full h-40">
                        <ResponsiveContainer width="100%" height="100%">
                          <RadarChart cx="50%" cy="50%" outerRadius="65%" data={radarData}>
                            <PolarGrid stroke="#474973" />
                            <PolarAngleAxis dataKey="subject" tick={{ fill: '#F1DAC4', fontSize: 10 }} />
                            <Radar
                              name="지표"
                              dataKey="score"
                              stroke="#F1DAC4"
                              fill="#A69CAC"
                              fillOpacity={0.4}
                            />
                          </RadarChart>
                        </ResponsiveContainer>
                      </div>
                    ) : (
                      <div className="w-full h-40 flex items-center justify-center text-xs text-[#8B86A3]">
                        집계 제외 경기라 지표를 표시하지 않습니다.
                      </div>
                    )}
                  </div>

                  <div className="bg-[#161B33] rounded-xl border border-[#474973]/70 p-3 flex flex-col col-span-1">
                    <h3 className="text-xs font-bold text-[#F1DAC4] mb-3 flex items-center gap-1">
                      <Users className="w-3 h-3 text-[#F1DAC4]" /> 맞라인 상대 비교
                    </h3>
                    {laneComparison ? (
                      <div className="flex flex-col justify-center px-1">
                        <div className="flex justify-between items-center mb-3">
                          <div className="flex items-center gap-2">
                            <img
                              src={`https://ddragon.leagueoflegends.com/cdn/14.3.1/img/champion/${laneComparison.myChampionName}.png`}
                              className="w-7 h-7 rounded-full border border-[#F1DAC4]"
                              alt=""
                            />
                            <div className="hidden sm:flex flex-col">
                              <span className="text-[10px] font-bold text-[#F1DAC4]">
                                {laneComparison.myChampionName}
                              </span>
                              <span className="text-[10px] text-[#8B86A3]">
                                {normalizePosition(laneComparison.myPosition)}
                              </span>
                            </div>
                          </div>

                          <span className="text-[10px] text-[#8B86A3] font-bold px-2 py-0.5 bg-[#474973] rounded-full">
                            VS
                          </span>

                          <div className="flex items-center gap-2">
                            <div className="hidden sm:flex flex-col items-end">
                              <span className="text-[10px] font-bold text-[#A69CAC]">
                                {laneComparison.opponentChampionName}
                              </span>
                              <span className="text-[10px] text-[#8B86A3]">
                                {normalizePosition(laneComparison.opponentPosition)}
                              </span>
                            </div>
                            <img
                              src={`https://ddragon.leagueoflegends.com/cdn/14.3.1/img/champion/${laneComparison.opponentChampionName}.png`}
                              className="w-7 h-7 rounded-full border border-[#A69CAC]"
                              alt=""
                            />
                          </div>
                        </div>

                        <ComparisonBar
                          label="CS"
                          myValue={laneComparison.myCs || 0}
                          oppValue={laneComparison.opponentCs || 0}
                        />
                        <ComparisonBar
                          label="골드"
                          myValue={laneComparison.myGoldEarned || 0}
                          oppValue={laneComparison.opponentGoldEarned || 0}
                        />
                        <ComparisonBar
                          label="딜량"
                          myValue={laneComparison.myDamageToChampions || 0}
                          oppValue={laneComparison.opponentDamageToChampions || 0}
                        />

                        <div className="mt-2 text-[11px] text-[#8B86A3]">
                          차이값:
                          <span className="ml-2 text-[#E8D8C8]">
                            CS {laneComparison.csDiff >= 0 ? '+' : ''}
                            {laneComparison.csDiff}
                          </span>
                          <span className="ml-2 text-[#F1DAC4]">
                            골드 {laneComparison.goldDiff >= 0 ? '+' : ''}
                            {laneComparison.goldDiff.toLocaleString()}
                          </span>
                          <span className="ml-2 text-[#D6C2DF]">
                            딜량 {laneComparison.damageDiff >= 0 ? '+' : ''}
                            {laneComparison.damageDiff.toLocaleString()}
                          </span>
                        </div>
                      </div>
                    ) : (
                      <div className="text-xs text-[#8B86A3] text-center mt-8">상대를 찾을 수 없습니다.</div>
                    )}
                  </div>

                  <div className="bg-[#161B33] rounded-xl border border-[#474973]/70 p-3 md:col-span-2">
                    <h3 className="text-xs font-bold text-[#F1DAC4] mb-3">핵심 지표 카드</h3>
                    {metricCards.length > 0 ? (
                      <div className="grid grid-cols-2 md:grid-cols-4 gap-2">
                        {metricCards.map((metric) => (
                          <div
                            key={metric.key}
                            className="rounded-lg border border-[#474973] bg-[#161B33] p-3"
                            title={metric.description || metric.label}
                          >
                            <div className="text-[10px] text-[#A69CAC]">{metric.label}</div>
                            <div className="text-sm font-bold text-white mt-1">{formatMetricValue(metric)}</div>
                            <div className="text-[10px] text-[#D6C2DF] mt-1">점수 {metric.score}</div>
                            <div className="text-[10px] text-[#8B86A3] mt-1 leading-tight">
                              {metric.description}
                            </div>
                          </div>
                        ))}
                      </div>
                    ) : (
                      <div className="text-xs text-[#8B86A3] text-center py-4">
                        집계 제외 경기라 성과 카드를 표시하지 않습니다.
                      </div>
                    )}
                  </div>

                  <div className="bg-[#161B33] rounded-xl border border-[#474973]/70 p-3 md:col-span-2">
                    <div className="flex flex-col gap-3 md:flex-row md:items-center md:justify-between mb-3">
                      <h3 className="text-xs font-bold text-[#F1DAC4]">시간대별 성장 추이</h3>
                      <div className="flex flex-wrap gap-2">
                        {TIMELINE_METRIC_OPTIONS.map((option) => (
                          <button
                            key={option.key}
                            onClick={() => setTimelineMetric(option.key)}
                            className={`px-2.5 py-1 rounded-lg text-[11px] border transition-colors ${
                              timelineMetric === option.key
                                ? 'bg-[#A69CAC]/20 border-[#A69CAC]/45 text-[#F1DAC4]'
                                : 'bg-[#0D0C1D] border-[#474973] text-[#A69CAC] hover:text-[#F1DAC4]'
                            }`}
                          >
                            {option.label}
                          </button>
                        ))}
                      </div>
                    </div>

                    <div className="mb-3 text-[11px] text-[#8B86A3]">
                      파란색은 내 수치, 빨간색은 맞라인 상대 수치입니다. 시간대별 우열 변화를 2라인으로 비교합니다.
                    </div>

                    {timelineData.length > 0 ? (
                      <div className="w-full h-52">
                        <ResponsiveContainer width="100%" height="100%">
                          <LineChart
                            data={timelineData}
                            margin={{ top: 5, right: 10, bottom: 0, left: -15 }}
                          >
                            <CartesianGrid strokeDasharray="3 3" stroke="#474973" vertical={false} />
                            <XAxis
                              dataKey="time"
                              stroke="#A69CAC"
                              tick={{ fill: '#A69CAC', fontSize: 10 }}
                              tickFormatter={(val) => `${val}분`}
                            />
                            <YAxis
                              stroke="#F1DAC4"
                              tick={{ fill: '#F1DAC4', fontSize: 10 }}
                            />
                            <RechartsTooltip
                              formatter={(value) =>
                                formatTooltipValue(value, timelineMeta.unit, timelineMeta.formatter)
                              }
                              labelFormatter={(label) => `${label}분`}
                              contentStyle={{
                                backgroundColor: '#161B33',
                                borderColor: '#474973',
                                borderRadius: '8px',
                                fontSize: '11px',
                              }}
                            />
                            <Legend
                              verticalAlign="top"
                              height={24}
                              wrapperStyle={{ fontSize: '10px', color: '#F1DAC4' }}
                            />
                            <Line
                              name={`내 ${timelineMeta.label}`}
                              type="monotone"
                              dataKey={timelineMeta.myDataKey}
                              stroke="#F1DAC4"
                              strokeWidth={3}
                              dot={{ r: 2.5 }}
                              activeDot={{ r: 5 }}
                            />
                            <Line
                              name={`상대 ${timelineMeta.label}`}
                              type="monotone"
                              dataKey={timelineMeta.opponentDataKey}
                              stroke="#A69CAC"
                              strokeWidth={2}
                              strokeDasharray="6 4"
                              dot={{ r: 2 }}
                              activeDot={{ r: 4 }}
                            />
                          </LineChart>
                        </ResponsiveContainer>
                      </div>
                    ) : (
                      <div className="w-full h-44 flex items-center justify-center text-xs text-[#8B86A3]">
                        {isExcludedAnalysis
                          ? '집계 제외 경기라 시간대별 성장 추이를 표시하지 않습니다.'
                          : '상세 분석 API 응답이 오면 시간대별 추이가 표시됩니다.'}
                      </div>
                    )}
                  </div>

                  <div className="bg-[#161B33] rounded-xl border border-[#474973]/70 p-3 md:col-span-2">
                    <h3 className="text-xs font-bold text-[#F1DAC4] mb-2 flex items-center gap-1">
                      <Target className="w-3 h-3 text-[#A69CAC]" /> 시스템 코멘트
                    </h3>
                    <div className="grid grid-cols-1 sm:grid-cols-2 gap-2">
                      {comments.map((item, idx) => (
                        <div
                          key={idx}
                          className={`p-2 rounded-lg border text-[11px] flex items-start gap-2 ${
                            item.type === 'good'
                              ? 'bg-[#474973]/40 border-[#F1DAC4]/30 text-[#F1DAC4]'
                              : item.type === 'warning'
                                ? 'bg-[#474973]/45 border-[#F1DAC4]/30 text-[#F1DAC4]'
                                : 'bg-[#161B33]/60 border-[#A69CAC]/35 text-[#F1DAC4]'
                          }`}
                        >
                          {item.type === 'good' && (
                            <ThumbsUp className="w-3 h-3 text-[#F1DAC4] mt-0.5 flex-shrink-0" />
                          )}
                          {item.type === 'warning' && (
                            <AlertCircle className="w-3 h-3 text-[#F1DAC4] mt-0.5 flex-shrink-0" />
                          )}
                          {item.type === 'bad' && (
                            <AlertCircle className="w-3 h-3 text-[#A69CAC] mt-0.5 flex-shrink-0" />
                          )}
                          <div className="leading-tight">
                            {item.title && <div className="font-semibold mb-0.5">{item.title}</div>}
                            <div>{item.text}</div>
                          </div>
                        </div>
                      ))}
                      {!comments.length && (
                        <div className="text-xs text-[#8B86A3]">표시할 코멘트가 없습니다.</div>
                      )}
                    </div>
                  </div>
                </div>
              )}

              {!activeAnalysisError && !isAnalysisLoading && !analysisDetail && (
                <div className="text-xs text-[#8B86A3] text-center py-4">
                  분석 데이터가 준비되면 자동으로 표시됩니다.
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
