import '@testing-library/jest-dom/vitest'
import { cleanup } from '@testing-library/react'
import { afterAll, afterEach, beforeAll } from 'vitest'
import { server } from './server'

// Node 22+ ships a built-in `localStorage` Web Storage global that is enabled
// by default. It requires an on-disk backing file (`--localstorage-file`) and,
// when unset, resolves to a broken object whose `setItem`/`clear` are not
// functions. Worse, it shadows jsdom's own `window.localStorage`, breaking the
// app's session persistence in tests. Swap in a spec-compliant in-memory
// Storage when the resolved global is unusable — keeps tests independent of the
// Node version and the experimental webstorage flag.
if (typeof window.localStorage?.setItem !== 'function') {
  class MemoryStorage implements Storage {
    private store = new Map<string, string>()

    get length() {
      return this.store.size
    }

    key(index: number) {
      return [...this.store.keys()][index] ?? null
    }

    getItem(key: string) {
      return this.store.has(key) ? this.store.get(key)! : null
    }

    setItem(key: string, value: string) {
      this.store.set(key, String(value))
    }

    removeItem(key: string) {
      this.store.delete(key)
    }

    clear() {
      this.store.clear()
    }
  }

  const storage = new MemoryStorage()
  const descriptor = { value: storage, configurable: true, writable: true }
  Object.defineProperty(window, 'localStorage', descriptor)
  Object.defineProperty(globalThis, 'localStorage', descriptor)
}

// Node's fetch needs absolute URLs; config.ts reads this before falling back
// to the relative /api/v1 default used in the browser.
window.__ENV__ = { PUBLIC_API_URL: 'http://localhost:3000/api/v1' }

// jsdom does not implement scrollIntoView, which the dashboards call after
// showing a status message.
Element.prototype.scrollIntoView = () => {}

beforeAll(() => server.listen({ onUnhandledRequest: 'error' }))

afterEach(() => {
  server.resetHandlers()
  cleanup()
  window.localStorage.clear()
  window.history.replaceState({}, '', '/')
})

afterAll(() => server.close())
