import type { InputHTMLAttributes } from 'react'
import { inputClass } from './ui'

interface FormFieldProps extends InputHTMLAttributes<HTMLInputElement> {
  label: string
  name: string
  // The backend's message for this field, from ProblemDetail "errors"
  error?: string
}

export function FormField({ label, name, error, ...inputProps }: FormFieldProps) {
  return (
    <div>
      <label htmlFor={name} className="block text-sm font-medium text-slate-700">
        {label}
      </label>
      <input
        id={name}
        name={name}
        className={`${inputClass} mt-1 ${error ? 'ring-red-400' : ''}`}
        aria-invalid={error ? true : undefined}
        {...inputProps}
      />
      {error && <p className="mt-1 text-sm text-red-600">{error}</p>}
    </div>
  )
}
