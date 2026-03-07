import { MOCK_DATA, POSITION_FALLBACK, QUEUE_LABEL } from '../data/mmrMockData';

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

const clamp = (value, min, max) => Math.min(max, Math.max(min, value));

const hashSeed = (text) => {
  const source = String(text || 'seed');
  let hash = 0;
  for (let i = 0; i < source.length; i += 1) {
    hash = (hash * 31 + source.charCodeAt(i)) % 2147483647;
  }
  return hash || 1;
};

const seededRand = (seed, offset = 0) => {
  const x = Math.sin(seed * 12.9898 + (offset + 1) * 78.233) * 43758.5453;
  return x - Math.floor(x);
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

const buildTimelinePoints = (durationMinutes) => {
  const safeDuration = clamp(Math.round(durationMinutes || 25), 15, 45);
  const points = [0];

  for (let minute = 3; minute < safeDuration; minute += 3) {
    points.push(minute);
  }

  if (points[points.length - 1] !== safeDuration) {
    points.push(safeDuration);
  }

  return points;
};

const buildGrowthTimeline = ({
  finalGold,
  finalCs,
  durationMinutes,
  seed,
  isJungle,
}) => {
  const points = buildTimelinePoints(durationMinutes);
  const stageCount = points.length - 1;
  const slumpIndex = clamp(
    Math.floor(seededRand(seed, 2) * stageCount),
    1,
    Math.max(stageCount - 2, 1),
  );
  const spikeIndex = clamp(
    Math.floor(seededRand(seed, 3) * stageCount),
    1,
    Math.max(stageCount - 2, 1),
  );

  const weights = [];
  for (let i = 0; i < stageCount; i += 1) {
    const phaseRatio = i / Math.max(stageCount - 1, 1);
    const base = 1 + Math.sin((phaseRatio + seededRand(seed, i)) * Math.PI) * 0.35;
    const swing = 0.75 + seededRand(seed, i + 10) * 0.7;

    let weight = base * swing;
    if (i === slumpIndex) weight *= 0.45;
    if (i === spikeIndex) weight *= 1.7;

    weights.push(Math.max(weight, 0.12));
  }

  const totalWeight = weights.reduce((sum, value) => sum + value, 0) || 1;

  let cumulativeGold = 500;
  let cumulativeCs = 0;

  return points.map((minute, idx) => {
    if (idx === 0) {
      return { time: String(minute), gold: cumulativeGold, cs: cumulativeCs };
    }

    const w = weights[idx - 1] / totalWeight;
    const goldGain = Math.round((finalGold - 500) * w);
    const csGain = Math.round(finalCs * w * (isJungle ? 0.9 : 1));

    cumulativeGold = clamp(cumulativeGold + goldGain, 500, finalGold);
    cumulativeCs = clamp(cumulativeCs + csGain, 0, finalCs);

    if (idx === points.length - 1) {
      cumulativeGold = finalGold;
      cumulativeCs = finalCs;
    }

    return {
      time: String(minute),
      gold: cumulativeGold,
      cs: cumulativeCs,
    };
  });
};

const buildVisibleSummary = (matchDetails) => {
  const countedMatches = matchDetails.filter((match) => isCountedResult(normalizeResultType(match.resultType, match.win)));
  const wins = countedMatches.filter((match) => isWinResult(normalizeResultType(match.resultType, match.win))).length;
  const losses = countedMatches.length - wins;
  const remakes = matchDetails.filter((match) => normalizeResultType(match.resultType, match.win) === 'REMAKE').length;
  const invalid = matchDetails.filter((match) => normalizeResultType(match.resultType, match.win) === 'INVALID').length;

  const totalKills = countedMatches.reduce((sum, match) => sum + (match.kills || 0), 0);
  const totalDeaths = countedMatches.reduce((sum, match) => sum + (match.deaths || 0), 0);
  const totalAssists = countedMatches.reduce((sum, match) => sum + (match.assists || 0), 0);

  const winRate = countedMatches.length > 0
    ? Math.round((wins * 100) / countedMatches.length)
    : 0;

  const kdaValue = countedMatches.length === 0
    ? 0
    : (totalDeaths === 0
        ? totalKills + totalAssists
        : (totalKills + totalAssists) / totalDeaths);

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

export const buildRecentChampions = (matchDetails) => {
  const stats = new Map();

  matchDetails
    .filter((match) => isCountedResult(normalizeResultType(match.resultType, match.win)))
    .forEach((match) => {
      const key = match.championName || 'Unknown';
      if (!stats.has(key)) {
        stats.set(key, { name: key, games: 0, wins: 0, kills: 0, deaths: 0, assists: 0 });
      }
      const row = stats.get(key);
      row.games += 1;
      if (isWinResult(normalizeResultType(match.resultType, match.win))) row.wins += 1;
      row.kills += match.kills || 0;
      row.deaths += match.deaths || 0;
      row.assists += match.assists || 0;
    });

  return Array.from(stats.values())
    .sort((a, b) => b.games - a.games)
    .slice(0, 3)
    .map((row) => {
      const winRate = row.games > 0 ? Math.round((row.wins * 100) / row.games) : 0;
      const kdaValue = row.deaths === 0 ? row.kills + row.assists : (row.kills + row.assists) / row.deaths;
      return {
        name: row.name,
        games: row.games,
        winRate,
        kda: kdaValue.toFixed(1),
      };
    });
};

const buildPlayerStats = ({ idx, meIndex, match, seed }) => {
  if (idx === meIndex) {
    return {
      kills: match.kills || 0,
      deaths: match.deaths || 0,
      assists: match.assists || 0,
      cs: match.totalCs || 0,
      gold: match.goldEarned || 0,
      damage: Math.max(
        (match.performanceScore || 1) * 1300,
        Math.round((match.goldEarned || 0) * 1.15),
        9000,
      ),
      items: (match.items || []).slice(0, 6),
      isMe: true,
    };
  }

  const r = (offset) => seededRand(seed, idx * 7 + offset);
  const role = POSITION_FALLBACK[idx % POSITION_FALLBACK.length];
  const killBase = role === 'SUP' ? 1 : role === 'JUNGLE' ? 4 : 5;
  const assistBase = role === 'SUP' ? 12 : role === 'JUNGLE' ? 10 : 7;
  const deathBase = role === 'SUP' ? 5 : 6;
  const csBase = role === 'SUP' ? 35 : role === 'JUNGLE' ? 145 : role === 'ADC' ? 205 : 175;

  const kills = Math.round(killBase + r(1) * 7);
  const deaths = Math.max(0, Math.round(deathBase + r(2) * 6 - 2));
  const assists = Math.round(assistBase + r(3) * 13);
  const cs = Math.max(0, Math.round(csBase + (r(4) - 0.5) * 70));
  const gold = Math.max(4500, Math.round(6000 + cs * 22 + kills * 260 + assists * 70 + r(5) * 700));
  const damage = Math.max(5000, Math.round(gold * (0.85 + r(6) * 0.9)));

  return {
    kills,
    deaths,
    assists,
    cs,
    gold,
    damage,
    items: [0, 0, 0, 0, 0, 0],
    isMe: false,
  };
};

const buildAnalysisComments = (player, match) => {
  const resultType = normalizeResultType(match.resultType, match.win);
  const kda = player.deaths === 0
    ? player.kills + player.assists
    : (player.kills + player.assists) / player.deaths;

  const comments = [];

  if (resultType === 'REMAKE') {
    comments.push({ type: 'warning', text: '다시하기 경기라 분석 신뢰도가 낮아 참고용으로만 보세요.' });
    comments.push({ type: 'warning', text: '이 경기는 승패 및 자체 점수 계산에서 제외됩니다.' });
    return comments;
  }

  if (kda >= 4) {
    comments.push({ type: 'good', text: `KDA ${kda.toFixed(2)}로 교전 영향력이 높았습니다.` });
  } else if (kda <= 1.8) {
    comments.push({ type: 'bad', text: `KDA ${kda.toFixed(2)}로 데스 관리가 필요합니다.` });
  } else {
    comments.push({ type: 'warning', text: `KDA ${kda.toFixed(2)}로 무난한 흐름이었습니다.` });
  }

  if ((player.cs || 0) >= 180) {
    comments.push({ type: 'good', text: '파밍 유지력이 좋아 중후반 성장 기반을 확보했습니다.' });
  } else {
    comments.push({ type: 'warning', text: 'CS 유지가 낮아 아이템 타이밍이 지연될 수 있습니다.' });
  }

  if (!player.isMe) {
    comments.push({ type: 'warning', text: '타 플레이어 지표는 공개 매치 정보를 기반으로 추정한 값입니다.' });
  } else if (resultType === 'LOSS') {
    comments.push({ type: 'bad', text: '패배 경기로 오브젝트 타이밍에서 손실이 컸습니다.' });
  }

  return comments;
};

const buildPlayerAnalysis = ({ player, match, seed }) => {
  const radarBase = {
    kda: clamp(Math.round(((player.kills + player.assists) / Math.max(player.deaths, 1)) * 18), 20, 99),
    damage: clamp(Math.round((player.damage / Math.max(player.gold, 1)) * 45), 20, 99),
    vision: clamp(Math.round(30 + seededRand(seed, 18) * 55), 20, 95),
    teamplay: clamp(Math.round(35 + (player.assists / Math.max(player.kills + player.assists, 1)) * 60), 20, 98),
    survival: clamp(Math.round(100 - player.deaths * 11 + seededRand(seed, 19) * 8), 20, 98),
    objective: clamp(Math.round(40 + seededRand(seed, 20) * 50), 20, 95),
  };

  return {
    radarData: [
      { subject: 'KDA', score: radarBase.kda },
      { subject: '딜량', score: radarBase.damage },
      { subject: '시야', score: radarBase.vision },
      { subject: '합류', score: radarBase.teamplay },
      { subject: '생존', score: radarBase.survival },
      { subject: '오브젝트', score: radarBase.objective },
    ],
    timeline: buildGrowthTimeline({
      finalGold: Math.max(player.gold || 0, 2000),
      finalCs: Math.max(player.cs || 0, 10),
      durationMinutes: match.gameDurationMinutes,
      seed,
      isJungle: player.position === 'JUNGLE',
    }),
    analysis: buildAnalysisComments(player, match),
  };
};

export const buildOverviewPlayers = (match, summonerName) => {
  const members = match.teamMembers || [];
  const champs = match.teamChamps || [];
  const meIndex = members.findIndex((name) => name?.split('#')[0] === summonerName);

  const baseSeed = hashSeed(`${match.gameEndTimeStamp || ''}-${summonerName}`);

  const players = members.map((name, idx) => {
    const derived = buildPlayerStats({ idx, meIndex, match, seed: baseSeed });

    return {
      id: idx + 1,
      position: POSITION_FALLBACK[idx % POSITION_FALLBACK.length],
      name: name?.split('#')[0] || `플레이어${idx + 1}`,
      champion: champs[idx] || 'Ahri',
      ...derived,
      maxDamage: 1,
    };
  });

  const maxDamage = Math.max(...players.map((player) => player.damage || 0), 1);
  players.forEach((player) => {
    player.maxDamage = maxDamage;
  });

  return {
    blue: players.slice(0, 5),
    red: players.slice(5, 10),
  };
};

export const toUiMatch = (match, summonerName, index, puuid) => {
  const resultType = normalizeResultType(match.resultType, match.win);
  const resultMeta = getResultMeta(resultType);
  const overview = buildOverviewPlayers(match, summonerName);
  const allPlayers = overview.blue.concat(overview.red);
  const myParticipant = allPlayers.find((p) => p.isMe) || allPlayers[0];

  return {
    id: `${match.gameEndTimeStamp || Date.now()}_${index}`,
    matchId: match.matchId,
    puuid,
    win: resultType === 'WIN',
    isRemake: resultType === 'REMAKE',
    isCountedGame: isCountedResult(resultType),
    resultType,
    resultLabel: resultMeta.label,
    gameDuration: formatDuration(match.gameDurationMinutes),
    gameType: QUEUE_LABEL[match.queueId] || `큐 ${match.queueId || '-'}`,
    timeAgo: formatTimeAgo(match.gameEndTimeStamp),
    myParticipantId: myParticipant?.id || 1,
    summary: {
      champion: match.championName || 'Ahri',
      kills: match.kills || 0,
      deaths: match.deaths || 0,
      assists: match.assists || 0,
      kda: (match.deaths || 0) === 0
        ? String((match.kills || 0) + (match.assists || 0))
        : (((match.kills || 0) + (match.assists || 0)) / (match.deaths || 1)).toFixed(2),
      cs: match.totalCs || 0,
      items: (match.items || []).slice(0, 6),
    },
    overview: {
      blueTeam: {
        isWin: resultType === 'WIN',
        hasWinner: isCountedResult(resultType),
        kills: overview.blue.reduce((sum, p) => sum + (p.kills || 0), 0),
        gold: `${(overview.blue.reduce((sum, p) => sum + (p.gold || 0), 0) / 1000).toFixed(1)}k`,
        players: overview.blue,
      },
      redTeam: {
        isWin: resultType === 'LOSS',
        hasWinner: isCountedResult(resultType),
        kills: overview.red.reduce((sum, p) => sum + (p.kills || 0), 0),
        gold: `${(overview.red.reduce((sum, p) => sum + (p.gold || 0), 0) / 1000).toFixed(1)}k`,
        players: overview.red,
      },
    },
    teamMembers: allPlayers.map((player) => {
      const seed = hashSeed(`${match.gameEndTimeStamp}-${player.id}-${player.name}`);
      const playerAnalysis = buildPlayerAnalysis({ player, match: { ...match, resultType }, seed });

      return {
        participantId: player.id,
        isMe: player.isMe,
        champion: player.champion,
        name: player.name,
        radarData: playerAnalysis.radarData,
        timeline: playerAnalysis.timeline,
        analysis: playerAnalysis.analysis,
      };
    }),
  };
};

export const mapApiToUiData = (apiData, preferredQueue = 'solo') => {
  if (!apiData?.summoner || !apiData?.queues) {
    return MOCK_DATA;
  }

  const queueData = apiData.queues[preferredQueue] || apiData.queues.solo || apiData.queues.flex;
  const rawMatches = queueData?.matchDetails || [];
  const visibleMatches = rawMatches.slice(0, RECENT_MATCH_LIMIT);
  const visibleSummary = buildVisibleSummary(visibleMatches);
  const summonerName = apiData.summoner.name || 'Unknown';

  return {
    summoner: {
      name: summonerName,
      summonerLevel: apiData.summoner.summonerLevel || 0,
      profileIconId: apiData.summoner.profileIconId || MOCK_DATA.summoner.profileIconId,
      scoreDetails: {
        totalScore: Number(
          queueData?.scoreResult?.totalScore
          || queueData?.scoreResult?.currentScore
          || 0,
        ).toFixed(1),
        grade: queueData?.scoreResult?.grade || 'C',
      },
    },
    summary: {
      wins: visibleSummary.wins,
      losses: visibleSummary.losses,
      remakes: visibleSummary.remakes,
      invalid: visibleSummary.invalid,
      countedGames: visibleSummary.countedGames,
      totalGames: visibleSummary.totalGames,
      winRate: visibleSummary.winRate,
      kda: visibleSummary.kda,
      recentChampions: buildRecentChampions(visibleMatches),
    },
    matches: visibleMatches.map((match, index) => toUiMatch(match, summonerName, index, apiData.summoner.puuid)),
  };
};