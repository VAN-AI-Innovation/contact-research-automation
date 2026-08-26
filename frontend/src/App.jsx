import { useEffect, useRef, useState } from 'react'
import './App.css'
import ContactTable from './components/ContactTable'

const API_BASE_URL = 'http://localhost:8080'
const TEST_START_URL = 'https://busanstartup.kr'
const MAX_PAGES = 20

function App() {
  const [keyword, setKeyword] = useState('')
  const [keywords, setKeywords] = useState([])

  const [jobId, setJobId] = useState(null)
  const [status, setStatus] = useState('IDLE')
  const [progress, setProgress] = useState(0)
  const [visitedPages, setVisitedPages] = useState(0)
  const [maxPages, setMaxPages] = useState(MAX_PAGES)
  const [collectedContacts, setCollectedContacts] = useState(0)
  const [errorMessage, setErrorMessage] = useState('')

  const eventSourceRef = useRef(null)

  const isRunning = status === 'RUNNING'

  useEffect(() => {
    return () => {
      if (eventSourceRef.current) {
        eventSourceRef.current.close()
      }
    }
  }, [])

  const addKeyword = () => {
    const value = keyword.trim()

    if (!value) return

    if (keywords.includes(value)) {
      setKeyword('')
      return
    }

    setKeywords([...keywords, value])
    setKeyword('')
  }

  const removeKeyword = (target) => {
    setKeywords(keywords.filter((item) => item !== target))
  }

  const handleKeyDown = (event) => {
    if (event.key === 'Enter') {
      event.preventDefault()
      addKeyword()
    }
  }

  const applyProgressEvent = (data) => {
    setJobId(data.jobId)
    setStatus(data.status)
    setVisitedPages(data.visitedPages ?? 0)
    setMaxPages(data.maxPages ?? MAX_PAGES)
    setCollectedContacts(data.collectedContacts ?? 0)
    setProgress(data.progress ?? 0)
  }

  const connectSse = (newJobId) => {
    if (eventSourceRef.current) {
      eventSourceRef.current.close()
    }

    const eventSource = new EventSource(
      `${API_BASE_URL}/api/crawl/${newJobId}/events`
    )

    eventSourceRef.current = eventSource

    const handleEvent = (event) => {
      const data = JSON.parse(event.data)
      applyProgressEvent(data)
    }

    eventSource.addEventListener('connected', handleEvent)
    eventSource.addEventListener('progress', handleEvent)

    eventSource.addEventListener('completed', (event) => {
      const data = JSON.parse(event.data)
      applyProgressEvent(data)
      eventSource.close()
      eventSourceRef.current = null
    })

    eventSource.addEventListener('stopped', (event) => {
      const data = JSON.parse(event.data)
      applyProgressEvent(data)
      eventSource.close()
      eventSourceRef.current = null
    })

    eventSource.addEventListener('failed', (event) => {
      const data = JSON.parse(event.data)
      applyProgressEvent(data)
      setErrorMessage('수집 중 오류가 발생했습니다.')
      eventSource.close()
      eventSourceRef.current = null
    })

    eventSource.onerror = () => {
      if (eventSource.readyState === EventSource.CLOSED) {
        return
      }

      setErrorMessage('실시간 진행 상태 연결을 확인해주세요.')
    }
  }

  const handleStart = async () => {
    if (keywords.length === 0) {
      alert('컨택 대상 키워드를 1개 이상 입력해주세요.')
      return
    }

    if (isRunning) {
      return
    }

    setErrorMessage('')
    setStatus('RUNNING')
    setProgress(0)
    setVisitedPages(0)
    setMaxPages(MAX_PAGES)
    setCollectedContacts(0)

    try {
      const response = await fetch(
        `${API_BASE_URL}/api/crawl/start`,
        {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
          },
          body: JSON.stringify({
            url: TEST_START_URL,
            maxPages: MAX_PAGES,
          }),
        }
      )

      if (!response.ok) {
        throw new Error('수집 시작 요청에 실패했습니다.')
      }

      const data = await response.json()

      setJobId(data.jobId)
      setStatus(data.status)

      connectSse(data.jobId)
    } catch (error) {
      console.error(error)
      setStatus('FAILED')
      setErrorMessage('수집을 시작하지 못했습니다.')
    }
  }

  const handleStop = async () => {
    if (!jobId || !isRunning) {
      return
    }

    try {
      const response = await fetch(
        `${API_BASE_URL}/api/crawl/${jobId}/stop`,
        {
          method: 'POST',
        }
      )

      if (!response.ok) {
        throw new Error('수집 중단 요청에 실패했습니다.')
      }

      const data = await response.json()

      setStatus(data.status)
      setVisitedPages(data.visitedPages ?? visitedPages)
      setCollectedContacts(
        data.collectedContacts ?? collectedContacts
      )
    } catch (error) {
      console.error(error)
      setErrorMessage('수집 중단 요청에 실패했습니다.')
    }
  }

  const getStatusText = () => {
    switch (status) {
      case 'RUNNING':
        return '수집 중...'
      case 'COMPLETED':
        return '수집 완료'
      case 'STOPPED':
        return '수집이 중단되었습니다.'
      case 'FAILED':
        return '수집 중 오류가 발생했습니다.'
      default:
        return ''
    }
  }

  return (
    <main className="page">
      <section className="research-card">
        <header>
          <p className="eyebrow">VAN AI Innovation</p>

          <h1>Contact Research Automation</h1>

          <p className="description">
            찾고 싶은 기업·기관·담당자와 관련된 검색 키워드를 입력해주세요.
          </p>
        </header>

        <div className="input-section">
          <label htmlFor="keyword">
            컨택 대상 키워드
          </label>

          <div className="input-row">
            <input
              id="keyword"
              type="text"
              value={keyword}
              placeholder="예: 부산 AI 스타트업"
              disabled={isRunning}
              onChange={(e) =>
                setKeyword(e.target.value)
              }
              onKeyDown={handleKeyDown}
            />

            <button
              type="button"
              className="add-button"
              onClick={addKeyword}
              disabled={isRunning}
            >
              추가
            </button>
          </div>

          <p className="helper">
            Enter 또는 추가 버튼으로 여러 검색어를 등록할 수 있습니다.
          </p>
        </div>

        <div className="tag-section">
          {keywords.length === 0 ? (
            <p className="empty">
              등록된 검색어가 없습니다.
            </p>
          ) : (
            <div className="tags">
              {keywords.map((item) => (
                <div
                  className="tag"
                  key={item}
                >
                  <span>{item}</span>

                  <button
                    type="button"
                    disabled={isRunning}
                    onClick={() =>
                      removeKeyword(item)
                    }
                    aria-label={`${item} 삭제`}
                  >
                    ×
                  </button>
                </div>
              ))}
            </div>
          )}
        </div>

        {status !== 'IDLE' && (
          <div className="progress-section">
            <div className="progress-header">
              <strong>
                {getStatusText()}
              </strong>

              <span>{progress}%</span>
            </div>

            <div className="progress-track">
              <div
                className="progress-bar"
                style={{
                  width: `${progress}%`,
                }}
              />
            </div>

            <div className="progress-info">
              <span>
                {visitedPages} / {maxPages} 페이지 방문
              </span>

              <span>
                고유 연락처 {collectedContacts}건 수집
              </span>
            </div>

            {errorMessage && (
              <p className="error-message">
                {errorMessage}
              </p>
            )}
          </div>
        )}

        {isRunning ? (
          <button
            type="button"
            className="stop-button"
            onClick={handleStop}
          >
            수집 중지
          </button>
        ) : (
          <button
            type="button"
            className="start-button"
            onClick={handleStart}
          >
            {status === 'COMPLETED' ||
            status === 'STOPPED' ||
            status === 'FAILED'
              ? '다시 수집'
              : '수집 시작'}
          </button>
        )}

        <ContactTable
          jobId={jobId}
          status={status}
        />
      </section>
    </main>
  )
}

export default App