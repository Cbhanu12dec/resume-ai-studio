import { useResumeStore } from '@/store/resumeStore'

function Section({ title, children }) {
  return (
    <div className="mb-5">
      <h3 className="text-xs font-bold uppercase tracking-widest text-acc mb-2">{title}</h3>
      {children}
    </div>
  )
}

function Field({ label, value }) {
  if (!value) return null
  return (
    <div className="flex gap-2 text-sm mb-1">
      <span className="text-muted shrink-0 w-24">{label}</span>
      <span className="text-text">{value}</span>
    </div>
  )
}

export default function ParsedDataTab() {
  const { parsedData } = useResumeStore()

  if (!parsedData) {
    return (
      <div className="flex items-center justify-center h-full text-muted text-sm">
        No parsed data available
      </div>
    )
  }

  const { name, contact, summary, experience, education, skills, projects, certifications } = parsedData

  return (
    <div className="h-full overflow-y-auto p-5 text-sm">
      <Section title="Contact">
        <Field label="Name"     value={name} />
        <Field label="Email"    value={contact?.email} />
        <Field label="Phone"    value={contact?.phone} />
        <Field label="Location" value={contact?.location} />
        <Field label="LinkedIn" value={contact?.linkedin} />
        <Field label="GitHub"   value={contact?.github} />
      </Section>

      {summary && (
        <Section title="Summary">
          <p className="text-text leading-relaxed">{summary}</p>
        </Section>
      )}

      {experience?.length > 0 && (
        <Section title={`Experience (${experience.length})`}>
          {experience.map((exp, i) => (
            <div key={i} className="mb-3 pb-3 border-b border-line last:border-0">
              <div className="flex justify-between">
                <span className="font-semibold text-text">{exp.title}</span>
                <span className="text-muted text-xs">{exp.startDate} – {exp.endDate || 'Present'}</span>
              </div>
              <span className="text-acc3 text-xs">{exp.company}{exp.location ? ` · ${exp.location}` : ''}</span>
              {exp.bullets?.length > 0 && (
                <ul className="mt-1 space-y-0.5">
                  {exp.bullets.map((b, j) => (
                    <li key={j} className="text-muted text-xs pl-3 relative before:content-['·'] before:absolute before:left-0">
                      {b}
                    </li>
                  ))}
                </ul>
              )}
            </div>
          ))}
        </Section>
      )}

      {education?.length > 0 && (
        <Section title={`Education (${education.length})`}>
          {education.map((edu, i) => (
            <div key={i} className="mb-2">
              <span className="font-semibold text-text">{edu.institution}</span>
              <div className="text-xs text-muted">{edu.degree}{edu.field ? `, ${edu.field}` : ''} · {edu.graduationDate}</div>
            </div>
          ))}
        </Section>
      )}

      {skills?.length > 0 && (
        <Section title={`Skills (${skills.length})`}>
          <div className="flex flex-wrap gap-1.5">
            {skills.map((s, i) => (
              <span key={i} className="px-2 py-0.5 bg-bg3 text-text text-xs rounded border border-line">
                {s}
              </span>
            ))}
          </div>
        </Section>
      )}

      {projects?.length > 0 && (
        <Section title={`Projects (${projects.length})`}>
          {projects.map((p, i) => (
            <div key={i} className="mb-2">
              <span className="font-semibold text-text">{p.name}</span>
              {p.technologies?.length > 0 && (
                <span className="text-acc3 text-xs ml-2">{p.technologies.join(', ')}</span>
              )}
              {p.description && <p className="text-muted text-xs mt-0.5">{p.description}</p>}
            </div>
          ))}
        </Section>
      )}

      {certifications?.length > 0 && (
        <Section title="Certifications">
          <ul className="space-y-1">
            {certifications.map((c, i) => (
              <li key={i} className="text-text text-sm">{c}</li>
            ))}
          </ul>
        </Section>
      )}
    </div>
  )
}
