import React, { useEffect, useMemo, useState } from 'react'
import { Client, IMessage } from '@stomp/stompjs'
import axios from 'axios'

type Message = {
  roomId: number
  senderId: string
  content: string
  ts: number
}

const ROOM_ID = 1

export default function App() {
  const [messages, setMessages] = useState<Message[]>([])
  const [draft, setDraft] = useState('')
  const [client, setClient] = useState<Client | null>(null)
  const [connected, setConnected] = useState(false)
  const [username, setUsername] = useState<string>(() => localStorage.getItem('tm.username') || '')
  const [password, setPassword] = useState<string>('')
  const [mode, setMode] = useState<'login' | 'register'>('login')
  const [error, setError] = useState<string | null>(null)
  const [token, setToken] = useState<string>(() => localStorage.getItem('tm.token') || '')

  useEffect(() => {
    if (token) {
      axios.defaults.headers.common['Authorization'] = `Bearer ${token}`
    } else {
      delete axios.defaults.headers.common['Authorization']
    }
  }, [token])

  useEffect(() => {
    if (!token) return
    const url = `${location.protocol === 'https:' ? 'wss' : 'ws'}://${location.hostname}:8080/ws`
    const c = new Client({
      brokerURL: url,
      reconnectDelay: 2000,
      debug: (str) => console.log(str),
      connectHeaders: { Authorization: `Bearer ${token}` }
    })
    c.onConnect = () => {
      setConnected(true)
      // Subscribe first to avoid missing any real-time messages
      c.subscribe(`/topic/rooms.${ROOM_ID}`, (frame: IMessage) => {
        const msg: Message = JSON.parse(frame.body)
        setMessages((prev) => [...prev, msg])
      })
      // Then merge recent history without overwriting live messages
      axios.get(`/api/rooms/${ROOM_ID}/messages?size=50`).then(res => {
        const incoming = res.data as Message[]
        setMessages((prev) => {
          const key = (m: Message) => `${m.ts}|${m.senderId}|${m.content}`
          const seen = new Set(prev.map(key))
          const merged = [...prev]
          for (const m of incoming) { if (!seen.has(key(m))) merged.push(m) }
          merged.sort((a,b) => a.ts - b.ts)
          return merged
        })
      }).catch(() => {})
    }
    c.onStompError = (frame) => {
      console.error('Broker reported error:', frame.headers['message'], frame.body)
    }
    c.onWebSocketClose = () => setConnected(false)
    c.activate()
    setClient(c)
    return () => { c.deactivate() }
  }, [token])

  const send = () => {
    if (!client || !connected) return
    const text = draft.trim()
    if (!text) return
    const payload: Message = { roomId: ROOM_ID, senderId: username || 'me', content: text, ts: Date.now() }
    client.publish({ destination: `/app/rooms.${ROOM_ID}.send`, body: JSON.stringify(payload) })
    setDraft('')
  }

  const doLogin = async (e: React.FormEvent) => {
    e.preventDefault()
    const name = username.trim()
    try {
      setError(null)
      if (!name || !password) { setError('Enter username and password'); return }
      const path = mode === 'register' ? '/api/auth/register' : '/api/auth/login'
      const res = await axios.post(path, { username: name, password })
      const t = res.data.token as string
      setToken(t)
      localStorage.setItem('tm.token', t)
      localStorage.setItem('tm.username', name)
    } catch (err: any) {
      setError(err?.response?.data?.error || 'Authentication failed')
    }
  }

  if (!token) {
    return (
      <div style={{ maxWidth: 480, margin: '4rem auto', fontFamily: 'system-ui' }}>
        <h1>TurtleMessenger</h1>
        <form onSubmit={doLogin} style={{ display: 'grid', gap: 8 }}>
          <input
            value={username}
            onChange={(e) => setUsername(e.target.value)}
            placeholder="Choose a username"
            style={{ flex: 1, padding: 8 }}
          />
          <input
            type="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            placeholder={mode === 'register' ? 'Create a password (min 6 chars)' : 'Password'}
            style={{ flex: 1, padding: 8 }}
          />
          <button type="submit">Enter</button>
          {error && <div style={{ color: '#d55' }}>{error}</div>}
        </form>
        <div style={{ marginTop: 8 }}>
          <button onClick={() => setMode(mode === 'login' ? 'register' : 'login')}>
            {mode === 'login' ? 'Create an account' : 'I already have an account'}
          </button>
        </div>
        <p style={{ color: '#666' }}>Account uses a JWT. Passwords are hashed on the server.</p>
      </div>
    )
  }

  return (
    <div style={{ maxWidth: 720, margin: '2rem auto', fontFamily: 'system-ui' }}>
      <h1>TurtleMessenger</h1>
      <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 8 }}>
        <div>User: <b>{username}</b></div>
        <button onClick={() => { setToken(''); localStorage.removeItem('tm.token'); }}>Logout</button>
      </div>
      <div style={{ border: '1px solid #ddd', padding: 16, height: 360, overflow: 'auto' }}>
        {messages.map((m, i) => (
          <div key={i}>
            <b>{m.senderId}:</b> {m.content} <small style={{ color: '#888' }}>{new Date(m.ts).toLocaleTimeString()}</small>
          </div>
        ))}
      </div>
      <div style={{ display: 'flex', gap: 8, marginTop: 12 }}>
        <input
          value={draft}
          onChange={(e) => setDraft(e.target.value)}
          onKeyDown={(e) => { if (e.key === 'Enter') send() }}
          placeholder="Type a message"
          style={{ flex: 1, padding: 8 }}
        />
        <button onClick={send} disabled={!connected}>Send</button>
      </div>
      <p style={{ color: connected ? '#2a7' : '#d55' }}>
        {connected ? 'Connected' : 'Disconnected'} to room #{ROOM_ID} via STOMP/WebSocket.
      </p>
    </div>
  )
}
