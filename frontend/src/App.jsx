import { useState, useEffect, useRef } from 'react'
import { Send, FolderOpen, Database, Terminal, CheckCircle } from 'lucide-react'
import { motion } from 'framer-motion'
import axios from 'axios'

function App() {
  const [activeTab, setActiveTab] = useState('chat')
  const [query, setQuery] = useState('')
  const [messages, setMessages] = useState([{ role: 'agent', content: 'Welcome! Load a project to begin.' }])
  const [loading, setLoading] = useState(false)

  // Data State
  const [dbData, setDbData] = useState(null)
  const [dbLoading, setDbLoading] = useState(false)
  const [dbError, setDbError] = useState(null)

  // Ingest State
  const [ingestStatus, setIngestStatus] = useState(null)
  const fileInputRef = useRef(null)

  // Chat Logic
  const sendMessage = async () => {
    if (!query.trim()) return
    const userMsg = { role: 'user', content: query }
    setMessages(prev => [...prev, userMsg])
    setQuery('')
    setLoading(true)

    try {
      const res = await axios.post('/api/chat', query, { headers: { 'Content-Type': 'text/plain' } })
      setMessages(prev => [...prev, { role: 'agent', content: res.data }])
    } catch (err) {
      setMessages(prev => [...prev, { role: 'agent', content: `Error: ${err.message}` }])
    } finally {
      setLoading(false)
    }
  }

  // Fetch Data Logic
  useEffect(() => {
    if (activeTab === 'db') {
      const fetchData = async () => {
        setDbLoading(true)
        setDbError(null)
        try {
          const res = await axios.get('/api/vectors/content')
          setDbData(res.data)
        } catch (err) {
          setDbError(err.message || 'Failed to fetch data')
        } finally {
          setDbLoading(false)
        }
      }
      fetchData()
    }
  }, [activeTab])

  // Ingest Handler
  const handleFolderSelect = async (e) => {
    const files = e.target.files
    if (!files || files.length === 0) return

    setIngestStatus('Uploading...')
    const formData = new FormData()
    for (let i = 0; i < files.length; i++) {
      formData.append("files", files[i])
    }

    try {
      const res = await axios.post('/api/ingest/upload', formData, {
        headers: { 'Content-Type': 'multipart/form-data' }
      })
      setIngestStatus(res.data)
      // Switch to chat after success
      setTimeout(() => setActiveTab('chat'), 1500)
    } catch (err) {
      setIngestStatus(`Error: ${err.message}`)
    } finally {
      // Reset input so change event works again
      e.target.value = null
    }
  }

  return (
    <div className="flex h-screen bg-background text-text font-sans selection:bg-primary/30">
      {/* Sidebar */}
      <div className="w-64 bg-surface border-r border-slate-700 flex flex-col p-4 gap-4 shrink-0">
        <h1 className="text-xl font-bold flex items-center gap-2 text-primary">
          <Terminal size={24} /> CodeIntel
        </h1>

        <nav className="flex flex-col gap-2">
          <TabButton icon={<Send size={18} />} label="Chat" active={activeTab === 'chat'} onClick={() => setActiveTab('chat')} />
          <TabButton icon={<Database size={18} />} label="Inspect DB" active={activeTab === 'db'} onClick={() => setActiveTab('db')} />
          <TabButton icon={<FolderOpen size={18} />} label="Ingest" active={activeTab === 'ingest'} onClick={() => setActiveTab('ingest')} />
        </nav>

        <div className="mt-auto text-xs text-slate-500">
          Status: <span className="text-emerald-400">Online</span>
        </div>
      </div>

      {/* Main Content */}
      <div className="flex-1 flex flex-col h-full overflow-hidden">
        {activeTab === 'chat' && (
          <ChatView messages={messages} query={query} setQuery={setQuery} onSend={sendMessage} loading={loading} />
        )}

        {activeTab === 'ingest' && (
          <div className="flex flex-col items-center justify-center h-full text-slate-400 p-8">
            <div className="bg-surface p-8 rounded-2xl border border-slate-700 text-center max-w-md w-full">
              <div className="bg-slate-800 w-16 h-16 rounded-full flex items-center justify-center mx-auto mb-6">
                <FolderOpen size={32} className="text-primary" />
              </div>
              <h2 className="text-2xl font-bold text-white mb-2">Upload Project</h2>
              <p className="mb-6 text-sm">Select a local folder to ingest code into the Knowledge Graph.</p>

              <input
                type="file"
                ref={fileInputRef}
                className="hidden"
                webkitdirectory=""
                directory=""
                multiple
                onChange={handleFolderSelect}
              />

              <button
                onClick={() => fileInputRef.current?.click()}
                disabled={ingestStatus === 'Uploading...'}
                className="w-full py-3 bg-primary text-white rounded-lg hover:bg-blue-600 transition font-medium disabled:opacity-50 flex justify-center items-center gap-2"
              >
                {ingestStatus === 'Uploading...' ? 'Uploading...' : 'Choose Folder'}
              </button>

              {ingestStatus && (
                <div className={`mt-4 text-sm p-3 rounded-lg ${ingestStatus.startsWith('Error') ? 'bg-red-500/10 text-red-400' : 'bg-emerald-500/10 text-emerald-400'}`}>
                  {ingestStatus}
                </div>
              )}
            </div>
          </div>
        )}

        {activeTab === 'db' && (
          <div className="flex flex-col h-full p-8 overflow-hidden">
            <div className="flex items-center justify-between mb-6">
              <h2 className="text-2xl font-bold text-white flex items-center gap-2">
                <Database size={24} className="text-primary" /> Inspection
              </h2>
              {dbLoading && <div className="text-sm text-slate-400 animate-pulse">Fetching...</div>}
            </div>

            <div className="flex-1 bg-surface border border-slate-700 rounded-xl overflow-auto">
              {dbData && dbData.ids?.length > 0 ? (
                <table className="w-full text-left border-collapse">
                  <thead className="bg-slate-800 text-xs uppercase text-slate-400 sticky top-0">
                    <tr>
                      <th className="p-4 font-medium border-b border-slate-700">ID</th>
                      <th className="p-4 font-medium border-b border-slate-700 w-1/2">Content</th>
                      <th className="p-4 font-medium border-b border-slate-700">Metadata</th>
                    </tr>
                  </thead>
                  <tbody className="text-sm divide-y divide-slate-700">
                    {dbData.ids.map((id, i) => (
                      <tr key={i} className="hover:bg-slate-800/50">
                        <td className="p-4 font-mono text-xs text-slate-300 align-top">{id}</td>
                        <td className="p-4 align-top">
                          <pre className="whitespace-pre-wrap font-mono text-xs text-slate-400 max-h-32 overflow-y-auto bg-black/20 p-2 rounded">{dbData.documents[i]}</pre>
                        </td>
                        <td className="p-4 align-top">
                          <pre className="font-mono text-xs text-emerald-400/80">{JSON.stringify(dbData.metadatas[i], null, 2)}</pre>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              ) : (
                <div className="p-8 text-center text-slate-500">
                  {dbLoading ? 'Loading data...' : 'No vectors found. Ingest a project first.'}
                  {dbError && <div className="text-red-400 mt-2">{dbError}</div>}
                </div>
              )}
            </div>
          </div>
        )}
      </div>
    </div>
  )
}

function TabButton({ icon, label, active, onClick }) {
  return (
    <button
      onClick={onClick}
      className={`flex items-center gap-3 px-4 py-3 rounded-lg transition-all w-full text-left ${active ? 'bg-primary/10 text-primary border border-primary/20 shadow-sm shadow-primary/10' : 'hover:bg-slate-800/50 text-slate-400 hover:text-slate-200'}`}
    >
      {icon}
      <span className="font-medium">{label}</span>
      {active && <motion.div layoutId="active-pill" className="ml-auto w-1.5 h-1.5 rounded-full bg-primary shadow-[0_0_8px_rgb(59,130,246)]" />}
    </button>
  )
}

function ChatView({ messages, query, setQuery, onSend, loading }) {
  return (
    <div className="flex flex-col h-full bg-[#0b1120]">
      <div className="flex-1 overflow-y-auto p-4 sm:p-8 space-y-6 scroll-smooth">
        {messages.map((msg, i) => (
          <motion.div
            initial={{ opacity: 0, y: 10 }}
            animate={{ opacity: 1, y: 0 }}
            key={i}
            className={`flex ${msg.role === 'user' ? 'justify-end' : 'justify-start'}`}
          >
            <div className={`max-w-[85%] sm:max-w-[75%] p-4 rounded-2xl shadow-sm ${msg.role === 'user'
                ? 'bg-primary text-white rounded-br-none shadow-blue-900/10'
                : 'bg-surface border border-slate-700/80 rounded-bl-none text-slate-200'
              }`}>
              <div className="whitespace-pre-wrap leading-relaxed">{msg.content}</div>
            </div>
          </motion.div>
        ))}
        {loading && (
          <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} className="flex items-center gap-2 text-slate-500 ml-4">
            <div className="w-2 h-2 bg-slate-500 rounded-full animate-bounce" style={{ animationDelay: '0s' }} />
            <div className="w-2 h-2 bg-slate-500 rounded-full animate-bounce" style={{ animationDelay: '0.1s' }} />
            <div className="w-2 h-2 bg-slate-500 rounded-full animate-bounce" style={{ animationDelay: '0.2s' }} />
          </motion.div>
        )}
      </div>

      <div className="p-4 sm:p-6 bg-surface border-t border-slate-700/50">
        <div className="max-w-4xl mx-auto flex gap-3">
          <input
            value={query}
            onChange={e => setQuery(e.target.value)}
            onKeyDown={e => e.key === 'Enter' && onSend()}
            className="flex-1 bg-background border border-slate-700/80 rounded-xl px-5 py-4 outline-none focus:border-primary focus:ring-1 focus:ring-primary/50 transition placeholder:text-slate-600 shadow-inner"
            placeholder="Ask a question about your code..."
          />
          <button
            onClick={onSend}
            disabled={!query.trim() || loading}
            className="bg-primary hover:bg-blue-600 text-white p-4 rounded-xl transition disabled:opacity-50 disabled:cursor-not-allowed shadow-lg shadow-blue-500/20 active:scale-95"
          >
            <Send size={20} />
          </button>
        </div>
      </div>
    </div>
  )
}

export default App
