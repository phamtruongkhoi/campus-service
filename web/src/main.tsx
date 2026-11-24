import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import './styles/tokens.css'
import './styles/base.css'
import './styles/layout.css'
import App from './App'
import { ConfigError } from './components/ConfigError'
import { isSupabaseConfigured } from './lib/supabase'
import { initTheme } from './theme'

initTheme()

createRoot(document.getElementById('root')!).render(
  <StrictMode>{isSupabaseConfigured ? <App /> : <ConfigError />}</StrictMode>,
)
