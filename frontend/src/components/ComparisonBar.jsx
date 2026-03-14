import React from 'react';

const safeNumber = (value) => {
  const num = Number(value);
  return Number.isFinite(num) ? num : 0;
};

const ComparisonBar = ({ label, myValue, oppValue, isLowerBetter = false }) => {
  const my = safeNumber(myValue);
  const opp = safeNumber(oppValue);

  const total = my + opp;
  const myPct = total === 0 ? 50 : (my / total) * 100;
  const oppPct = total === 0 ? 50 : (opp / total) * 100;

  const diff = my - opp;
  const isTie = my === opp;
  const isMyWin = !isTie && (isLowerBetter ? my < opp : my > opp);
  const isOppWin = !isTie && (isLowerBetter ? opp < my : opp > my);

  const statusText = isTie ? '동일' : isMyWin ? '내 우세' : '상대 우세';

  return (
    <div className="flex flex-col gap-1.5 mb-3">
      <div className="flex items-center justify-between px-1">
        <span className={`text-[10px] md:text-xs ${isMyWin ? 'text-[#F1DAC4] font-bold' : 'text-[#A69CAC]'}`}>
          {my.toLocaleString()}
        </span>

        <div className="flex flex-col items-center min-w-[90px]">
          <span className="text-[10px] md:text-xs text-[#8B86A3]">{label}</span>
          <span
            className={`text-[9px] md:text-[10px] ${
              isTie
                ? 'text-[#8B86A3]'
                : isMyWin
                  ? 'text-[#E8D8C8]'
                  : 'text-[#C8BAD0]'
            }`}
          >
            {statusText}
            {!isTie && (
              <span className="ml-1">
                ({diff > 0 ? '+' : ''}{diff.toLocaleString()})
              </span>
            )}
          </span>
        </div>

        <span className={`text-[10px] md:text-xs ${isOppWin ? 'text-[#A69CAC] font-bold' : 'text-[#A69CAC]'}`}>
          {opp.toLocaleString()}
        </span>
      </div>

      <div className="flex w-full h-1.5 md:h-2 rounded-full overflow-hidden bg-[#474973]">
        <div
          className="bg-[#F1DAC4] h-full transition-all duration-500"
          style={{ width: `${myPct}%` }}
        />
        <div
          className="bg-[#A69CAC] h-full transition-all duration-500"
          style={{ width: `${oppPct}%` }}
        />
      </div>
    </div>
  );
};

export default ComparisonBar;