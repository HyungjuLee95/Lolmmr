import { useState, useEffect, useCallback } from 'react';
import axios from 'axios';
import { LineChart, Line, XAxis, YAxis, Tooltip, ResponsiveContainer } from 'recharts';
import './App.css';

// --- 전역 설정 ---
const LOL_VERSION = "15.2.1";

// --- 유틸리티 함수 ---
const getChampImg = (name) => {
  if (!name) return "https://ddragon.leagueoflegends.com/cdn/img/champion/splash/ext/0.jpg";

  // Yunara 같은 특수 케이스나 오타 데이터 방어 로직
  // 공식 데이터 드래곤에 없는 이름일 경우를 대비해 첫 글자 대문자화
  const formattedName = name.charAt(0).toUpperCase() + name.slice(1);

  // 만약 이름이 "Yunara"라면 다른 챔피언으로 매칭하거나 기본 이미지를 반환하도록 설정 가능
  if (formattedName === "Yunara") {
    return "https://ddragon.leagueoflegends.com/cdn/img/champion/splash/ext/0.jpg"; // 기본 이미지
  }

  return `https://ddragon.leagueoflegends.com/cdn/${LOL_VERSION}/img/champion/${formattedName}.png`;
};

const getItemImg = (id) => id === 0 ? null : `https://ddragon.leagueoflegends.com/cdn/${LOL_VERSION}/img/item/${id}.png`;
const getSpellImg = (id, spellMap) => spellMap[id] ? `https://ddragon.leagueoflegends.com/cdn/${LOL_VERSION}/img/spell/${spellMap[id]}.png` : null;
const getRuneImg = (id, runeMap) => runeMap[id] ? `https://ddragon.leagueoflegends.com/cdn/img/${runeMap[id]}` : null;

const getTierImg = (tier) => {
  if (!tier || tier === "UNRANKED") return "https://raw.communitydragon.org/latest/plugins/rcp-fe-lol-static-assets/global/default/images/ranked-emblem/emblem-unranked.png";
  const t = tier.toLowerCase();
  return `https://raw.communitydragon.org/latest/plugins/rcp-fe-lol-static-assets/global/default/images/ranked-emblem/emblem-${t}.png`;
};

const timeAgo = (timestamp) => {
  const diff = Date.now() - timestamp;
  const mins = Math.floor(diff / 60000);
  const hrs = Math.floor(mins / 60);
  const days = Math.floor(hrs / 24);
  if (days > 0) return `${days}일 전`;
  if (hrs > 0) return `${hrs}시간 전`;
  return `${mins}분 전`;
};

function App() {
  const [summonerName, setSummonerName] = useState('');
  const [result, setResult] = useState(null);
  const [loading, setLoading] = useState(false);
  const [dataMap, setDataMap] = useState({ champ: {}, spell: {}, rune: {} });
  const [selectedQueue, setSelectedQueue] = useState('solo'); // 'solo' or 'flex'

  useEffect(() => {
    const loadGameData = async () => {
      try {
        const [c, s, r] = await Promise.all([
          axios.get(`https://ddragon.leagueoflegends.com/cdn/${LOL_VERSION}/data/ko_KR/champion.json`),
          axios.get(`https://ddragon.leagueoflegends.com/cdn/${LOL_VERSION}/data/ko_KR/summoner.json`),
          axios.get(`https://ddragon.leagueoflegends.com/cdn/${LOL_VERSION}/data/ko_KR/runesReforged.json`)
        ]);
        const cMap = {};
        Object.values(c.data.data).forEach(x => cMap[x.id] = x.name);

        // 데이터에 없는 챔피언(Yunara 등) 강제 매핑 (필요시)
        cMap["Yunara"] = "알 수 없는 챔피언";

        const sMap = {}; Object.values(s.data.data).forEach(x => sMap[x.key] = x.id);
        const rMap = {}; r.data.forEach(st => {
          rMap[st.id] = st.icon;
          st.slots.forEach(sl => sl.runes.forEach(ru => rMap[ru.id] = ru.icon));
        });
        setDataMap({ champ: cMap, spell: sMap, rune: rMap });
      } catch (e) { console.error(e); }
    };
    loadGameData();
  }, []);

  const handleSearch = useCallback(async (name) => {
    const target = name || summonerName;
    if (!target) return;
    setLoading(true);
    try {
      const res = await axios.get(`http://localhost:8080/api/mmr?name=${encodeURIComponent(target)}`);
      setResult(res.data);
      setSummonerName(target);
    } catch (e) { alert("검색 실패"); } finally { setLoading(false); }
  }, [summonerName]);

  const activeQueueData = result?.queues ? result.queues[selectedQueue] : null;
  const matchDetails = activeQueueData?.matchDetails || [];

  const winRate = matchDetails.length > 0
    ? Math.round((matchDetails.filter(m => m.win).length / matchDetails.length) * 100)
    : 0;

  // standardMmr 등은 activeQueueData에서 가져옵니다.
  const stdMmr = activeQueueData?.standardMmr || 1000;
  const scoreResult = activeQueueData?.scoreResult || { currentScore: stdMmr, scoreHistory: [] };
  const currentMmr = scoreResult.currentScore;
  const scoreHistory = scoreResult.scoreHistory;

  const mmrDiff = scoreHistory.length > 0 ? (currentMmr - scoreHistory[0]) : 0;

  const chartData = scoreHistory.map((s, i) => ({
    name: (i + 1) + '판',
    score: s
  }));

  const getChampStats = () => {
    if (!matchDetails || matchDetails.length === 0) return [];
    const stats = {};
    matchDetails.forEach(m => {
      const name = m.championName;
      if (!stats[name]) {
        stats[name] = { name: name, plays: 0, wins: 0, kills: 0, deaths: 0, assists: 0 };
      }
      const s = stats[name];
      s.plays += 1;
      if (m.win) s.wins += 1;
      s.kills += m.kills;
      s.deaths += m.deaths;
      s.assists += m.assists;
    });
    return Object.values(stats).sort((a, b) => b.plays - a.plays).slice(0, 3);
  };

  const topChamps = getChampStats();

  // 현재 랭크 정보 가져오기 (솔랭/자랭 분리에 맞춰)
  const currentRankInfo = selectedQueue === 'solo'
    ? result?.summoner?.soloRank
    : result?.summoner?.flexRank;

  const displayTier = currentRankInfo?.tier || result?.summoner?.tier || "UNRANKED";
  const displayRank = currentRankInfo?.rank || result?.summoner?.rank || "";

  return (
    <div className="app-container">
      <header className="header">
        <h1>LOL 실시간 MMR 분석기</h1>
        <div className="search-box">
          <input
            value={summonerName}
            onChange={e => setSummonerName(e.target.value)}
            onKeyPress={e => e.key === 'Enter' && handleSearch()}
            placeholder="소환사명#KR1"
          />
          <button onClick={() => handleSearch()} disabled={loading}>
            {loading ? '분석 중...' : '검색'}
          </button>
        </div>
      </header>

      {result && (
        <main className="main-content">
          <div className="queue-tabs">
            <button
              className={`tab-btn ${selectedQueue === 'solo' ? 'active' : ''}`}
              onClick={() => setSelectedQueue('solo')}
            >
              솔로랭크
            </button>
            <button
              className={`tab-btn ${selectedQueue === 'flex' ? 'active' : ''}`}
              onClick={() => setSelectedQueue('flex')}
            >
              자유랭크
            </button>
          </div>

          <section className="analysis-dashboard">
            <div className="dash-header">
              <div className="profile-container">
                <div className={`tier-badge-wrap tier-${(displayTier).toLowerCase()}`}>
                  <div className="tier-img-clipper">
                    <img src={getTierImg(displayTier)} alt="tier" className="tier-main-img" />
                  </div>
                </div>
                <div className="user-text-info">
                  <span className="ai-badge">AI 정밀 분석</span>
                  <div className="tier-name">{displayTier} {displayRank}</div>
                  <div className="summoner-name">{result.summoner.name} <span className="level">(Lv.{result.summoner.summonerLevel})</span></div>
                </div>
              </div>

              <div className="mmr-info-panel">
                <div className="std-mmr-text">{displayTier} {displayRank} 표준: {stdMmr}점</div>
                <div className="current-mmr-box">
                  <span className="mmr-value">{currentMmr}</span>
                  <span className="mmr-unit">점</span>
                </div>
                <div className={`mmr-diff-tag ${mmrDiff >= 0 ? 'up' : 'down'}`}>
                  {mmrDiff >= 0 ? '▲' : '▼'} {Math.abs(mmrDiff)} (시작 기준)
                </div>
              </div>
            </div>

            <div className="winrate-section">
              <div className="winrate-labels">
                <span>최근 승률: <strong>{winRate}%</strong></span>
                <span className={mmrDiff < -20 ? 'status-low' : 'status-good'}>
                  상태: {mmrDiff < -20 ? '낮음' : '적정'}
                </span>
              </div>
              <div className="progress-container">
                <div className="progress-bar" style={{ width: `${winRate}%` }}></div>
                <div className="progress-guide-line"></div>
              </div>
            </div>

            <div className="chart-section" style={{ width: '100%', height: 200, marginTop: '20px', padding: '0 20px', boxSizing: 'border-box' }}>
              <h3 style={{ fontSize: '14px', color: '#999', marginBottom: '10px' }}>최근 점수 변동 그래프</h3>
              <ResponsiveContainer width="100%" height="80%">
                <LineChart data={chartData}>
                  <XAxis dataKey="name" hide />
                  <YAxis domain={['auto', 'auto']} hide />
                  <Tooltip
                    contentStyle={{ backgroundColor: '#1e1e2f', border: 'none', borderRadius: '8px', color: '#fff' }}
                    itemStyle={{ color: '#00e5ff' }}
                  />
                  <Line type="monotone" dataKey="score" stroke="#00e5ff" strokeWidth={3} dot={false} />
                </LineChart>
              </ResponsiveContainer>
            </div>

            <div className="top-champs-section">
              {topChamps.map((ch, idx) => (
                <div key={idx} className="champ-stat-card">
                  <img
                    src={getChampImg(ch.name)}
                    alt={ch.name}
                    className="stat-champ-img"
                    onError={(e) => { e.target.src = "https://ddragon.leagueoflegends.com/cdn/img/champion/splash/ext/0.jpg"; }}
                  />
                  <div className="stat-info">
                    <div className="stat-name">{dataMap.champ[ch.name] || ch.name}</div>
                    <div className="stat-detail">
                      <span className="stat-winrate">{Math.round((ch.wins / ch.plays) * 100)}%</span>
                      <span className="stat-plays">({ch.plays}판)</span>
                      <span className="stat-kda">{((ch.kills + ch.assists) / Math.max(1, ch.deaths)).toFixed(2)}:1</span>
                    </div>
                  </div>
                  <div className="mini-progress">
                    <div className="mini-bar" style={{ width: `${(ch.wins / ch.plays) * 100}%` }}></div>
                  </div>
                </div>
              ))}
            </div>

            <div className={`analysis-msg-box ${mmrDiff < 0 ? 'danger' : 'safe'}`}>
              {mmrDiff < 0
                ? `⚠️ 점수 하락세! 최근 성적이 좋지 않아 점수가 떨어지는 추세입니다.`
                : `✅ 점수 상승세! 훌륭한 퍼포먼스로 점수를 꾸준히 올리고 있습니다.`}
            </div>
          </section>

          <div className="match-list">
            {matchDetails.length === 0 ? (
              <div className="no-matches">전적 데이터가 없습니다.</div>
            ) : matchDetails.map((m, idx) => (
              <div key={idx} className={`match-card ${m.win ? 'win' : 'lose'}`}>
                <div className="col-meta">
                  <div className="res-status">{m.win ? '승리' : '패배'}</div>
                  <div className="sub-text">{timeAgo(m.gameEndTimeStamp)}</div>
                  <div className="sub-text">{m.gameDurationMinutes}분</div>
                </div>
                <div className="col-champ">
                  <div className="champ-portrait-wrap">
                    <img
                      className="champ-main-img"
                      src={getChampImg(m.championName)}
                      alt="champ"
                      onError={(e) => { e.target.src = "https://ddragon.leagueoflegends.com/cdn/img/champion/splash/ext/0.jpg"; }}
                    />
                    <div className="icon-column">
                      <img src={getSpellImg(m.spell1Id, dataMap.spell)} alt="s" />
                      <img src={getSpellImg(m.spell2Id, dataMap.spell)} alt="s" />
                    </div>
                    <div className="icon-column">
                      <img className="rune-circle" src={getRuneImg(m.mainRuneId, dataMap.rune)} alt="r" />
                      <img src={getRuneImg(m.subRuneId, dataMap.rune)} alt="r" />
                    </div>
                  </div>
                  <div className="champ-ko-name">{dataMap.champ[m.championName] || m.championName}</div>
                </div>
                {/* ... (생략된 뒷부분은 기존과 동일) ... */}
                <div className="col-stats">
                  <div className="kda-text"><strong>{m.kills}</strong> / <span className="d">{m.deaths}</span> / <strong>{m.assists}</strong></div>
                  <div className="kda-ratio">{((m.kills + m.assists) / Math.max(1, m.deaths)).toFixed(2)}:1 평점</div>
                </div>
                <div className="col-items">
                  <div className="item-layout">
                    {m.items.map((it, i) => (
                      <div key={i} className="item-slot">{getItemImg(it) && <img src={getItemImg(it)} alt="item" />}</div>
                    ))}
                  </div>
                </div>
                <div className="col-players">
                  {[0, 5].map(start => (
                    <div key={start} className="team-group">
                      {m.teamMembers.slice(start, start + 5).map((name, i) => (
                        <div key={i} className="player-row" onClick={() => handleSearch(name)}>
                          <img
                            src={getChampImg(m.teamChamps[start + i])}
                            alt="p"
                            onError={(e) => { e.target.src = "https://ddragon.leagueoflegends.com/cdn/img/champion/splash/ext/0.jpg"; }}
                          />
                          <span className={start === 5 ? 'enemy-name' : 'team-name'}>{name.split('#')[0]}</span>
                        </div>
                      ))}
                    </div>
                  ))}
                </div>
              </div>
            ))}
          </div>
        </main>
      )}
    </div>
  );
}

export default App;