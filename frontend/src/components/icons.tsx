interface IconProps {
  active?: boolean;
}

export function CrossIcon() {
  return (
    <svg width="16" height="16" viewBox="0 0 16 16" fill="none">
      <rect x="6.5" y="2" width="3" height="12" rx="1.5" fill="white" fillOpacity="0.9" />
      <rect x="2" y="6.5" width="12" height="3" rx="1.5" fill="white" fillOpacity="0.9" />
    </svg>
  );
}

export function ChartIcon({ active }: IconProps) {
  return (
    <svg width="16" height="16" fill="none" viewBox="0 0 24 24" stroke={active ? '#059669' : '#94A3B8'} strokeWidth={2}>
      <path strokeLinecap="round" strokeLinejoin="round" d="M3 3v18h18M7 16l4-4 4 4 4-6" />
    </svg>
  );
}

export function CashIcon({ active }: IconProps) {
  return (
    <svg width="16" height="16" fill="none" viewBox="0 0 24 24" stroke={active ? '#059669' : '#94A3B8'} strokeWidth={2}>
      <path strokeLinecap="round" strokeLinejoin="round" d="M3 7h18v13H3zM3 7l9-4 9 4M12 12v3" />
    </svg>
  );
}

export function BoxIcon({ active }: IconProps) {
  return (
    <svg width="16" height="16" fill="none" viewBox="0 0 24 24" stroke={active ? '#059669' : '#94A3B8'} strokeWidth={2}>
      <path strokeLinecap="round" strokeLinejoin="round" d="M20 7l-8-4-8 4m16 0v10l-8 4M4 7v10l8 4m0-18v18" />
    </svg>
  );
}

export function LogoutIcon() {
  return (
    <svg width="16" height="16" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
      <path strokeLinecap="round" strokeLinejoin="round" d="M15 3h4a2 2 0 012 2v14a2 2 0 01-2 2h-4M10 17l5-5-5-5M15 12H3" />
    </svg>
  );
}

export function LockIcon() {
  return (
    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={2}>
      <path strokeLinecap="round" strokeLinejoin="round" d="M9 12l2 2 4-4m5.618-4.016A11.955 11.955 0 0112 2.944a11.955 11.955 0 01-8.618 3.04A12.02 12.02 0 003 9c0 5.591 3.824 10.29 9 11.622 5.176-1.332 9-6.03 9-11.622 0-1.042-.133-2.052-.382-3.016z" />
    </svg>
  );
}
