import React, { useEffect, useState } from 'react'
import { Client, IMessage } from '@stomp/stompjs'
import axios from 'axios'

type Message = {
  roomId: number
  senderId: string
  content: string
  ts: number
}


const ROOM_ID = 1

type Contact = {
  id: number
  username: string
  status: 'PENDING' | 'ACCEPTED' | 'BLOCKED'
}

export default function App() {
  const [messages, setMessages] = useState<Message[]>([])
  const [draft, setDraft] = useState('')
  const [client, setClient] = useState<Client | null>(null)
  const [connected, setConnected] = useState(false)
  const [username, setUsername] = useState<string>(() => localStorage.getItem('tm.username') || '')
  const [password, setPassword] = useState<string>('')
  const [mode, setMode] = useState<'login' | 'register'>('login')
  const [error, setError] = useState<string | null>(null)
  const [accessToken, setAccessToken] = useState<string>(() => localStorage.getItem('tm.access') || '')
  const [refreshToken, setRefreshToken] = useState<string>(() => localStorage.getItem('tm.refresh') || '')

  // Contacts state
  const [contacts, setContacts] = useState<Contact[]>([])
  const [requests, setRequests] = useState<Contact[]>([])
  const [newContact, setNewContact] = useState('')
  const [contactsError, setContactsError] = useState<string | null>(null)

  // Attach Authorization header for REST
  useEffect(() => {
    if (accessToken) {
      axios.defaults.headers.common['Authorization'] = `Bearer ${accessToken}`
    } else {
      delete axios.defaults.headers.common['Authorization']
    }
  }, [accessToken])

  // Axios 401 -> try refresh
  useEffect(() => {
    const id = axios.interceptors.response.use(
      undefined,
      async (err) => {
        const status = err?.response?.status
        const original = err?.config || {}
        const url: string | undefined = original?.url
        // avoid infinite loop on auth endpoints
        const isAuthCall = !!url && url.includes('/api/auth/')
        // Some servers may return 403 (not 401) for missing/expired access tokens
        if ((status === 401 || status === 403) && !isAuthCall && refreshToken) {
          try {
            // Explicitly send the refresh token in the Authorization header to avoid using a stale access token header
            const res = await axios.post(
              '/api/auth/refresh',
              { refreshToken },
              { headers: { Authorization: `Bearer ${refreshToken}` } }
            )
            const newAccess = res.data.accessToken as string
            if (!newAccess) throw new Error('no access token returned')
            setAccessToken(newAccess)
            localStorage.setItem('tm.access', newAccess)
            // retry original request with the new access token
            ;(original as any).headers = { ...((original as any).headers || {}), Authorization: `Bearer ${newAccess}` }
            return axios.request(original)
          } catch (e) {
            // refresh failed -> logout
            setAccessToken('')
            setRefreshToken('')
            localStorage.removeItem('tm.access')
            localStorage.removeItem('tm.refresh')
            localStorage.removeItem('tm.username')
          }
        }
        return Promise.reject(err)
      }
    )
    return () => axios.interceptors.response.eject(id)
  }, [refreshToken])

  // Fetch contacts after login and on changes triggered by actions
  const refreshContacts = async () => {
    if (!accessToken) return
    try {
      const [c, r] = await Promise.all([
        axios.get('/api/contacts'),
        axios.get('/api/contacts/requests')
      ])
      setContacts(c.data as Contact[])
      setRequests(r.data as Contact[])
      setContactsError(null)
    } catch (e: any) {
      const msg = e?.response?.data?.error || 'Failed to load contacts'
      setContactsError(msg)
    }
  }

  useEffect(() => {
    refreshContacts()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [accessToken])

  // STOMP connection
  useEffect(() => {
    if (!accessToken) return
    const url = `${location.protocol === 'https:' ? 'wss' : 'ws'}://${location.hostname}:8080/ws`
    const c = new Client({
      brokerURL: url,
      reconnectDelay: 2000,
      debug: (str) => console.log(str),
      connectHeaders: { Authorization: `Bearer ${accessToken}` }
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
  }, [accessToken])

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
    const name = username.trim().toLowerCase()
    try {
      setError(null)
      if (!name || !password) { setError('Enter username and password'); return }
      // Basic client-side validation to match server rules
      if (!/^[a-z0-9._-]{3,50}$/.test(name)) {
        setError('Username must be 3-50 chars: a-z, 0-9, . _ -');
        return
      }
      const path = mode === 'register' ? '/api/auth/register' : '/api/auth/login'
      const res = await axios.post(path, { username: name, password })
      const access = res.data.accessToken as string
      const refresh = res.data.refreshToken as string
      setAccessToken(access)
      setRefreshToken(refresh)
      localStorage.setItem('tm.access', access)
      localStorage.setItem('tm.refresh', refresh)
      localStorage.setItem('tm.username', name)
    } catch (err: any) {
      const msg = err?.response?.data?.error || err?.response?.data?.message || 'Authentication failed'
      setError(msg)
    }
  }

  const logout = () => {
    setAccessToken('')
    setRefreshToken('')
    localStorage.removeItem('tm.access')
    localStorage.removeItem('tm.refresh')
    localStorage.removeItem('tm.username')
  }

  const addContact = async () => {
    const u = newContact.trim()
    if (!u) return
    try {
      setContactsError(null)
      await axios.post('/api/contacts', { user: u })
      setNewContact('')
      await refreshContacts()
    } catch (e: any) {
      const msg = e?.response?.data?.error || 'Failed to add contact'
      setContactsError(msg)
    }
  }

  const acceptRequest = async (u: string) => {
    try {
      await axios.post('/api/contacts/accept', { user: u })
      await refreshContacts()
    } catch (e) {
      // ignore; UI will refetch
    }
  }

  const removeContact = async (u: string) => {
    try {
      await axios.delete('/api/contacts', { params: { user: u } })
      await refreshContacts()
    } catch (e) {
      // ignore
    }
  }

  if (!accessToken) {
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
        <p style={{ color: '#666' }}>Account uses access + refresh tokens. Passwords are hashed on the server.</p>
      </div>
    )
  }

  return (
    <div style={{ maxWidth: 1000, margin: '2rem auto', fontFamily: 'system-ui' }}>
      <h1>TurtleMessenger</h1>
      <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 8 }}>
        <div>User: <b>{username}</b></div>
        <button onClick={logout}>Logout</button>
      </div>

      <div style={{ display: 'grid', gridTemplateColumns: '260px 1fr', gap: 16 }}>
        {/* Sidebar: Contacts */}
        <div style={{ border: '1px solid #ddd', padding: 12, height: 480, overflow: 'auto' }}>
          <h3 style={{ marginTop: 0 }}>Contacts</h3>
          <div style={{ display: 'flex', gap: 6, marginBottom: 8 }}>
            <input
              value={newContact}
              onChange={(e) => setNewContact(e.target.value)}
              onKeyDown={(e) => { if (e.key === 'Enter') addContact() }}
              placeholder="Add by username or ID"
              style={{ flex: 1, padding: 6 }}
            />
            <button onClick={addContact}>Add</button>
          </div>
          {contactsError && <div style={{ color: '#d55', marginBottom: 8 }}>{contactsError}</div>}
          <div>
            {contacts.length === 0 && <div style={{ color: '#777' }}>No contacts yet.</div>}
            {contacts.map((c) => (
              <div key={c.id} style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', padding: '6px 0', borderBottom: '1px solid #eee' }}>
                <div>
                  <b>{c.username}</b>
                  <span style={{ color: '#888', marginLeft: 6 }}>[{c.status}]</span>
                </div>
                <button onClick={() => removeContact(c.username)} title="Remove">✕</button>
              </div>
            ))}
          </div>

          <h4>Incoming requests</h4>
          <div>
            {requests.length === 0 && <div style={{ color: '#777' }}>None</div>}
            {requests.map((r) => (
              <div key={r.id} style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', padding: '6px 0', borderBottom: '1px solid #eee' }}>
                <div>
                  <b>{r.username}</b>
                  <span style={{ color: '#888', marginLeft: 6 }}>[{r.status}]</span>
                </div>
                <div style={{ display: 'flex', gap: 6 }}>
                  <button onClick={() => acceptRequest(r.username)}>Accept</button>
                  <button onClick={() => removeContact(r.username)}>Decline</button>
                </div>
              </div>
            ))}
          </div>
        </div>

        {/* Main: Chat */}
        <div>
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
      </div>
    </div>
  )
}
