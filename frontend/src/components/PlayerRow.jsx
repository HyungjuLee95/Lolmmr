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
      className={`flex items-center justify-between py-1 px-2 md:py-1.5 md:px-3 hover:bg-black/20 ${
        player?.isMe ? 'bg-black/30 border-l-2 border-l-blue-400' : ''
      }`}
    >
      <div className="flex items-center gap-2 w-[140px] md:w-[190px]">
        <div className="relative flex-shrink-0">
          <img
            src={`https://ddragon.leagueoflegends.com/cdn/14.3.1/img/champion/${player?.champion || 'Ahri'}.png`}
            className="w-6 h-6 md:w-8 md:h-8 rounded-full"
            alt={player?.champion || 'champion'}
          />
        </div>

        <div className="min-w-0 flex flex-col">
          <div className="flex items-center gap-1.5 min-w-0">
            <span
              className={`text-[10px] md:text-xs truncate ${
                player?.isMe ? 'text-white font-bold' : 'text-gray-300'
              }`}
            >
              {player?.name || 'Unknown'}
            </span>

            <span className="px-1.5 py-0.5 rounded bg-gray-800 text-[9px] md:text-[10px] text-gray-400 border border-gray-700 flex-shrink-0">
              {position}
            </span>
          </div>

          {player?.isMe && (
            <span className="text-[9px] md:text-[10px] text-blue-400">내 전적</span>
          )}
        </div>
      </div>

      <div className="w-[80px] text-center flex flex-col">
        <span className="text-[10px] md:text-xs font-semibold text-gray-200">
          {kills} / <span className="text-red-400">{deaths}</span> / {assists}
        </span>
      </div>

      <div className="w-[80px] md:w-[100px] flex flex-col gap-1 items-center hidden sm:flex">
        <span className="text-[9px] md:text-[10px] text-gray-400">{damage.toLocaleString()}</span>
        <div className="w-full bg-gray-800 h-1.5 rounded-full overflow-hidden">
          <div
            className={`h-full rounded-full ${isBlueTeam ? 'bg-blue-500' : 'bg-red-500'}`}
            style={{ width: `${damagePercent}%` }}
          />
        </div>
      </div>

      <div className="w-[40px] text-center text-[10px] md:text-xs text-gray-400">{cs}</div>

      <div className="w-[120px] md:w-[150px] flex gap-0.5 justify-end">
        {Array.from({ length: 6 }).map((_, idx) => {
          const item = items[idx] || 0;

          return (
            <div
              key={idx}
              className={`w-4 h-4 md:w-5 md:h-5 rounded ${
                item === 0 ? 'bg-gray-800/50' : 'bg-gray-700'
              }`}
            >
              {item !== 0 && (
                <img
                  src={`https://ddragon.leagueoflegends.com/cdn/14.3.1/img/item/${item}.png`}
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