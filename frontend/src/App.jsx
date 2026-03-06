import React, { useCallback, useEffect, useState } from 'react';
import axios from 'axios';
import {
  LineChart, Line, XAxis, YAxis, Tooltip as RechartsTooltip, ResponsiveContainer, CartesianGrid, Legend,
  Radar, RadarChart, PolarGrid, PolarAngleAxis
} from 'recharts';


const Icon = ({ children, className = '' }) => (
  <svg
    viewBox="0 0 24 24"
    width="1em"
    height="1em"
    fill="none"
    stroke="currentColor"
    strokeWidth="2"
    strokeLinecap="round"
    strokeLinejoin="round"
    className={className}
    style={{ width: "1em", height: "1em", flexShrink: 0 }}
  >
    {children}
  </svg>
);

const Search = ({ className }) => <Icon className={className}><circle cx="11" cy="11" r="8" /><line x1="21" y1="21" x2="16.65" y2="16.65" /></Icon>;
const Trophy = ({ className }) => <Icon className={className}><path d="M8 21h8" /><path d="M12 17v4" /><path d="M7 4h10v3a5 5 0 0 1-10 0z" /><path d="M5 5H3a3 3 0 0 0 3 3" /><path d="M19 5h2a3 3 0 0 1-3 3" /></Icon>;
const ChevronRight = ({ className }) => <Icon className={className}><polyline points="9 18 15 12 9 6" /></Icon>;
const ChevronDown = ({ className }) => <Icon className={className}><polyline points="6 9 12 15 18 9" /></Icon>;
const Activity = ({ className }) => <Icon className={className}><polyline points="22 12 18 12 15 21 9 3 6 12 2 12" /></Icon>;
const Flame = ({ className }) => <Icon className={className}><path d="M8.5 14.5A4.5 4.5 0 1 0 15 18c0-1.6-1-2.7-2-3.6 0 0 .5 2.1-1 2.6 0 0-1.2-.5-1-2.5-1.5 1-2.5 2.2-2.5 3.9Z" /><path d="M12 2s3 2.5 3 5a3 3 0 0 1-6 0c0-2.5 3-5 3-5Z" /></Icon>;
const Target = ({ className }) => <Icon className={className}><circle cx="12" cy="12" r="10" /><circle cx="12" cy="12" r="6" /><circle cx="12" cy="12" r="2" /></Icon>;
const AlertCircle = ({ className }) => <Icon className={className}><circle cx="12" cy="12" r="10" /><line x1="12" y1="8" x2="12" y2="12" /><line x1="12" y1="16" x2="12.01" y2="16" /></Icon>;
const ThumbsUp = ({ className }) => <Icon className={className}><path d="M14 9V5a3 3 0 0 0-3-3l-1 4-3 3v11h11l3-8V9z" /><path d="M7 22H4a2 2 0 0 1-2-2v-8a2 2 0 0 1 2-2h3" /></Icon>;
const Swords = ({ className }) => <Icon className={className}><path d="M14.5 17.5 21 11l-2-2-6.5 6.5" /><path d="m3 3 2 2" /><path d="M3 21l6.5-6.5" /><path d="m9-9L11 3 3 11l2 2 6.5-6.5" /></Icon>;
const Users = ({ className }) => <Icon className={className}><path d="M16 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2" /><circle cx="8.5" cy="7" r="4" /><path d="M20 8v6" /><path d="M23 11h-6" /></Icon>;

const MOCK_DATA = {
  summoner: {
    name: 'Hide on bush',
    summonerLevel: 632,
    profileIconId: 5412,
    scoreDetails: {
      totalScore: 92.5,
      grade: 'S+',
    },
  },
  summary: {
    wins: 14,
    losses: 6,
    winRate: 70,
    kda: '3.84',
    recentChampions: [
      { name: 'Ahri', winRate: 80, kda: '5.2', games: 10 },
      { name: 'Azir', winRate: 60, kda: '3.1', games: 5 },
      { name: 'Orianna', winRate: 66, kda: '4.0', games: 5 },
    ],
  },
  matches: [
    {
      id: 'KR_6942123451',
      win: true,
      gameDuration: '25:12',
      gameType: '솔랭',
      timeAgo: '2시간 전',
      myParticipantId: 1,
      summary: { champion: 'Ahri', kills: 10, deaths: 2, assists: 8, kda: '9.00', cs: 210, items: [3089, 3152, 3020, 3165, 3041, 0] },
      overview: {
        blueTeam: {
          isWin: true, kills: 25, gold: '45.2k',
          players: [
            { id: 3, position: 'TOP', name: '탑신병자', champion: 'Darius', kills: 5, deaths: 4, assists: 3, damage: 18000, maxDamage: 32000, cs: 200, gold: 10200, items: [3078, 3047, 3053, 0, 0, 0] },
            { id: 2, position: 'JUNGLE', name: 'T1 Oner', champion: 'LeeSin', kills: 4, deaths: 2, assists: 12, damage: 12000, maxDamage: 32000, cs: 130, gold: 8900, items: [3142, 3111, 3026, 3071, 0, 0] },
            { id: 1, position: 'MID', name: 'Hide on bush', champion: 'Ahri', kills: 10, deaths: 2, assists: 8, damage: 32000, maxDamage: 32000, cs: 210, gold: 12500, items: [3089, 3152, 3020, 3165, 3041, 0], isMe: true },
            { id: 4, position: 'ADC', name: '원딜러', champion: 'Jinx', kills: 6, deaths: 1, assists: 5, damage: 24000, maxDamage: 32000, cs: 230, gold: 11500, items: [3031, 3006, 3085, 3072, 0, 0] },
            { id: 5, position: 'SUP', name: '서포터', champion: 'Lulu', kills: 0, deaths: 2, assists: 18, damage: 6000, maxDamage: 32000, cs: 15, gold: 5000, items: [3158, 2065, 3107, 0, 0, 0] },
          ],
        },
        redTeam: {
          isWin: false, kills: 11, gold: '38.1k',
          players: [
            { id: 6, position: 'TOP', name: '적탑', champion: 'Ornn', kills: 1, deaths: 5, assists: 4, damage: 14000, maxDamage: 32000, cs: 160, gold: 8200, items: [3068, 3047, 3075, 0, 0, 0] },
            { id: 7, position: 'JUNGLE', name: '적정글', champion: 'Sejuani', kills: 2, deaths: 6, assists: 5, damage: 9000, maxDamage: 32000, cs: 110, gold: 7500, items: [3068, 3111, 3075, 0, 0, 0] },
            { id: 8, position: 'MID', name: '적미드', champion: 'Azir', kills: 5, deaths: 4, assists: 2, damage: 28000, maxDamage: 32000, cs: 240, gold: 11200, items: [3152, 3020, 3115, 3089, 0, 0] },
            { id: 9, position: 'ADC', name: '적원딜', champion: 'Aphelios', kills: 3, deaths: 6, assists: 3, damage: 21000, maxDamage: 32000, cs: 200, gold: 9800, items: [3031, 3006, 3085, 0, 0, 0] },
            { id: 10, position: 'SUP', name: '적서폿', champion: 'Nautilus', kills: 0, deaths: 4, assists: 6, damage: 5000, maxDamage: 32000, cs: 30, gold: 4500, items: [3111, 2065, 3190, 0, 0, 0] },
          ],
        },
      },
      teamMembers: [
        {
          participantId: 1,
          isMe: true,
          champion: 'Ahri',
          name: 'Hide on bush',
          radarData: [
            { subject: 'KDA', score: 95 },
            { subject: '딜량', score: 90 },
            { subject: '시야', score: 40 },
            { subject: '합류', score: 85 },
            { subject: '생존', score: 80 },
            { subject: '오브젝트', score: 70 },
          ],
          timeline: [
            { time: '0', gold: 500, cs: 0 },
            { time: '5', gold: 1800, cs: 45 },
            { time: '10', gold: 3500, cs: 90 },
            { time: '15', gold: 5800, cs: 140 },
            { time: '20', gold: 8900, cs: 185 },
            { time: '25', gold: 12500, cs: 210 },
          ],
          analysis: [
            { type: 'good', text: '라인전 단계(10분 이전)에서 상대를 압도했습니다.' },
            { type: 'good', text: '팀 전체 킬의 75%에 관여하며 승리를 견인했습니다.' },
            { type: 'bad', text: '시야 점수가 티어 평균 대비 부족합니다.' },
          ],
        },
        {
          participantId: 2,
          isMe: false,
          champion: 'LeeSin',
          name: 'T1 Oner',
          radarData: [
            { subject: 'KDA', score: 60 },
            { subject: '딜량', score: 50 },
            { subject: '시야', score: 90 },
            { subject: '합류', score: 95 },
            { subject: '생존', score: 40 },
            { subject: '오브젝트', score: 100 },
          ],
          timeline: [
            { time: '0', gold: 500, cs: 0 },
            { time: '5', gold: 1500, cs: 30 },
            { time: '10', gold: 2800, cs: 60 },
            { time: '15', gold: 4200, cs: 90 },
            { time: '20', gold: 5500, cs: 110 },
            { time: '25', gold: 7200, cs: 130 },
          ],
          analysis: [
            { type: 'good', text: '모든 드래곤과 전령 획득에 관여했습니다.' },
            { type: 'bad', text: '중반 이후 데스가 누적되며 레벨링이 다소 지연되었습니다.' },
          ],
        },
      ],
    },
  ],
};

const QUEUE_LABEL = {
  420: '솔랭',
  440: '자랭',
};

const POSITION_FALLBACK = ['TOP', 'JUNGLE', 'MID', 'ADC', 'SUP'];

const formatDuration = (minutes) => {
  const safeMinutes = Number.isFinite(minutes) ? minutes : 0;
  const m = Math.max(0, Math.floor(safeMinutes));
  return `${String(m).padStart(2, '0')}:00`;
};

const formatTimeAgo = (timestamp) => {
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

const buildRecentChampions = (matchDetails) => {
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

const buildOverviewPlayers = (match, summonerName) => {
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

const toUiMatch = (match, summonerName, index) => {
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
      kda: (match.deaths || 0) === 0 ? String((match.kills || 0) + (match.assists || 0)) : (((match.kills || 0) + (match.assists || 0)) / (match.deaths || 1)).toFixed(2),
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

const mapApiToUiData = (apiData, preferredQueue = 'solo') => {
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

const PlayerRow = ({ player, isBlueTeam }) => (
  <div className={`flex items-center justify-between py-1 px-2 md:py-1.5 md:px-3 hover:bg-black/20 ${player.isMe ? 'bg-black/30 border-l-2 border-l-blue-400' : ''}`}>
    <div className="flex items-center gap-2 w-[120px] md:w-[160px]">
      <div className="relative flex-shrink-0">
        <img src={`https://ddragon.leagueoflegends.com/cdn/14.3.1/img/champion/${player.champion}.png`} className="w-6 h-6 md:w-8 md:h-8 rounded-full" alt="" />
      </div>
      <span className={`text-[10px] md:text-xs truncate ${player.isMe ? 'text-white font-bold' : 'text-gray-400'}`}>{player.name}</span>
    </div>
    <div className="w-[80px] text-center flex flex-col">
      <span className="text-[10px] md:text-xs font-semibold text-gray-200">{player.kills} / <span className="text-red-400">{player.deaths}</span> / {player.assists}</span>
    </div>
    <div className="w-[80px] md:w-[100px] flex flex-col gap-1 items-center hidden sm:flex">
      <span className="text-[9px] md:text-[10px] text-gray-400">{player.damage.toLocaleString()}</span>
      <div className="w-full bg-gray-800 h-1.5 rounded-full overflow-hidden">
        <div className={`h-full rounded-full ${isBlueTeam ? 'bg-blue-500' : 'bg-red-500'}`} style={{ width: `${(player.damage / player.maxDamage) * 100}%` }}></div>
      </div>
    </div>
    <div className="w-[40px] text-center text-[10px] md:text-xs text-gray-400">{player.cs}</div>
    <div className="w-[120px] md:w-[150px] flex gap-0.5 justify-end">
      {player.items.map((item, idx) => (
        <div key={idx} className={`w-4 h-4 md:w-5 md:h-5 rounded ${item === 0 ? 'bg-gray-800/50' : 'bg-gray-700'}`}>
          {item !== 0 && <img src={`https://ddragon.leagueoflegends.com/cdn/14.3.1/img/item/${item}.png`} className="w-full h-full rounded" alt="item"/>}
        </div>
      ))}
    </div>
  </div>
);

const ComparisonBar = ({ label, myValue, oppValue, isLowerBetter = false }) => {
  const total = myValue + oppValue;
  const myPct = total === 0 ? 50 : (myValue / total) * 100;
  const oppPct = total === 0 ? 50 : (oppValue / total) * 100;
  const isMyWin = isLowerBetter ? myValue < oppValue : myValue > oppValue;
  const isOppWin = isLowerBetter ? oppValue < myValue : oppValue > myValue;

  return (
    <div className="flex flex-col gap-1 mb-3">
      <div className="flex justify-between text-[10px] md:text-xs px-1">
        <span className={`${isMyWin ? 'text-blue-400 font-bold' : 'text-gray-400'}`}>{myValue.toLocaleString()}</span>
        <span className="text-gray-500">{label}</span>
        <span className={`${isOppWin ? 'text-red-400 font-bold' : 'text-gray-400'}`}>{oppValue.toLocaleString()}</span>
      </div>
      <div className="flex w-full h-1.5 md:h-2 rounded-full overflow-hidden bg-gray-800">
        <div className="bg-blue-500 h-full transition-all duration-500" style={{ width: `${myPct}%` }}></div>
        <div className="bg-red-500 h-full transition-all duration-500" style={{ width: `${oppPct}%` }}></div>
      </div>
    </div>
  );
};

const MatchCard = ({ match, isExpanded, onToggle }) => {
  const [activeTab, setActiveTab] = useState('overview');
  const [selectedPlayerId, setSelectedPlayerId] = useState(match.myParticipantId);

  const bluePlayer = match.overview.blueTeam.players.find((p) => p.id === selectedPlayerId);
  const redPlayer = match.overview.redTeam.players.find((p) => p.id === selectedPlayerId);
  const currentPlayerOverview = bluePlayer || redPlayer;

  let opponentOverview = null;
  if (currentPlayerOverview) {
    const opponentTeam = bluePlayer ? match.overview.redTeam.players : match.overview.blueTeam.players;
    opponentOverview = opponentTeam.find((p) => p.position === currentPlayerOverview.position);
  }
  const selectedPlayerAnalysis = match.teamMembers.find((p) => p.participantId === selectedPlayerId);

  return (
    <div className="flex flex-col rounded-xl border border-gray-800 bg-[#1c1c1f] overflow-hidden">
      <div
        onClick={onToggle}
        className={`flex flex-col sm:flex-row cursor-pointer border-l-4 ${match.win ? 'border-l-blue-500 hover:bg-[#1e293b]/60' : 'border-l-red-500 hover:bg-[#3f1d24]/60'} transition-colors`}
      >
        <div className="w-full sm:w-28 p-3 flex flex-col justify-center text-xs text-gray-400 border-b sm:border-b-0 sm:border-r border-gray-800/50">
          <div className={`font-bold mb-1 ${match.win ? 'text-blue-400' : 'text-red-400'}`}>{match.win ? '승리' : '패배'}</div>
          <div className="mb-1">{match.gameType}</div>
          <div>{match.timeAgo}</div>
          <div className="border-t border-gray-700 my-1"></div>
          <div>{match.gameDuration}</div>
        </div>

        <div className="flex-1 p-3 flex items-center gap-4">
          <div className="flex items-center gap-2">
            <img src={`https://ddragon.leagueoflegends.com/cdn/14.3.1/img/champion/${match.summary.champion}.png`} alt={match.summary.champion} className="w-10 h-10 sm:w-12 sm:h-12 rounded-full border border-gray-700"/>
            <div className="flex flex-col gap-1 hidden sm:flex">
              <div className="w-4 h-4 sm:w-5 sm:h-5 bg-gray-700 rounded text-[8px] flex items-center justify-center border border-gray-600">D</div>
              <div className="w-4 h-4 sm:w-5 sm:h-5 bg-gray-700 rounded text-[8px] flex items-center justify-center border border-gray-600">F</div>
            </div>
          </div>
          <div className="flex flex-col items-center flex-1">
            <div className="font-bold text-gray-200 text-base sm:text-lg tracking-wide">
              {match.summary.kills} <span className="text-gray-500 font-normal">/</span> <span className="text-red-400">{match.summary.deaths}</span> <span className="text-gray-500 font-normal">/</span> {match.summary.assists}
            </div>
            <div className="text-xs text-gray-400 mt-0.5">{match.summary.kda} 평점</div>
          </div>
          <div className="flex flex-col items-center justify-center w-16 sm:w-20 text-xs text-gray-400">
            <div>CS {match.summary.cs}</div>
          </div>
        </div>

        <div className="w-full sm:w-auto p-3 flex items-center justify-center bg-black/10">
          <div className="grid grid-cols-4 sm:grid-cols-3 gap-1">
            {match.summary.items.map((item, idx) => (
              <div key={idx} className={`w-6 h-6 rounded ${item === 0 ? 'bg-gray-800/50' : 'bg-gray-700'}`}>
                {item !== 0 && <img src={`https://ddragon.leagueoflegends.com/cdn/14.3.1/img/item/${item}.png`} alt="item" className="w-full h-full rounded"/>}
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
            <button onClick={() => setActiveTab('overview')} className={`px-4 py-3 text-xs md:text-sm font-semibold transition-colors border-b-2 flex items-center gap-1 md:gap-2 whitespace-nowrap ${activeTab === 'overview' ? 'border-blue-500 text-blue-400' : 'border-transparent text-gray-400 hover:text-gray-200'}`}>
              <Swords className="w-4 h-4" /> 종합 전적
            </button>
            <button onClick={() => setActiveTab('analysis')} className={`px-4 py-3 text-xs md:text-sm font-semibold transition-colors border-b-2 flex items-center gap-1 md:gap-2 whitespace-nowrap ${activeTab === 'analysis' ? 'border-purple-500 text-purple-400' : 'border-transparent text-gray-400 hover:text-gray-200'}`}>
              <Activity className="w-4 h-4" /> 상세 분석 및 코칭
            </button>
          </div>

          {activeTab === 'overview' && (
            <div className="flex flex-col gap-3 p-3 md:p-4">
              <div className="bg-[#1e293b]/20 rounded-lg border border-blue-900/30">
                <div className="bg-[#1e293b]/50 px-3 py-2 border-b border-blue-900/30 flex justify-between items-center">
                  <span className="text-xs font-bold text-blue-400">{match.overview.blueTeam.isWin ? '승리' : '패배'} (블루팀)</span>
                  <span className="text-[10px] md:text-xs text-gray-400">총 킬: {match.overview.blueTeam.kills}</span>
                </div>
                <div className="flex flex-col">{match.overview.blueTeam.players.map((p) => <PlayerRow key={p.id} player={p} isBlueTeam={true} />)}</div>
              </div>
              <div className="bg-[#3f1d24]/20 rounded-lg border border-red-900/30">
                <div className="bg-[#3f1d24]/50 px-3 py-2 border-b border-red-900/30 flex justify-between items-center">
                  <span className="text-xs font-bold text-red-400">{match.overview.redTeam.isWin ? '승리' : '패배'} (레드팀)</span>
                  <span className="text-[10px] md:text-xs text-gray-400">총 킬: {match.overview.redTeam.kills}</span>
                </div>
                <div className="flex flex-col">{match.overview.redTeam.players.map((p) => <PlayerRow key={p.id} player={p} isBlueTeam={false} />)}</div>
              </div>
            </div>
          )}

          {activeTab === 'analysis' && currentPlayerOverview && selectedPlayerAnalysis && (
            <div className="flex flex-col">
              <div className="bg-[#18181b] px-4 py-3 flex items-center gap-4 border-b border-gray-800 overflow-x-auto">
                <span className="text-xs font-bold text-gray-400 whitespace-nowrap">분석 대상:</span>
                <div className="flex gap-2 md:gap-4">
                  {match.overview.blueTeam.players.map((p) => (
                    <div key={p.id} onClick={() => setSelectedPlayerId(p.id)} className={`flex items-center gap-1.5 cursor-pointer p-1 pr-2 rounded-full transition-all ${selectedPlayerId === p.id ? 'bg-purple-600/20 ring-1 ring-purple-500' : 'hover:bg-gray-800 grayscale hover:grayscale-0 opacity-60 hover:opacity-100'}`}>
                      <img src={`https://ddragon.leagueoflegends.com/cdn/14.3.1/img/champion/${p.champion}.png`} className="w-6 h-6 md:w-8 md:h-8 rounded-full" alt=""/>
                      <div className="flex flex-col hidden sm:flex">
                        <span className={`text-[10px] md:text-xs font-bold leading-none ${selectedPlayerId === p.id ? 'text-purple-400' : 'text-gray-300'}`}>{p.champion}</span>
                      </div>
                    </div>
                  ))}
                </div>
              </div>

              <div className="p-3 md:p-4 grid grid-cols-1 md:grid-cols-2 gap-4">
                <div className="bg-[#1c1c1f] rounded-xl border border-gray-800/60 p-3 flex flex-col items-center col-span-1">
                  <h3 className="text-xs font-bold text-gray-300 mb-2">플레이 성향 지표</h3>
                  <div className="w-full h-40">
                    <ResponsiveContainer width="100%" height="100%">
                      <RadarChart cx="50%" cy="50%" outerRadius="65%" data={selectedPlayerAnalysis.radarData}>
                        <PolarGrid stroke="#3f3f46" />
                        <PolarAngleAxis dataKey="subject" tick={{ fill: '#a1a1aa', fontSize: 10 }} />
                        <Radar name="지표" dataKey="score" stroke="#a855f7" fill="#a855f7" fillOpacity={0.4} />
                      </RadarChart>
                    </ResponsiveContainer>
                  </div>
                </div>

                <div className="bg-[#1c1c1f] rounded-xl border border-gray-800/60 p-3 flex flex-col col-span-1">
                  <h3 className="text-xs font-bold text-gray-300 mb-3 flex items-center gap-1"><Users className="w-3 h-3 text-orange-400" /> 맞라인({currentPlayerOverview.position}) 상대 비교</h3>
                  {opponentOverview ? (
                    <div className="flex flex-col justify-center px-1">
                      <div className="flex justify-between items-center mb-3">
                        <div className="flex items-center gap-2">
                          <img src={`https://ddragon.leagueoflegends.com/cdn/14.3.1/img/champion/${currentPlayerOverview.champion}.png`} className="w-7 h-7 rounded-full border border-blue-500" alt="" />
                          <span className="text-[10px] font-bold text-blue-400 hidden sm:block">{currentPlayerOverview.champion}</span>
                        </div>
                        <span className="text-[10px] text-gray-500 font-bold px-2 py-0.5 bg-gray-800 rounded-full">VS</span>
                        <div className="flex items-center gap-2">
                          <span className="text-[10px] font-bold text-red-400 hidden sm:block">{opponentOverview.champion}</span>
                          <img src={`https://ddragon.leagueoflegends.com/cdn/14.3.1/img/champion/${opponentOverview.champion}.png`} className="w-7 h-7 rounded-full border border-red-500" alt="" />
                        </div>
                      </div>
                      <ComparisonBar label="CS" myValue={currentPlayerOverview.cs} oppValue={opponentOverview.cs} />
                      <ComparisonBar label="골드" myValue={currentPlayerOverview.gold} oppValue={opponentOverview.gold} />
                      <ComparisonBar label="딜량" myValue={currentPlayerOverview.damage} oppValue={opponentOverview.damage} />
                    </div>
                  ) : (
                    <div className="text-xs text-gray-500 text-center mt-8">상대를 찾을 수 없습니다.</div>
                  )}
                </div>

                <div className="bg-[#1c1c1f] rounded-xl border border-gray-800/60 p-3 md:col-span-2">
                  <h3 className="text-xs font-bold text-gray-300 mb-2">시간대별 성장 추이</h3>
                  <div className="w-full h-44">
                    <ResponsiveContainer width="100%" height="100%">
                      <LineChart data={selectedPlayerAnalysis.timeline} margin={{ top: 5, right: 10, bottom: 0, left: -25 }}>
                        <CartesianGrid strokeDasharray="3 3" stroke="#27272a" vertical={false} />
                        <XAxis dataKey="time" stroke="#71717a" tick={{ fill: '#71717a', fontSize: 10 }} tickFormatter={(val) => `${val}분`} />
                        <YAxis yAxisId="left" stroke="#eab308" tick={{ fill: '#eab308', fontSize: 10 }} tickFormatter={(val) => `${val / 1000}k`} />
                        <YAxis yAxisId="right" orientation="right" stroke="#60a5fa" tick={{ fill: '#60a5fa', fontSize: 10 }} />
                        <RechartsTooltip contentStyle={{ backgroundColor: '#18181b', borderColor: '#3f3f46', borderRadius: '8px', fontSize: '11px' }} />
                        <Legend verticalAlign="top" height={24} wrapperStyle={{ fontSize: '10px', color: '#a1a1aa' }} />
                        <Line yAxisId="left" name="골드" type="monotone" dataKey="gold" stroke="#eab308" strokeWidth={2} dot={{ r: 2 }} />
                        <Line yAxisId="right" name="CS" type="monotone" dataKey="cs" stroke="#60a5fa" strokeWidth={2} dot={{ r: 2 }} />
                      </LineChart>
                    </ResponsiveContainer>
                  </div>
                </div>

                <div className="bg-[#1c1c1f] rounded-xl border border-gray-800/60 p-3 md:col-span-2">
                  <h3 className="text-xs font-bold text-gray-300 mb-2 flex items-center gap-1"><Target className="w-3 h-3 text-purple-400" /> 시스템 코멘트</h3>
                  <div className="grid grid-cols-1 sm:grid-cols-2 gap-2">
                    {selectedPlayerAnalysis.analysis.map((item, idx) => (
                      <div key={idx} className={`p-2 rounded-lg border text-[11px] flex items-start gap-2 ${item.type === 'good' ? 'bg-blue-900/10 border-blue-500/20 text-blue-100' : item.type === 'warning' ? 'bg-yellow-900/10 border-yellow-500/20 text-yellow-100' : 'bg-red-900/10 border-red-500/20 text-red-100'}`}>
                        {item.type === 'good' && <ThumbsUp className="w-3 h-3 text-blue-400 mt-0.5 flex-shrink-0" />}
                        {item.type === 'warning' && <AlertCircle className="w-3 h-3 text-yellow-400 mt-0.5 flex-shrink-0" />}
                        {item.type === 'bad' && <AlertCircle className="w-3 h-3 text-red-400 mt-0.5 flex-shrink-0" />}
                        <span className="leading-tight">{item.text}</span>
                      </div>
                    ))}
                  </div>
                </div>
              </div>
            </div>
          )}
        </div>
      )}
    </div>
  );
};

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

  const getGradeColor = (grade) => {
    if (grade.startsWith('S')) return 'text-yellow-400 drop-shadow-[0_0_8px_rgba(250,204,21,0.5)]';
    if (grade.startsWith('A')) return 'text-purple-400';
    if (grade.startsWith('B')) return 'text-blue-400';
    return 'text-gray-400';
  };

  return (
    <div className="min-h-screen bg-[#121212] text-gray-200 font-sans pb-20">
      <header className="bg-[#18181b] border-b border-gray-800 sticky top-0 z-50">
        <div className="max-w-6xl mx-auto px-4 py-4 flex flex-col md:flex-row items-center justify-between gap-4">
          <div className="flex items-center gap-2 text-2xl font-bold text-blue-500">
            <Trophy className="w-8 h-8" /><span>LOLMMR</span>
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
            <button type="submit" className="absolute right-2 top-1.5 bg-blue-600 hover:bg-blue-700 text-white px-3 py-1 rounded text-xs font-medium">검색</button>
          </form>
        </div>
      </header>

      {(isLoading || errorMessage) && (
        <div className="max-w-6xl mx-auto px-4 pt-4">
          {isLoading && <div className="text-xs text-blue-300">데이터를 불러오는 중...</div>}
          {errorMessage && <div className="text-xs text-amber-300 mt-1">{errorMessage}</div>}
        </div>
      )}

      <main className="max-w-6xl mx-auto px-4 py-8">
        <div className="flex flex-col lg:flex-row gap-6">
          <div className="w-full lg:w-80 flex flex-col gap-4 flex-shrink-0">
            <div className="bg-[#1c1c1f] rounded-xl p-5 border border-gray-800 relative overflow-hidden">
              <div className="absolute top-0 left-0 w-full h-2 bg-gradient-to-r from-blue-600 to-purple-600"></div>
              <div className="flex items-start gap-4">
                <div className="relative">
                  <img src={`https://ddragon.leagueoflegends.com/cdn/14.3.1/img/profileicon/${data.summoner.profileIconId}.png`} alt="Profile" className="w-20 h-20 rounded-xl border border-gray-700 object-cover"/>
                  <div className="absolute -bottom-2 left-1/2 -translate-x-1/2 bg-[#121212] border border-gray-700 text-[10px] px-2 py-0.5 rounded-full text-gray-300 whitespace-nowrap">
                    LV {data.summoner.summonerLevel}
                  </div>
                </div>
                <div>
                  <h1 className="text-xl font-bold text-white mb-2">{data.summoner.name}</h1>
                  <div className="flex gap-2">
                    <div className="bg-[#27272a] px-2 py-1 rounded flex flex-col items-center border border-gray-700/50">
                      <span className="text-[10px] text-gray-400">자체 평가</span>
                      <span className={`text-sm font-black ${getGradeColor(data.summoner.scoreDetails.grade)}`}>{data.summoner.scoreDetails.grade}</span>
                    </div>
                    <div className="bg-[#27272a] px-2 py-1 rounded flex flex-col items-center border border-gray-700/50">
                      <span className="text-[10px] text-gray-400">종합 점수</span>
                      <span className="text-sm font-bold text-white">{data.summoner.scoreDetails.totalScore}</span>
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
                    <circle cx="40" cy="40" r="34" fill="transparent" stroke="#3f3f46" strokeWidth="8" />
                    <circle cx="40" cy="40" r="34" fill="transparent" stroke="#3b82f6" strokeWidth="8" strokeDasharray={`${(data.summary.winRate / 100) * 213} 213`} />
                  </svg>
                  <div className="absolute flex flex-col items-center">
                    <span className="text-sm font-bold text-white">{data.summary.winRate}%</span>
                    <span className="text-[10px] text-gray-400">{data.summary.wins}승 {data.summary.losses}패</span>
                  </div>
                </div>
              </div>
              <div className="h-16 w-px bg-gray-800"></div>
              <div className="flex flex-col justify-center text-center">
                <span className="text-xs text-gray-400 mb-1">KDA 평점</span>
                <div className="text-xl font-bold text-blue-400">{data.summary.kda}</div>
                <div className="text-[10px] text-gray-500 mt-1">킬관여율 55%</div>
              </div>
            </div>

            <div className="bg-[#1c1c1f] rounded-xl border border-gray-800 overflow-hidden">
              <div className="px-4 py-3 border-b border-gray-800 text-xs font-bold text-gray-300 flex items-center gap-1.5">
                <Flame className="w-3.5 h-3.5 text-orange-500" /> 모스트 챔피언
              </div>
              <div className="p-1.5">
                {data.summary.recentChampions.map((champ, idx) => (
                  <div key={idx} className="flex items-center gap-3 p-2.5 hover:bg-[#27272a] rounded-lg transition-colors cursor-default">
                    <img src={`https://ddragon.leagueoflegends.com/cdn/14.3.1/img/champion/${champ.name}.png`} alt={champ.name} className="w-8 h-8 rounded-full bg-gray-700"/>
                    <div className="flex-1">
                      <div className="text-sm font-semibold text-gray-200 leading-tight">{champ.name}</div>
                      <div className="text-[10px] text-gray-400 mt-0.5">{champ.games} 게임</div>
                    </div>
                    <div className="text-right">
                      <div className={`text-sm font-semibold ${champ.winRate >= 60 ? 'text-red-400' : 'text-gray-300'}`}>{champ.winRate}%</div>
                      <div className="text-[10px] text-gray-400">{champ.kda} 평점</div>
                    </div>
                  </div>
                ))}
              </div>
            </div>
          </div>

          <div className="flex-1 flex flex-col gap-2">
            {data.matches.map((match) => (
              <MatchCard
                key={`${match.id}-${expandedMatchId === match.id ? 'open' : 'closed'}` }
                match={match}
                isExpanded={expandedMatchId === match.id}
                onToggle={() => setExpandedMatchId((prev) => (prev === match.id ? null : match.id))}
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
