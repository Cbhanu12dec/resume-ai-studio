import { useResumeStore } from '@/store/resumeStore'

export default function TabBar({ tabs, active, onChange }) {
  const { matchResult } = useResumeStore()

  return (
    <div className="flex items-center border-b border-line bg-bg1 px-2 shrink-0">
      {tabs.map((tab) => (
        <button
          key={tab.id}
          onClick={() => onChange(tab.id)}
          className={`relative px-5 py-2.5 text-sm font-semibold transition-colors ${
            active === tab.id
              ? 'text-acc border-b-2 border-acc'
              : 'text-muted hover:text-text border-b-2 border-transparent'
          }`}
        >
          {tab.label}
          {/* Dot indicator on Match when there's a result */}
          {tab.id === 'match' && matchResult && active !== 'match' && (
            <span className="absolute top-1.5 right-1.5 w-1.5 h-1.5 bg-success rounded-full" />
          )}
        </button>
      ))}
    </div>
  )
}
