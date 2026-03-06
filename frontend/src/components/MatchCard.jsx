import React, { useState } from 'react';
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
import { Activity, AlertCircle, ChevronDown, ChevronRight, Swords, Target, ThumbsUp, Users } from './icons';
import ComparisonBar from './ComparisonBar';
import PlayerRow from './PlayerRow';

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
          <div className="border-t border-gray-700 my-1" />
          <div>{match.gameDuration}</div>
        </div>

        <div className="flex-1 p-3 flex items-center gap-4">
          <div className="flex items-center gap-2">
            <img src={`https://ddragon.leagueoflegends.com/cdn/14.3.1/img/champion/${match.summary.champion}.png`} alt={match.summary.champion} className="w-10 h-10 sm:w-12 sm:h-12 rounded-full border border-gray-700" />
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
                {item !== 0 && <img src={`https://ddragon.leagueoflegends.com/cdn/14.3.1/img/item/${item}.png`} alt="item" className="w-full h-full rounded" />}
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
                <div className="flex flex-col">{match.overview.blueTeam.players.map((p) => <PlayerRow key={p.id} player={p} isBlueTeam />)}</div>
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
                      <img src={`https://ddragon.leagueoflegends.com/cdn/14.3.1/img/champion/${p.champion}.png`} className="w-6 h-6 md:w-8 md:h-8 rounded-full" alt="" />
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

export default MatchCard;
