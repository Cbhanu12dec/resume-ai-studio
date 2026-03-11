import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { resumeApi, matchApi } from '@/services/api'
import { useResumeStore } from '@/store/resumeStore'
import { useChatStore } from '@/store/chatStore'
import toast from 'react-hot-toast'

export const RESUME_KEYS = {
  all: ['resumes'],
  byId: (id) => ['resumes', id],
  latex: (id) => ['resumes', id, 'latex'],
  versions: (id) => ['resumes', id, 'versions'],
  matchHistory: (id) => ['match', id],
}

// ─── Upload ───────────────────────────────────────────────────────────────
export function useUploadResume() {
  const qc = useQueryClient()
  const { setActiveResume } = useResumeStore()

  return useMutation({
    mutationFn: async ({ file, onProgress, onStatus }) => {
      // Upload returns immediately with status=PARSING
      const resume = await resumeApi.upload(file, onProgress)
      // Poll until READY (or ERROR)
      return resumeApi.pollUntilReady(resume.id, onStatus)
    },
    onSuccess: (resume) => {
      qc.invalidateQueries({ queryKey: RESUME_KEYS.all })
      setActiveResume(resume)
      toast.success(`Resume parsed! Found ${resume.parsedData?.name ?? 'unknown'}.`)
    },
  })
}

// ─── Get by ID ────────────────────────────────────────────────────────────
export function useResume(id) {
  return useQuery({
    queryKey: RESUME_KEYS.byId(id),
    queryFn: () => resumeApi.getById(id),
    enabled: !!id,
  })
}

// ─── LaTeX update ─────────────────────────────────────────────────────────
export function useUpdateLatex(resumeId) {
  const qc = useQueryClient()
  const { setSaving } = useResumeStore()

  return useMutation({
    mutationFn: (latexSource) => resumeApi.updateLatex(resumeId, latexSource),
    onMutate: () => setSaving(true),
    onSettled: () => setSaving(false),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: RESUME_KEYS.versions(resumeId) })
      toast.success('LaTeX saved')
    },
  })
}

// ─── Chat edit ────────────────────────────────────────────────────────────
export function useChatEdit(resumeId) {
  const { updateFromChatEdit } = useResumeStore()
  const { addAssistantMessage, setTyping, getHistory } = useChatStore()

  return useMutation({
    mutationFn: (message) => resumeApi.chat(resumeId, message, getHistory()),
    onMutate: () => setTyping(true),
    onSettled: () => setTyping(false),
    onSuccess: (result) => {
      updateFromChatEdit(result)
      addAssistantMessage(result.message)
    },
  })
}

// ─── Match score ──────────────────────────────────────────────────────────
export function useMatchScore(resumeId) {
  const { setMatchResult } = useResumeStore()

  return useMutation({
    mutationFn: (jobDescription) => matchApi.score(resumeId, jobDescription),
    onSuccess: (result) => {
      setMatchResult(result)
      toast.success(`Match scored: ${result.score}%`)
    },
  })
}

// ─── Versions ────────────────────────────────────────────────────────────
export function useVersions(resumeId) {
  return useQuery({
    queryKey: RESUME_KEYS.versions(resumeId),
    queryFn: () => resumeApi.versions(resumeId),
    enabled: !!resumeId,
  })
}

// ─── Restore version ──────────────────────────────────────────────────────
export function useRestoreVersion(resumeId) {
  const qc = useQueryClient()
  const { setActiveResume } = useResumeStore()

  return useMutation({
    mutationFn: (versionId) => resumeApi.restore(resumeId, versionId),
    onSuccess: (resume) => {
      setActiveResume(resume)
      qc.invalidateQueries({ queryKey: RESUME_KEYS.versions(resumeId) })
      toast.success('Version restored')
    },
  })
}
