import { MOCK_DATA, POSITION_FALLBACK, QUEUE_LABEL } from '../data/mmrMockData';

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

export const buildRecentChampions = (matchDetails) => {
  const stats = new Map();

  matchDetails.forEach((match) => {
    const key = match.championName || 'Unknown';
    if (!stats.has(key)) {
      stats.set(key, { name: key, games: 0, wins: 0, kills: 0, deaths: 0, assists: 0 });
    }
    const row = stats.get(key);
    row.games += 1;
    if (match.win) row.wins += 1;
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

export const buildOverviewPlayers = (match, summonerName) => {
  const members = match.teamMembers || [];
  const champs = match.teamChamps || [];
  const meIndex = members.findIndex((name) => name?.split('#')[0] === summonerName);

  const players = members.map((name, idx) => ({
    id: idx + 1,
    position: POSITION_FALLBACK[idx % POSITION_FALLBACK.length],
    name: name?.split('#')[0] || `플레이어${idx + 1}`,
    champion: champs[idx] || 'Ahri',
    kills: idx === meIndex ? match.kills : 0,
    deaths: idx === meIndex ? match.deaths : 0,
    assists: idx === meIndex ? match.assists : 0,
    damage: idx === meIndex ? Math.max((match.performanceScore || 1) * 1000, 8000) : 0,
    maxDamage: Math.max((match.performanceScore || 1) * 1000, 8000),
    cs: idx === meIndex ? match.totalCs || 0 : 0,
    gold: idx === meIndex ? match.goldEarned || 0 : 0,
    items: idx === meIndex ? (match.items || []).slice(0, 6) : [0, 0, 0, 0, 0, 0],
    isMe: idx === meIndex,
  }));

  return {
    blue: players.slice(0, 5),
    red: players.slice(5, 10),
  };
};

export const toUiMatch = (match, summonerName, index) => {
  const overview = buildOverviewPlayers(match, summonerName);
  const myParticipant = overview.blue.concat(overview.red).find((p) => p.isMe) || overview.blue[0];

  return {
    id: `${match.gameEndTimeStamp || Date.now()}_${index}`,
    win: !!match.win,
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
        isWin: !!match.win,
        kills: overview.blue.reduce((sum, p) => sum + (p.kills || 0), 0),
        gold: `${(overview.blue.reduce((sum, p) => sum + (p.gold || 0), 0) / 1000).toFixed(1)}k`,
        players: overview.blue,
      },
      redTeam: {
        isWin: !match.win,
        kills: overview.red.reduce((sum, p) => sum + (p.kills || 0), 0),
        gold: `${(overview.red.reduce((sum, p) => sum + (p.gold || 0), 0) / 1000).toFixed(1)}k`,
        players: overview.red,
      },
    },
    teamMembers: overview.blue.concat(overview.red).map((player) => ({
      participantId: player.id,
      isMe: player.isMe,
      champion: player.champion,
      name: player.name,
      radarData: [
        { subject: 'KDA', score: Math.min(100, ((player.kills + player.assists) * 10)) },
        { subject: '딜량', score: Math.min(100, Math.round((player.damage / (player.maxDamage || 1)) * 100)) },
        { subject: '시야', score: 40 },
        { subject: '합류', score: 60 },
        { subject: '생존', score: Math.max(20, 100 - (player.deaths * 12)) },
        { subject: '오브젝트', score: 50 },
      ],
      timeline: [
        { time: '0', gold: 500, cs: 0 },
        { time: '5', gold: Math.round((player.gold || 0) * 0.2), cs: Math.round((player.cs || 0) * 0.2) },
        { time: '10', gold: Math.round((player.gold || 0) * 0.4), cs: Math.round((player.cs || 0) * 0.4) },
        { time: '15', gold: Math.round((player.gold || 0) * 0.6), cs: Math.round((player.cs || 0) * 0.6) },
        { time: '20', gold: Math.round((player.gold || 0) * 0.8), cs: Math.round((player.cs || 0) * 0.8) },
        { time: '25', gold: player.gold || 0, cs: player.cs || 0 },
      ],
      analysis: [
        { type: player.isMe && match.win ? 'good' : 'bad', text: player.isMe && match.win ? '승리에 기여한 경기입니다.' : '지표를 바탕으로 개선 포인트를 확인하세요.' },
        { type: 'warning', text: '임시 분석 데이터입니다. 상세 로직은 Phase C에서 고도화됩니다.' },
      ],
    })),
  };
};

export const mapApiToUiData = (apiData, preferredQueue = 'solo') => {
  if (!apiData?.summoner || !apiData?.queues) {
    return MOCK_DATA;
  }

  const queueData = apiData.queues[preferredQueue] || apiData.queues.solo || apiData.queues.flex;
  const matchDetails = queueData?.matchDetails || [];
  const queueSummary = queueData?.summary || { wins: 0, losses: 0, winRate: 0, kda: '0.00' };
  const summonerName = apiData.summoner.name || 'Unknown';

  return {
    summoner: {
      name: summonerName,
      summonerLevel: apiData.summoner.summonerLevel || 0,
      profileIconId: apiData.summoner.profileIconId || MOCK_DATA.summoner.profileIconId,
      scoreDetails: {
        totalScore: Number(queueData?.scoreResult?.totalScore || 0).toFixed(1),
        grade: queueData?.scoreResult?.grade || 'C',
      },
    },
    summary: {
      wins: queueSummary.wins || 0,
      losses: queueSummary.losses || 0,
      winRate: queueSummary.winRate || 0,
      kda: queueSummary.kda || '0.00',
      recentChampions: buildRecentChampions(matchDetails),
    },
    matches: matchDetails.map((match, index) => toUiMatch(match, summonerName, index)),
  };
};
