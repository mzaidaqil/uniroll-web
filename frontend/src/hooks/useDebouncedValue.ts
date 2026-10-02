import { useEffect, useState } from 'react'

// Returns `value` only after it has stopped changing for `delayMs`.
// Typing "prog" then sends one search request instead of four.
export function useDebouncedValue<T>(value: T, delayMs = 300): T {
  const [debounced, setDebounced] = useState(value)

  useEffect(() => {
    const timer = setTimeout(() => setDebounced(value), delayMs)
    return () => clearTimeout(timer) // a new keystroke cancels the pending update
  }, [value, delayMs])

  return debounced
}
