import { useEffect, useState } from 'react'

function App() {
  const [backendStatus, setBackendStatus] = useState('확인 중...')

  useEffect(() => {
    fetch('http://localhost:8080/api/health')
      .then((response) => {
        if (!response.ok) {
          throw new Error('API 요청 실패')
        }
        return response.text()
      })
      .then((data) => setBackendStatus(data))
      .catch(() => setBackendStatus('ERROR'))
  }, [])

  return (
    <main>
      <h1>Contact Research Automation</h1>
      <p>Backend: {backendStatus}</p>
    </main>
  )
}

export default App
