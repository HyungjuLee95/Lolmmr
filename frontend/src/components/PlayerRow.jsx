import React from 'react';

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
        <div className={`h-full rounded-full ${isBlueTeam ? 'bg-blue-500' : 'bg-red-500'}`} style={{ width: `${(player.damage / player.maxDamage) * 100}%` }} />
      </div>
    </div>
    <div className="w-[40px] text-center text-[10px] md:text-xs text-gray-400">{player.cs}</div>
    <div className="w-[120px] md:w-[150px] flex gap-0.5 justify-end">
      {player.items.map((item, idx) => (
        <div key={idx} className={`w-4 h-4 md:w-5 md:h-5 rounded ${item === 0 ? 'bg-gray-800/50' : 'bg-gray-700'}`}>
          {item !== 0 && <img src={`https://ddragon.leagueoflegends.com/cdn/14.3.1/img/item/${item}.png`} className="w-full h-full rounded" alt="item" />}
        </div>
      ))}
    </div>
  </div>
);

export default PlayerRow;
