import { useState } from 'react'
import './App.css'

function App() {
  const [keyword, setKeyword] = useState('')
  const [keywords, setKeywords] = useState([])

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

  const handleStart = () => {
    if (keywords.length === 0) {
      alert('컨택 대상 키워드를 1개 이상 입력해주세요.')
      return
    }

    alert(`수집 대상 ${keywords.length}개가 등록되었습니다.`)
    console.log('수집 대상:', keywords)
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
          <label htmlFor="keyword">컨택 대상 키워드</label>

          <div className="input-row">
            <input
              id="keyword"
              type="text"
              value={keyword}
              placeholder="예: 부산 AI 스타트업"
              onChange={(e) => setKeyword(e.target.value)}
              onKeyDown={handleKeyDown}
            />

            <button type="button" className="add-button" onClick={addKeyword}>
              추가
            </button>
          </div>

          <p className="helper">
            Enter 또는 추가 버튼으로 여러 검색어를 등록할 수 있습니다.
          </p>
        </div>

        <div className="tag-section">
          {keywords.length === 0 ? (
            <p className="empty">등록된 검색어가 없습니다.</p>
          ) : (
            <div className="tags">
              {keywords.map((item) => (
                <div className="tag" key={item}>
                  <span>{item}</span>
                  <button
                    type="button"
                    onClick={() => removeKeyword(item)}
                    aria-label={`${item} 삭제`}
                  >
                    ×
                  </button>
                </div>
              ))}
            </div>
          )}
        </div>

        <button type="button" className="start-button" onClick={handleStart}>
          수집 시작
        </button>
      </section>
    </main>
  )
}

export default App
