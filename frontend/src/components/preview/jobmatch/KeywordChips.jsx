export default function KeywordChips({ title, keywords, variant }) {
  if (!keywords?.length) return null

  return (
    <div>
      <p className="text-xs font-bold text-muted uppercase tracking-wider mb-2">{title}</p>
      <div className="flex flex-wrap gap-1.5">
        {keywords.map((kw) => (
          <span key={kw} className={variant === 'match' ? 'chip-match' : 'chip-missing'}>
            {kw}
          </span>
        ))}
      </div>
    </div>
  )
}
