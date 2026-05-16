import { API_URL } from './config'

export default function App() {
  return (
    <main style={{ fontFamily: 'sans-serif', padding: '2rem' }}>
      <h1>CareDesk</h1>
      <p>Unified clinic management platform.</p>
      <p style={{ color: '#666', fontSize: '0.875rem' }}>
        API: <code>{API_URL}</code>
      </p>
    </main>
  )
}
