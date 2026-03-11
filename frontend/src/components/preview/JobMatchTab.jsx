import { useResumeStore } from '@/store/resumeStore'
import MatchRing from './jobmatch/MatchRing'
import KeywordChips from './jobmatch/KeywordChips'
import CategoryBars from './jobmatch/CategoryBars'

export default function JobMatchTab() {
  const { matchResult } = useResumeStore()

  if (!matchResult) {
    return (
      <div className="flex items-center justify-center h-full p-8">
        <div className="text-center max-w-sm">
          <div className="text-5xl mb-4">🎯</div>
          <h3 className="text-text font-bold mb-2">Job Match Analysis</h3>
          <p className="text-muted text-sm leading-relaxed">
            Paste any job description in the chat to auto-score your resume against it.
          </p>
        </div>
      </div>
    )
  }

  const { score, breakdown, matchingKeywords, missingKeywords, suggestions } = matchResult

  return (
    <div className="h-full overflow-y-auto p-5 space-y-6">
      {/* Header */}
      <div>
        <h2 className="text-text font-bold text-lg mb-0.5">Job Match Analysis</h2>
        <p className="text-muted text-xs">Paste any job description in chat to re-score</p>
      </div>

      {/* Score + Keywords Row */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-5">
        {/* Score ring */}
        <div className="card flex flex-col items-center py-6">
          <MatchRing score={score} />
        </div>

        {/* Keywords */}
        <div className="card space-y-4">
          <KeywordChips
            title={`Matching (${matchingKeywords?.length ?? 0})`}
            keywords={matchingKeywords ?? []}
            variant="match"
          />
          <KeywordChips
            title={`Missing (${missingKeywords?.length ?? 0})`}
            keywords={missingKeywords ?? []}
            variant="missing"
          />
        </div>
      </div>

      {/* Category breakdown */}
      {breakdown && (
        <div className="card">
          <h3 className="text-xs font-bold uppercase tracking-widest text-acc3 mb-4">Score Breakdown</h3>
          <CategoryBars breakdown={breakdown} />
        </div>
      )}

      {/* Suggestions */}
      {suggestions?.length > 0 && (
        <div className="card">
          <h3 className="text-xs font-bold uppercase tracking-widest text-acc mb-3">AI Suggestions</h3>
          <ul className="space-y-2">
            {suggestions.map((s, i) => (
              <li key={i} className="flex gap-2 text-sm">
                <span className="text-acc mt-0.5 shrink-0">→</span>
                <span className="text-text">{s}</span>
              </li>
            ))}
          </ul>
        </div>
      )}
    </div>
  )
}
