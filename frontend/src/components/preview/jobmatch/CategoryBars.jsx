import { motion } from 'framer-motion'

const CATEGORY_META = {
  technical:  { label: 'Technical',  color: '#06b6d4' },
  experience: { label: 'Experience', color: '#10b981' },
  education:  { label: 'Education',  color: '#7c3aed' },
  keywords:   { label: 'Keywords',   color: '#e94560' },
  industry:   { label: 'Industry',   color: '#f59e0b' },
}

export default function CategoryBars({ breakdown }) {
  return (
    <div className="space-y-3">
      {Object.entries(breakdown).map(([key, value]) => {
        const meta = CATEGORY_META[key] ?? { label: key, color: '#6b6b9a' }
        return (
          <div key={key} className="flex items-center gap-3">
            <span className="text-muted text-xs w-20 shrink-0">{meta.label}</span>
            <div className="flex-1 bg-bg3 rounded-full h-2 overflow-hidden">
              <motion.div
                className="h-2 rounded-full"
                style={{ backgroundColor: meta.color }}
                initial={{ width: 0 }}
                animate={{ width: `${value}%` }}
                transition={{ duration: 0.8, ease: 'easeOut', delay: 0.1 }}
              />
            </div>
            <span
              className="text-xs font-bold w-9 text-right shrink-0"
              style={{ color: meta.color }}
            >
              {value}%
            </span>
          </div>
        )
      })}
    </div>
  )
}
