import { MOCK_DATA, QUEUE_LABEL } from '../data/mmrMockData';

const RESULT_META = {
  WIN: {
    label: '승리',
    shortLabel: '승리',
  },
  LOSS: {
    label: '패배',
    shortLabel: '패배',
  },
  REMAKE: {
    label: '다시하기',
    shortLabel: '다시하기',
  },
  INVALID: {
    label: '제외',
    shortLabel: '제외',
  },
};

const safeNumber = (value, fallback = 0) => {
  const n = Number(value);
  return Number.isFinite(n) ? n : fallback;
};

const safeString = (value, fallback = '') => {
  if (value === null || value === undefined) return fallback;
  const text = String(value);
  return text.trim() ? text : fallback;
};

const toFixedString = (value, digits = 1, fallback = '0.0') => {
  const n = Number(value);
  return Number.isFinite(n) ? n.toFixed(digits) : fallback;
};

const normalizeResultType = (resultType, win) => {
  if (typeof resultType === 'string' && RESULT_META[resultType]) {
    return resultType;
  }
  return win ? 'WIN' : 'LOSS';
};

const isCountedResult = (resultType) => resultType === 'WIN' || resultType === 'LOSS';
const isWinResult = (resultType) => resultType === 'WIN';
const getResultMeta = (resultType) => RESULT_META[resultType] || RESULT_META.INVALID;

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

const normalizeQueue = (queue) => {
  return queue === 'flex' ? 'flex' : 'solo';
};

const queueLabel = (queue) => {
  return queue === 'flex' ? '자유랭크' : '솔로랭크';
};

const inferAvailableQueuesFromResponse = (apiData) => {
  const result = [];

  if (apiData?.queues?.solo || apiData?.soloMatchDetails || apiData?.soloScoreResult) {
    result.push('solo');
  }

  if (apiData?.queues?.flex || apiData?.flexMatchDetails || apiData?.flexScoreResult) {
    result.push('flex');
  }

  return result.length > 0 ? result : ['solo', 'flex'];
};

const pickActiveQueue = (preferredQueue, availableQueues, apiData) => {
  const normalized = normalizeQueue(preferredQueue);

  if (availableQueues.includes(normalized)) {
    return normalized;
  }

  if (apiData?.queues?.solo || apiData?.soloMatchDetails) return 'solo';
  if (apiData?.queues?.flex || apiData?.flexMatchDetails) return 'flex';

  return availableQueues[0] || 'solo';
};

const buildEmptyOverview = (resultType) => {
  const counted = isCountedResult(resultType);

  return {
    blueTeam: {
      isWin: resultType === 'WIN',
      hasWinner: counted,
      kills: 0,
      deaths: 0,
      gold: '0.0k',
      objectives: {},
      players: [],
    },
    redTeam: {
      isWin: resultType === 'LOSS',
      hasWinner: counted,
      kills: 0,
      deaths: 0,
      gold: '0.0k',
      objectives: {},
      players: [],
    },
  };
};

export const formatDuration = (minutes) => {
  const safeMinutes = safeNumber(minutes, 0);
  const m = Math.max(0, Math.floor(safeMinutes));
  return `${String(m).padStart(2, '0')}:00`;
};

export const formatTimeAgo = (timestamp) => {
  if (!timestamp) return '방금 전';

  const diffMs = Date.now() - safeNumber(timestamp, 0);
  if (!Number.isFinite(diffMs) || diffMs < 0) return '방금 전';

  const diffMin = Math.floor(diffMs / 60000);
  if (diffMin < 60) return `${Math.max(diffMin, 1)}분 전`;

  const diffHour = Math.floor(diffMin / 60);
  if (diffHour < 24) return `${diffHour}시간 전`;

  const diffDay = Math.floor(diffHour / 24);
  return `${diffDay}일 전`;
};

const buildRecentChampions = (matchDetails = []) => {
  const stats = new Map();

  matchDetails
    .filter((match) => isCountedResult(normalizeResultType(match?.resultType, match?.win)))
    .forEach((match) => {
      const key = match?.championName || 'Unknown';

      if (!stats.has(key)) {
        stats.set(key, {
          name: key,
          games: 0,
          wins: 0,
          kills: 0,
          deaths: 0,
          assists: 0,
        });
      }

      const row = stats.get(key);
      row.games += 1;

      if (isWinResult(normalizeResultType(match?.resultType, match?.win))) {
        row.wins += 1;
      }

      row.kills += safeNumber(match?.kills, 0);
      row.deaths += safeNumber(match?.deaths, 0);
      row.assists += safeNumber(match?.assists, 0);
    });

  return Array.from(stats.values())
    .sort((a, b) => b.games - a.games)
    .slice(0, 3)
    .map((row) => {
      const winRate = row.games > 0 ? Math.round((row.wins * 100) / row.games) : 0;
      const kdaValue =
        row.deaths === 0 ? row.kills + row.assists : (row.kills + row.assists) / row.deaths;

      return {
        name: row.name,
        games: row.games,
        winRate,
        kda: kdaValue.toFixed(1),
      };
    });
};

const buildVisibleSummary = (matchDetails = []) => {
  const countedMatches = matchDetails.filter((match) =>
    isCountedResult(normalizeResultType(match?.resultType, match?.win))
  );

  const wins = countedMatches.filter((match) =>
    isWinResult(normalizeResultType(match?.resultType, match?.win))
  ).length;

  const losses = countedMatches.length - wins;
  const remakes = matchDetails.filter(
    (match) => normalizeResultType(match?.resultType, match?.win) === 'REMAKE'
  ).length;
  const invalid = matchDetails.filter(
    (match) => normalizeResultType(match?.resultType, match?.win) === 'INVALID'
  ).length;

  const totalKills = countedMatches.reduce((sum, match) => sum + safeNumber(match?.kills, 0), 0);
  const totalDeaths = countedMatches.reduce((sum, match) => sum + safeNumber(match?.deaths, 0), 0);
  const totalAssists = countedMatches.reduce((sum, match) => sum + safeNumber(match?.assists, 0), 0);

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
    totalGames: matchDetails.length,
    winRate,
    kda: kdaValue.toFixed(2),
  };
};

const buildMatchScoring = (match) => ({
  baseDelta: safeNumber(match?.baseDelta, null),
  performanceDelta: safeNumber(match?.performanceDelta, null),
  finalDelta: safeNumber(match?.finalDelta, null),
  performanceScore: safeNumber(match?.performanceScore, null),
  perfIndex: safeNumber(match?.perfIndex, null),
  growthScore: safeNumber(match?.growthScore, null),
  teamplayScore: safeNumber(match?.teamplayScore, null),
  efficiencyScore: safeNumber(match?.efficiencyScore, null),
  survivalScore: safeNumber(match?.survivalScore, null),
});

const buildMatchMetrics = (match) => ({
  killParticipation: safeNumber(match?.killParticipation, 0),
  damageShare: safeNumber(match?.damageShare, 0),
  damageConversion: safeNumber(match?.damageConversion, 0),
  visionPerMinute: safeNumber(match?.visionPerMinute, 0),
  goldPerMinute: safeNumber(match?.goldPerMinute, 0),
  csPerMinute: safeNumber(match?.csPerMinute, 0),
  timeAliveRatio: safeNumber(match?.timeAliveRatio, 0),

  goldDiff15: safeNumber(match?.goldDiff15, 0),
  csDiff15: safeNumber(match?.csDiff15, 0),
  xpDiff15: safeNumber(match?.xpDiff15, 0),

  objectiveParticipationScore: safeNumber(match?.objectiveParticipationScore, 0),
  throwDeathPenalty: safeNumber(match?.throwDeathPenalty, 0),

  teamKills: safeNumber(match?.teamKills, 0),
  teamGoldEarned: safeNumber(match?.teamGoldEarned, 0),
  teamDamageToChampions: safeNumber(match?.teamDamageToChampions, 0),

  visionScore: safeNumber(match?.visionScore, 0),
  controlWardsPlaced: safeNumber(match?.controlWardsPlaced, 0),
  wardsPlaced: safeNumber(match?.wardsPlaced, 0),
  wardsKilled: safeNumber(match?.wardsKilled, 0),
  totalTimeSpentDead: safeNumber(match?.totalTimeSpentDead, 0),

  damageToChampions: safeNumber(match?.damageToChampions, 0),
  damageToObjectives: safeNumber(match?.damageToObjectives, 0),
  damageToTurrets: safeNumber(match?.damageToTurrets, 0),

  championLevel: safeNumber(match?.championLevel, 0),
  leaver: Boolean(match?.leaver),
});

const buildScoreDetails = (scoreResult = {}) => {
  const totalScoreRaw =
    scoreResult?.totalScore ?? scoreResult?.currentScore ?? scoreResult?.baseScore ?? 0;

  return {
    totalScore: toFixedString(totalScoreRaw, 1, '0.0'),
    currentScore: safeNumber(scoreResult?.currentScore, 0),
    baseScore: safeNumber(scoreResult?.baseScore, 0),
    grade: safeString(scoreResult?.grade, 'C'),
    scoreTier: safeString(scoreResult?.scoreTier, ''),
    sampleCount: safeNumber(scoreResult?.sampleCount, 0),
    countedGames: safeNumber(scoreResult?.countedGames, 0),
    excludedCount: safeNumber(scoreResult?.excludedCount, 0),
    remakeCount: safeNumber(scoreResult?.remakeCount, 0),
    invalidCount: safeNumber(scoreResult?.invalidCount, 0),

    averageDelta: safeNumber(scoreResult?.averageDelta, 0),
    averagePerformance: safeNumber(scoreResult?.averagePerformance, 0),
    averagePerfIndex: safeNumber(scoreResult?.averagePerfIndex, 0),
    averageBaseDelta: safeNumber(scoreResult?.averageBaseDelta, 0),
    averagePerformanceDelta: safeNumber(scoreResult?.averagePerformanceDelta, 0),

    scoreHistory: Array.isArray(scoreResult?.scoreHistory) ? scoreResult.scoreHistory : [],
    scoreDeltaHistory: Array.isArray(scoreResult?.scoreDeltaHistory)
      ? scoreResult.scoreDeltaHistory
      : [],
    performanceHistory: Array.isArray(scoreResult?.performanceHistory)
      ? scoreResult.performanceHistory
      : [],
    perfIndexHistory: Array.isArray(scoreResult?.perfIndexHistory)
      ? scoreResult.perfIndexHistory
      : [],
    baseDeltaHistory: Array.isArray(scoreResult?.baseDeltaHistory)
      ? scoreResult.baseDeltaHistory
      : [],
    performanceDeltaHistory: Array.isArray(scoreResult?.performanceDeltaHistory)
      ? scoreResult.performanceDeltaHistory
      : [],

    roleStats: scoreResult?.roleStats || {},
  };
};

export const toUiMatch = (match, summonerName, index, puuid) => {
  const resultType = normalizeResultType(match?.resultType, match?.win);
  const resultMeta = getResultMeta(resultType);

  const kills = safeNumber(match?.kills, 0);
  const deaths = safeNumber(match?.deaths, 0);
  const assists = safeNumber(match?.assists, 0);

  const scoring = buildMatchScoring(match);
  const metrics = buildMatchMetrics(match);

  return {
    id: `${match?.matchId || match?.gameEndTimeStamp || Date.now()}_${index}`,
    matchId: match?.matchId,
    puuid,
    summonerName,
    participantId: safeNumber(match?.participantId, 0),
    myParticipantId: safeNumber(match?.participantId, 1),
    riotId: safeString(match?.riotId, summonerName),
    win: resultType === 'WIN',
    loss: resultType === 'LOSS',
    isRemake: resultType === 'REMAKE',
    isCountedGame: isCountedResult(resultType),
    isInvalid: resultType === 'INVALID',
    leaver: Boolean(match?.leaver),

    resultType,
    resultLabel: safeString(match?.displayResult, resultMeta.label),
    resultShortLabel: resultMeta.shortLabel,

    gameDuration: formatDuration(match?.gameDurationMinutes),
    gameDurationMinutes: safeNumber(match?.gameDurationMinutes, 0),
    gameType: QUEUE_LABEL[match?.queueId] || `큐 ${match?.queueId || '-'}`,
    queueId: safeNumber(match?.queueId, 0),
    timeAgo: formatTimeAgo(match?.gameEndTimeStamp),
    gameEndTimeStamp: safeNumber(match?.gameEndTimeStamp, 0),

    teamPosition: normalizePosition(match?.teamPosition),

    summary: {
      champion: safeString(match?.championName, 'Ahri'),
      position: normalizePosition(match?.teamPosition),
      kills,
      deaths,
      assists,
      kda:
        deaths === 0
          ? String(kills + assists)
          : ((kills + assists) / Math.max(deaths, 1)).toFixed(2),
      cs: safeNumber(match?.totalCs, 0),
      gold: safeNumber(match?.goldEarned, 0),
      items: Array.isArray(match?.items) ? match.items.slice(0, 6) : [],
      championLevel: safeNumber(match?.championLevel, 0),
      visionScore: safeNumber(match?.visionScore, 0),
      damageToChampions: safeNumber(match?.damageToChampions, 0),
    },

    scoring,
    metrics,

    overview: buildEmptyOverview(resultType),
    teamMembers: Array.isArray(match?.teamMembers) ? match.teamMembers : [],
    teamChamps: Array.isArray(match?.teamChamps) ? match.teamChamps : [],
  };
};

const resolveSummaryForUi = (queueData, allMatches, meta = {}) => {
  const visibleSummary = buildVisibleSummary(allMatches);
  const apiSummary = queueData?.summary || {};
  const scoreResult = queueData?.scoreResult || {};

  return {
    wins: safeNumber(apiSummary.wins ?? visibleSummary.wins, 0),
    losses: safeNumber(apiSummary.losses ?? visibleSummary.losses, 0),
    remakes: safeNumber(apiSummary.remakes ?? visibleSummary.remakes, 0),
    invalid: safeNumber(apiSummary.invalid ?? visibleSummary.invalid, 0),
    countedGames: safeNumber(apiSummary.countedGames ?? visibleSummary.countedGames, 0),
    totalGames: safeNumber(apiSummary.totalGames ?? visibleSummary.totalGames, 0),
    winRate: safeNumber(apiSummary.winRate ?? visibleSummary.winRate, 0),
    kda: safeString(apiSummary.kda ?? visibleSummary.kda, '0.00'),

    displayMatchCount: safeNumber(
      apiSummary.displayMatchCount ?? meta.displayMatchCount ?? allMatches.length,
      allMatches.length
    ),
    scoreSampleCount: safeNumber(
      apiSummary.scoreSampleCount ?? meta.scoreSampleCount ?? scoreResult.sampleCount ?? allMatches.length,
      allMatches.length
    ),
    scoreCountedGames: safeNumber(
      apiSummary.scoreCountedGames ?? scoreResult.countedGames ?? visibleSummary.countedGames,
      visibleSummary.countedGames
    ),
    excludedGames: safeNumber(
      apiSummary.excludedGames ?? scoreResult.excludedCount,
      0
    ),

    recentChampions: buildRecentChampions(allMatches),
  };
};

const resolveQueueData = (apiData, preferredQueue) => {
  const activeQueue = normalizeQueue(preferredQueue);

  if (apiData?.queues) {
    return (
      apiData.queues[activeQueue] ||
      apiData.queues.solo ||
      apiData.queues.flex || {
        matchDetails: [],
        scoreResult: {},
        summary: {},
        rankInfo: null,
        counts: {},
      }
    );
  }

  return activeQueue === 'flex'
    ? {
        matchDetails: apiData?.flexMatchDetails || [],
        scoreResult: apiData?.flexScoreResult || {},
        summary: apiData?.flexSummary || {},
        rankInfo: apiData?.summoner?.flexRank || null,
        counts: apiData?.flexCounts || {},
      }
    : {
        matchDetails: apiData?.soloMatchDetails || [],
        scoreResult: apiData?.soloScoreResult || {},
        summary: apiData?.soloSummary || {},
        rankInfo: apiData?.summoner?.soloRank || null,
        counts: apiData?.soloCounts || {},
      };
};

export const mapApiToUiData = (apiData, preferredQueue = 'solo') => {
  if (!apiData?.summoner) {
    return MOCK_DATA;
  }

  const availableQueues = Array.isArray(apiData?.meta?.availableQueues)
    ? apiData.meta.availableQueues.map(normalizeQueue)
    : inferAvailableQueuesFromResponse(apiData);

  const activeQueue = pickActiveQueue(preferredQueue, availableQueues, apiData);
  const activeQueueLabel = queueLabel(activeQueue);

  const queueData = resolveQueueData(apiData, activeQueue);
  const rawMatches = Array.isArray(queueData?.matchDetails) ? queueData.matchDetails : [];
  const summonerName = safeString(apiData?.summoner?.name, 'Unknown');
  const scoreDetails = buildScoreDetails(queueData?.scoreResult || {});
  const meta = {
    ...(apiData?.meta || {}),
    availableQueues,
    activeQueue,
    activeQueueLabel,
    displayMatchCount: safeNumber(apiData?.meta?.displayMatchCount, 5),
    scoreSampleCount: safeNumber(apiData?.meta?.scoreSampleCount, 20),
    analysisMode: safeString(apiData?.meta?.analysisMode, 'light'),
  };
  const summary = resolveSummaryForUi(queueData, rawMatches, meta);

  return {
    summoner: {
      name: summonerName,
      puuid: safeString(apiData?.summoner?.puuid, ''),
      summonerLevel: safeNumber(apiData?.summoner?.summonerLevel, 0),
      profileIconId: safeNumber(
        apiData?.summoner?.profileIconId,
        MOCK_DATA.summoner.profileIconId
      ),
      scoreDetails,
      soloRank: apiData?.summoner?.soloRank || null,
      flexRank: apiData?.summoner?.flexRank || null,
      currentRank:
        activeQueue === 'flex'
          ? apiData?.summoner?.flexRank || null
          : apiData?.summoner?.soloRank || null,
    },
    meta,
    activeQueue,
    activeQueueLabel,
    availableQueues,
    summary,
    matches: rawMatches.map((match, index) =>
      toUiMatch(match, summonerName, index, apiData?.summoner?.puuid)
    ),
  };
};