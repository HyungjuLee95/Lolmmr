import React from 'react';

const normalizePosition = (position) => {
  if (!position) return 'UNK';

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

const safeNumber = (value) => {
  const num = Number(value);
  return Number.isFinite(num) ? num : 0;
};

const PlayerRow = ({ player, isBlueTeam }) => {
  const damage = safeNumber(player?.damage);
  const maxDamage = Math.max(safeNumber(player?.maxDamage), 1);
  const cs = safeNumber(player?.cs);
  const kills = safeNumber(player?.kills);
  const deaths = safeNumber(player?.deaths);
  const assists = safeNumber(player?.assists);
  const items = Array.isArray(player?.items) ? player.items.slice(0, 6) : [];
  const position = normalizePosition(player?.position);
  const damagePercent = Math.max(0, Math.min(100, (damage / maxDamage) * 100));

  return (
    <div
      className={`flex items-center justify-between py-1 px-2 md:py-1.5 md:px-3 hover:bg-slate-900/30 ${
        player?.isMe ? 'bg-slate-900/50 border-l-2 border-l-[#F1DAC4]' : ''
      }`}
    >
      <div className="flex items-center gap-2 w-[140px] md:w-[190px]">
        <div className="relative flex-shrink-0">
          <img
            src={`https://ddragon.leagueoflegends.com/cdn/16.6.1/img/champion/${player?.champion || 'Ahri'}.png`}
            className="w-6 h-6 md:w-8 md:h-8 rounded-full"
            alt={player?.champion || 'champion'}
          />
        </div>

        <div className="min-w-0 flex flex-col">
          <div className="flex items-center gap-1.5 min-w-0">
            <span
              className={`text-[10px] md:text-xs truncate ${
                player?.isMe ? 'text-slate-100 font-bold' : 'text-slate-400'
              }`}
            >
              {player?.name || 'Unknown'}
            </span>

            <span className="px-1.5 py-0.5 rounded bg-slate-700 text-[9px] md:text-[10px] text-slate-400 border border-slate-500/40 flex-shrink-0">
              {position}
            </span>
          </div>

          {player?.isMe && (
            <span className="text-[9px] md:text-[10px] text-slate-100">내 전적</span>
          )}
        </div>
      </div>

      <div className="w-[80px] text-center flex flex-col">
        <span className="text-[10px] md:text-xs font-semibold text-slate-100">
          {kills} / <span className="text-slate-400">{deaths}</span> / {assists}
        </span>
      </div>

      <div className="w-[80px] md:w-[100px] flex flex-col gap-1 items-center hidden sm:flex">
        <span className="text-[9px] md:text-[10px] text-slate-400">{damage.toLocaleString()}</span>
        <div className="w-full bg-slate-700 h-1.5 rounded-full overflow-hidden">
          <div
            className={`h-full rounded-full ${isBlueTeam ? 'bg-[#F1DAC4]' : 'bg-[#A69CAC]'}`}
            style={{ width: `${damagePercent}%` }}
          />
        </div>
      </div>

      <div className="w-[40px] text-center text-[10px] md:text-xs text-slate-400">{cs}</div>

      <div className="w-[120px] md:w-[150px] flex gap-0.5 justify-end">
        {Array.from({ length: 6 }).map((_, idx) => {
          const item = items[idx] || 0;

          return (
            <div
              key={idx}
              className={`w-4 h-4 md:w-5 md:h-5 rounded ${
                item === 0 ? 'bg-slate-700/60' : 'bg-slate-700'
              }`}
            >
              {item !== 0 && (
                <img
                  src={`https://ddragon.leagueoflegends.com/cdn/16.6.1/img/item/${item}.png`}
                  className="w-full h-full rounded"
                  alt="item"
                />
              )}
            </div>
          );
        })}
      </div>
    </div>
  );
};

export default PlayerRow;