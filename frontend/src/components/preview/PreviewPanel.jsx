import { useResumeStore } from '@/store/resumeStore'
import TabBar from './TabBar'
import LatexEditor from './LatexEditor'
import PdfPreview from './PdfPreview'
import JobMatchTab from './JobMatchTab'
import ParsedDataTab from './ParsedDataTab'

const TABS = [
  { id: 'latex',   label: 'LaTeX' },
  { id: 'preview', label: 'Preview' },
  { id: 'match',   label: 'Match' },
  { id: 'parsed',  label: 'Parsed' },
]

export default function PreviewPanel({ resumeId }) {
  const { activeTab, setActiveTab } = useResumeStore()

  return (
    <div className="flex flex-col h-full">
      <TabBar tabs={TABS} active={activeTab} onChange={setActiveTab} />

      <div className="flex-1 min-h-0 overflow-hidden">
        {activeTab === 'latex'   && <LatexEditor resumeId={resumeId} />}
        {activeTab === 'preview' && <PdfPreview  resumeId={resumeId} />}
        {activeTab === 'match'   && <JobMatchTab  resumeId={resumeId} />}
        {activeTab === 'parsed'  && <ParsedDataTab />}
      </div>
    </div>
  )
}
