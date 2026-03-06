export const MOCK_DATA = {
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

export const QUEUE_LABEL = {
  420: '솔랭',
  440: '자랭',
};

export const POSITION_FALLBACK = ['TOP', 'JUNGLE', 'MID', 'ADC', 'SUP'];
