import { useEffect } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { useResume } from '@/hooks/useResume'
import { useResumeStore } from '@/store/resumeStore'
import { useChatStore } from '@/store/chatStore'
import TopBar from '@/components/TopBar'
import ChatPanel from '@/components/chat/ChatPanel'
import PreviewPanel from '@/components/preview/PreviewPanel'

export default function StudioPage() {
  const { resumeId } = useParams()
  const navigate = useNavigate()
  const { setActiveResume } = useResumeStore()
  const { clearMessages } = useChatStore()

  const { data: resume, isLoading, isError } = useResume(resumeId)

  useEffect(() => {
    if (resume) {
      setActiveResume(resume)
      clearMessages()
    }
  }, [resume, setActiveResume, clearMessages])

  if (isLoading) {
    return (
      <div className="min-h-screen bg-bg0 grid-bg flex items-center justify-center">
        <div className="flex flex-col items-center gap-4">
          <div className="w-12 h-12 rounded-full border-4 border-acc/30 border-t-acc animate-spin" />
          <p className="text-muted text-sm">Loading resume…</p>
        </div>
      </div>
    )
  }

  if (isError) {
    return (
      <div className="min-h-screen bg-bg0 grid-bg flex items-center justify-center">
        <div className="text-center">
          <p className="text-acc text-lg font-bold mb-2">Resume not found</p>
          <button onClick={() => navigate('/')} className="btn-primary">
            Upload New Resume
          </button>
        </div>
      </div>
    )
  }

  return (
    <div className="flex flex-col h-screen bg-bg0 overflow-hidden">
      <TopBar />
      <div className="flex flex-1 min-h-0">
        {/* Left: Chat Panel */}
        <div className="w-72 xl:w-80 shrink-0 flex flex-col border-r border-line bg-bg1">
          <ChatPanel resumeId={resumeId} />
        </div>

        {/* Right: Preview Panel */}
        <div className="flex-1 min-w-0 flex flex-col">
          <PreviewPanel resumeId={resumeId} />
        </div>
      </div>
    </div>
  )
}
