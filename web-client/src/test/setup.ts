import '@testing-library/jest-dom/vitest'
import { cleanup } from '@testing-library/react'
import { afterAll, afterEach, beforeAll } from 'vitest'
import { server } from './server'

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
