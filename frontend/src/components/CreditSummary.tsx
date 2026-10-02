// "9 / 20 credit hours" with a progress bar; turns amber when close to the limit
export function CreditSummary({ total, max }: { total: number; max: number }) {
  const percent = Math.min(100, Math.round((total / max) * 100))
  const nearLimit = total >= max - 2

  return (
    <div className="rounded-lg bg-white p-4 shadow-sm ring-1 ring-slate-200">
      <div className="flex items-baseline justify-between">
        <p className="text-sm font-medium text-slate-700">Credit hours</p>
        <p className="text-sm text-slate-600">
          <span className="text-lg font-semibold text-slate-900">{total}</span> / {max}
        </p>
      </div>
      <div className="mt-2 h-2 overflow-hidden rounded-full bg-slate-100">
        <div className={`h-full rounded-full ${nearLimit ? 'bg-amber-500' : 'bg-indigo-600'}`}
             style={{ width: `${percent}%` }} />
      </div>
    </div>
  )
}
