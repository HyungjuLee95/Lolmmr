import React from 'react';

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
        <div className="bg-blue-500 h-full transition-all duration-500" style={{ width: `${myPct}%` }} />
        <div className="bg-red-500 h-full transition-all duration-500" style={{ width: `${oppPct}%` }} />
      </div>
    </div>
  );
};

export default ComparisonBar;
