import { MOCK_DATA, QUEUE_LABEL } from '../data/mmrMockData';

const RECENT_MATCH_LIMIT = 2;

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

const buildEmptyOverview = (resultType) => {
  const counted = isCountedResult(resultType);

  return {
    blueTeam: {
      isWin: resultType === 'WIN',
      hasWinner: counted,
      kills: 0,
      gold: '0.0k',
      players: [],
    },
    redTeam: {
      isWin: resultType === 'LOSS',
      hasWinner: counted,
      kills: 0,
      gold: '0.0k',
      players: [],
    },
  };
};

export const formatDuration = (minutes) => {
  const safeMinutes = Number.isFinite(minutes) ? minutes : 0;
  const m = Math.max(0, Math.floor(safeMinutes));
  return `${String(m).padStart(2, '0')}:00`;
};

export const formatTimeAgo = (timestamp) => {
  if (!timestamp) return '방금 전';
  const diffMs = Date.now() - Number(timestamp);
  if (!Number.isFinite(diffMs) || diffMs < 0) return '방금 전';

  const diffMin = Math.floor(diffMs / 60000);
  if (diffMin < 60) return `${Math.max(diffMin, 1)}분 전`;

  const diffHour = Math.floor(diffMin / 60);
  if (diffHour < 24) return `${diffHour}시간 전`;

  const diffDay = Math.floor(diffHour / 24);
  return `${diffDay}일 전`;
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

  const totalKills = countedMatches.reduce((sum, match) => sum + (match?.kills || 0), 0);
  const totalDeaths = countedMatches.reduce((sum, match) => sum + (match?.deaths || 0), 0);
  const totalAssists = countedMatches.reduce((sum, match) => sum + (match?.assists || 0), 0);

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

export const buildRecentChampions = (matchDetails = []) => {
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

      row.kills += match?.kills || 0;
      row.deaths += match?.deaths || 0;
      row.assists += match?.assists || 0;
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

export const toUiMatch = (match, summonerName, index, puuid) => {
  const resultType = normalizeResultType(match?.resultType, match?.win);
  const resultMeta = getResultMeta(resultType);

  const kills = match?.kills || 0;
  const deaths = match?.deaths || 0;
  const assists = match?.assists || 0;

  return {
    id: `${match?.matchId || match?.gameEndTimeStamp || Date.now()}_${index}`,
    matchId: match?.matchId,
    puuid,
    summonerName,
    win: resultType === 'WIN',
    isRemake: resultType === 'REMAKE',
    isCountedGame: isCountedResult(resultType),
    resultType,
    resultLabel: match?.displayResult || resultMeta.label,
    gameDuration: formatDuration(match?.gameDurationMinutes),
    gameType: QUEUE_LABEL[match?.queueId] || `큐 ${match?.queueId || '-'}`,
    timeAgo: formatTimeAgo(match?.gameEndTimeStamp),
    myParticipantId: 1,
    teamPosition: normalizePosition(match?.teamPosition),
    summary: {
      champion: match?.championName || 'Ahri',
      position: normalizePosition(match?.teamPosition),
      kills,
      deaths,
      assists,
      kda:
        deaths === 0
          ? String(kills + assists)
          : ((kills + assists) / Math.max(deaths, 1)).toFixed(2),
      cs: match?.totalCs || 0,
      gold: match?.goldEarned || 0,
      items: (match?.items || []).slice(0, 6),
    },
    overview: buildEmptyOverview(resultType),
    teamMembers: [],
  };
};

const resolveSummaryForUi = (queueData, visibleMatches) => {
  const visibleSummary = buildVisibleSummary(visibleMatches);
  const apiSummary = queueData?.summary || {};
  const scoreResult = queueData?.scoreResult || {};

  return {
    wins: Number(apiSummary.wins ?? visibleSummary.wins ?? 0),
    losses: Number(apiSummary.losses ?? visibleSummary.losses ?? 0),
    remakes: Number(apiSummary.remakes ?? visibleSummary.remakes ?? 0),
    invalid: Number(apiSummary.invalid ?? visibleSummary.invalid ?? 0),
    countedGames: Number(apiSummary.countedGames ?? visibleSummary.countedGames ?? 0),
    totalGames: Number(apiSummary.totalGames ?? visibleSummary.totalGames ?? 0),
    winRate: Number(apiSummary.winRate ?? visibleSummary.winRate ?? 0),
    kda: String(apiSummary.kda ?? visibleSummary.kda ?? '0.00'),
    displayMatchCount: Number(apiSummary.displayMatchCount ?? visibleMatches.length ?? 0),
    scoreSampleCount: Number(
      apiSummary.scoreSampleCount ?? scoreResult.sampleCount ?? visibleMatches.length ?? 0
    ),
    scoreCountedGames: Number(
      apiSummary.scoreCountedGames ?? scoreResult.countedGames ?? visibleSummary.countedGames ?? 0
    ),
    excludedGames: Number(
      apiSummary.excludedGames ?? scoreResult.excludedCount ?? 0
    ),
    recentChampions: buildRecentChampions(visibleMatches),
  };
};

const resolveQueueData = (apiData, preferredQueue) => {
  if (apiData?.queues) {
    return apiData.queues[preferredQueue] || apiData.queues.solo || apiData.queues.flex;
  }

  const legacyQueueData =
    preferredQueue === 'flex'
      ? {
          matchDetails: apiData?.flexMatchDetails || [],
          scoreResult: apiData?.flexScoreResult || {},
          summary: apiData?.flexSummary || {},
        }
      : {
          matchDetails: apiData?.soloMatchDetails || [],
          scoreResult: apiData?.soloScoreResult || {},
          summary: apiData?.soloSummary || {},
        };

  return legacyQueueData;
};

export const mapApiToUiData = (apiData, preferredQueue = 'solo') => {
  if (!apiData?.summoner) {
    return MOCK_DATA;
  }

  const queueData = resolveQueueData(apiData, preferredQueue);
  const rawMatches = queueData?.matchDetails || [];
  const visibleMatches = rawMatches.slice(0, RECENT_MATCH_LIMIT);
  const summary = resolveSummaryForUi(queueData, visibleMatches);
  const summonerName = apiData?.summoner?.name || 'Unknown';

  return {
    summoner: {
      name: summonerName,
      summonerLevel: apiData?.summoner?.summonerLevel || 0,
      profileIconId: apiData?.summoner?.profileIconId || MOCK_DATA.summoner.profileIconId,
      scoreDetails: {
        totalScore: Number(
          queueData?.scoreResult?.totalScore ||
          queueData?.scoreResult?.currentScore ||
          0
        ).toFixed(1),
        grade: queueData?.scoreResult?.grade || 'C',
        sampleCount: Number(
          queueData?.scoreResult?.sampleCount || summary.scoreSampleCount || 0
        ),
        countedGames: Number(
          queueData?.scoreResult?.countedGames || summary.scoreCountedGames || 0
        ),
        excludedCount: Number(
          queueData?.scoreResult?.excludedCount || summary.excludedGames || 0
        ),
      },
    },
    summary,
    matches: visibleMatches.map((match, index) =>
      toUiMatch(match, summonerName, index, apiData?.summoner?.puuid)
    ),
  };
};