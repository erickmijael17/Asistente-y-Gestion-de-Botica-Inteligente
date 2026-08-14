interface BadgeProps {
  label: string;
  variant: 'green' | 'red' | 'yellow' | 'blue' | 'gray';
}

const styles: Record<BadgeProps['variant'], string> = {
  green: 'bg-emerald-50 text-emerald-700 border-emerald-200',
  red: 'bg-red-50 text-red-700 border-red-200',
  yellow: 'bg-amber-50 text-amber-700 border-amber-200',
  blue: 'bg-blue-50 text-blue-700 border-blue-200',
  gray: 'bg-slate-100 text-slate-500 border-slate-200',
};

export function Badge({ label, variant }: BadgeProps) {
  return (
    <span className={`inline-flex items-center px-2 py-0.5 text-xs font-medium border rounded-full ${styles[variant]}`}>
      {label}
    </span>
  );
}
