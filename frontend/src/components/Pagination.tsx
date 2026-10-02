import type { Page } from '../api/types'
import { buttonSecondary } from './ui'

interface PaginationProps {
  page: Page<unknown>['page']
  onPageChange: (page: number) => void
}

// The API counts pages from 0; people count from 1
export function Pagination({ page, onPageChange }: PaginationProps) {
  if (page.totalPages <= 1) {
    return null
  }
  return (
    <div className="mt-4 flex items-center justify-between">
      <p className="text-sm text-slate-600">
        Page {page.number + 1} of {page.totalPages} · {page.totalElements} subjects
      </p>
      <div className="flex gap-2">
        <button type="button" className={buttonSecondary} disabled={page.number === 0}
                onClick={() => onPageChange(page.number - 1)}>
          Previous
        </button>
        <button type="button" className={buttonSecondary} disabled={page.number + 1 >= page.totalPages}
                onClick={() => onPageChange(page.number + 1)}>
          Next
        </button>
      </div>
    </div>
  )
}
