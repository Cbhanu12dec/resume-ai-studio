import { useEffect, useState } from 'react'
import { motion } from 'framer-motion'

const SIZE = 140
const STROKE = 12
const R = (SIZE - STROKE) / 2
const CIRCUMFERENCE = 2 * Math.PI * R

function scoreColor(score) {
  if (score >= 80) return '#10b981'
  if (score >= 60) return '#f59e0b'
  return '#e94560'
}

export default function MatchRing({ score }) {
  const [animated, setAnimated] = useState(0)

  useEffect(() => {
    const timeout = setTimeout(() => setAnimated(score), 100)
    return () => clearTimeout(timeout)
  }, [score])

  const offset = CIRCUMFERENCE - (animated / 100) * CIRCUMFERENCE
  const color = scoreColor(score)

  return (
    <div className="flex flex-col items-center gap-3">
      <div className="relative" style={{ width: SIZE, height: SIZE }}>
        <svg width={SIZE} height={SIZE} viewBox={`0 0 ${SIZE} ${SIZE}`}>
          {/* Background track */}
          <circle
            cx={SIZE / 2}
            cy={SIZE / 2}
            r={R}
            fill="none"
            stroke="var(--line)"
            strokeWidth={STROKE}
          />
          {/* Score arc */}
          <motion.circle
            cx={SIZE / 2}
            cy={SIZE / 2}
            r={R}
            fill="none"
            stroke={color}
            strokeWidth={STROKE}
            strokeLinecap="round"
            strokeDasharray={CIRCUMFERENCE}
            initial={{ strokeDashoffset: CIRCUMFERENCE }}
            animate={{ strokeDashoffset: offset }}
            transition={{ duration: 1.2, ease: 'easeOut' }}
            transform={`rotate(-90 ${SIZE / 2} ${SIZE / 2})`}
          />
        </svg>

        {/* Center text */}
        <div className="absolute inset-0 flex flex-col items-center justify-center">
          <motion.span
            className="text-3xl font-black"
            style={{ color }}
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            transition={{ delay: 0.5 }}
          >
            {score}%
          </motion.span>
          <span className="text-muted text-xs font-bold uppercase tracking-wider">Match</span>
        </div>
      </div>

      {/* Label */}
      <p className="text-text text-sm font-semibold">
        {score >= 80 ? 'Excellent match!' : score >= 60 ? 'Good match' : 'Needs improvement'}
      </p>
    </div>
  )
}
