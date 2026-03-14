import { create } from 'zustand'

export const useResumeStore = create((set, get) => ({
  // Active resume
  activeResume: null,
  latexSource: '',
  parsedData: null,

  // UI state
  activeTab: 'latex',   // 'latex' | 'match' | 'parsed'
  isSaving: false,
  isCompiling: false,

  // Match result
  matchResult: null,

  // Actions
  setActiveResume: (resume) => set({
    activeResume: resume,
    latexSource: resume?.latexSource ?? '',
    parsedData: resume?.parsedData ?? null,
  }),

  setLatexSource: (src) => set({ latexSource: src }),
  setParsedData: (data) => set({ parsedData: data }),
  setActiveTab: (tab) => set({ activeTab: tab }),
  setSaving: (v) => set({ isSaving: v }),
  setCompiling: (v) => set({ isCompiling: v }),
  setMatchResult: (result) => set({ matchResult: result }),

  updateFromChatEdit: ({ updatedParsedData, updatedLatex }) => {
    set({
      parsedData: updatedParsedData,
      latexSource: updatedLatex,
      activeResume: { ...get().activeResume, parsedData: updatedParsedData, latexSource: updatedLatex },
    })
  },
}))
