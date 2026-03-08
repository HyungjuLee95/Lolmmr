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
        <span className={`text-[10px] md:text-xs ${isMyWin ? 'text-blue-400 font-bold' : 'text-gray-400'}`}>
          {my.toLocaleString()}
        </span>

        <div className="flex flex-col items-center min-w-[90px]">
          <span className="text-[10px] md:text-xs text-gray-500">{label}</span>
          <span
            className={`text-[9px] md:text-[10px] ${
              isTie
                ? 'text-gray-500'
                : isMyWin
                  ? 'text-blue-300'
                  : 'text-red-300'
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

        <span className={`text-[10px] md:text-xs ${isOppWin ? 'text-red-400 font-bold' : 'text-gray-400'}`}>
          {opp.toLocaleString()}
        </span>
      </div>

      <div className="flex w-full h-1.5 md:h-2 rounded-full overflow-hidden bg-gray-800">
        <div
          className="bg-blue-500 h-full transition-all duration-500"
          style={{ width: `${myPct}%` }}
        />
        <div
          className="bg-red-500 h-full transition-all duration-500"
          style={{ width: `${oppPct}%` }}
        />
      </div>
    </div>
  );
};

export default ComparisonBar;