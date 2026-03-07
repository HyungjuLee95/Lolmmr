import React from 'react';

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
    style={{ width: '1em', height: '1em', flexShrink: 0 }}
  >
    {children}
  </svg>
);

export const Search = ({ className }) => <Icon className={className}><circle cx="11" cy="11" r="8" /><line x1="21" y1="21" x2="16.65" y2="16.65" /></Icon>;
export const Trophy = ({ className }) => <Icon className={className}><path d="M8 21h8" /><path d="M12 17v4" /><path d="M7 4h10v3a5 5 0 0 1-10 0z" /><path d="M5 5H3a3 3 0 0 0 3 3" /><path d="M19 5h2a3 3 0 0 1-3 3" /></Icon>;
export const ChevronRight = ({ className }) => <Icon className={className}><polyline points="9 18 15 12 9 6" /></Icon>;
export const ChevronDown = ({ className }) => <Icon className={className}><polyline points="6 9 12 15 18 9" /></Icon>;
export const Activity = ({ className }) => <Icon className={className}><polyline points="22 12 18 12 15 21 9 3 6 12 2 12" /></Icon>;
export const Flame = ({ className }) => <Icon className={className}><path d="M8.5 14.5A4.5 4.5 0 1 0 15 18c0-1.6-1-2.7-2-3.6 0 0 .5 2.1-1 2.6 0 0-1.2-.5-1-2.5-1.5 1-2.5 2.2-2.5 3.9Z" /><path d="M12 2s3 2.5 3 5a3 3 0 0 1-6 0c0-2.5 3-5 3-5Z" /></Icon>;
export const Target = ({ className }) => <Icon className={className}><circle cx="12" cy="12" r="10" /><circle cx="12" cy="12" r="6" /><circle cx="12" cy="12" r="2" /></Icon>;
export const AlertCircle = ({ className }) => <Icon className={className}><circle cx="12" cy="12" r="10" /><line x1="12" y1="8" x2="12" y2="12" /><line x1="12" y1="16" x2="12.01" y2="16" /></Icon>;
export const ThumbsUp = ({ className }) => <Icon className={className}><path d="M14 9V5a3 3 0 0 0-3-3l-1 4-3 3v11h11l3-8V9z" /><path d="M7 22H4a2 2 0 0 1-2-2v-8a2 2 0 0 1 2-2h3" /></Icon>;
export const Swords = ({ className }) => <Icon className={className}><path d="M14.5 17.5 21 11l-2-2-6.5 6.5" /><path d="m3 3 2 2" /><path d="M3 21l6.5-6.5" /><path d="m9-9L11 3 3 11l2 2 6.5-6.5" /></Icon>;
export const Users = ({ className }) => <Icon className={className}><path d="M16 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2" /><circle cx="8.5" cy="7" r="4" /><path d="M20 8v6" /><path d="M23 11h-6" /></Icon>;
