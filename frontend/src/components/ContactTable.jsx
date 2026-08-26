import { useEffect, useState } from 'react'

const API_BASE_URL = 'http://localhost:8080'

const EMPTY_FORM = {
  organizationName: '',
  personName: '',
  department: '',
  position: '',
  email: '',
  phone: '',
}

function ContactTable({ jobId, status }) {
  const [contacts, setContacts] = useState([])
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')

  const [editingId, setEditingId] = useState(null)
  const [editForm, setEditForm] = useState(EMPTY_FORM)
  const [saving, setSaving] = useState(false)

  const [selectedIds, setSelectedIds] = useState([])
  const [excluding, setExcluding] = useState(false)
  const [copyMessage, setCopyMessage] = useState('')

  useEffect(() => {
    if (status !== 'COMPLETED' || !jobId) {
      return
    }

    loadContacts()
  }, [status, jobId])

  const loadContacts = async () => {
    setLoading(true)
    setError('')
    setSelectedIds([])

    try {
      const response = await fetch(
        `${API_BASE_URL}/api/sessions/${jobId}/contacts`
      )

      if (!response.ok) {
        throw new Error('결과 조회 실패')
      }

      const data = await response.json()
      setContacts(data)
    } catch (err) {
      console.error(err)
      setError('수집 결과를 불러오지 못했습니다.')
    } finally {
      setLoading(false)
    }
  }

  const startEdit = (contact) => {
    setEditingId(contact.id)

    setEditForm({
      organizationName: contact.organizationName ?? '',
      personName: contact.personName ?? '',
      department: contact.department ?? '',
      position: contact.position ?? '',
      email: contact.email ?? '',
      phone: contact.phone ?? '',
    })

    setError('')
  }

  const cancelEdit = () => {
    setEditingId(null)
    setEditForm(EMPTY_FORM)
    setError('')
  }

  const handleChange = (event) => {
    const { name, value } = event.target

    setEditForm((prev) => ({
      ...prev,
      [name]: value,
    }))
  }

  const saveEdit = async (contactId) => {
    setSaving(true)
    setError('')

    try {
      const response = await fetch(
        `${API_BASE_URL}/api/contacts/${contactId}`,
        {
          method: 'PATCH',
          headers: {
            'Content-Type': 'application/json',
          },
          body: JSON.stringify(editForm),
        }
      )

      if (!response.ok) {
        const message = await response.text()
        throw new Error(message)
      }

      const updated = await response.json()

      setContacts((prev) =>
        prev.map((contact) =>
          contact.id === contactId ? updated : contact
        )
      )

      setEditingId(null)
      setEditForm(EMPTY_FORM)
    } catch (err) {
      console.error(err)
      setError(
        '수정 내용을 저장하지 못했습니다. 입력값을 확인해주세요.'
      )
    } finally {
      setSaving(false)
    }
  }

  const toggleContact = (contactId) => {
    setSelectedIds((prev) => {
      if (prev.includes(contactId)) {
        return prev.filter((id) => id !== contactId)
      }

      return [...prev, contactId]
    })
  }

  const toggleAll = () => {
    if (
      contacts.length > 0 &&
      selectedIds.length === contacts.length
    ) {
      setSelectedIds([])
      return
    }

    setSelectedIds(
      contacts.map((contact) => contact.id)
    )
  }

  const excludeSelectedContacts = async () => {
    if (selectedIds.length === 0 || excluding) {
      return
    }

    setExcluding(true)
    setError('')

    try {
      const response = await fetch(
        `${API_BASE_URL}/api/contacts/exclude`,
        {
          method: 'PATCH',
          headers: {
            'Content-Type': 'application/json',
          },
          body: JSON.stringify({
            contactIds: selectedIds,
          }),
        }
      )

      if (!response.ok) {
        const message = await response.text()
        throw new Error(message)
      }

      const excludedIds = [...selectedIds]

      setContacts((prev) =>
        prev.filter(
          (contact) =>
            !excludedIds.includes(contact.id)
        )
      )

      if (
        editingId !== null &&
        excludedIds.includes(editingId)
      ) {
        setEditingId(null)
        setEditForm(EMPTY_FORM)
      }

      setSelectedIds([])
    } catch (err) {
      console.error(err)
      setError(
        '선택한 연락처를 제외하지 못했습니다.'
      )
    } finally {
      setExcluding(false)
    }
  }

  const formatContactForClipboard = (contact) => {
    return [
      `기업/기관: ${contact.organizationName ?? ''}`,
      `담당자: ${contact.personName ?? ''}`,
      `부서: ${contact.department ?? ''}`,
      `직책: ${contact.position ?? ''}`,
      `이메일: ${contact.email ?? ''}`,
      `전화번호: ${contact.phone ?? ''}`,
      `출처: ${contact.sourceUrl ?? ''}`,
    ].join('\n')
  }

  const copyContacts = async (targetContacts) => {
    if (targetContacts.length === 0) {
      return
    }

    setError('')
    setCopyMessage('')

    const text = targetContacts
      .map(formatContactForClipboard)
      .join('\n\n')

    try {
      await navigator.clipboard.writeText(text)

      setCopyMessage(
        `${targetContacts.length}건이 클립보드에 복사되었습니다.`
      )
    } catch (err) {
      console.error(err)
      setError('클립보드 복사에 실패했습니다.')
    }
  }

  const copySelectedContacts = () => {
    const selectedContacts = contacts.filter((contact) =>
      selectedIds.includes(contact.id)
    )

    copyContacts(selectedContacts)
  }

  const formatPhoneForCsv = (phone) => {
    if (!phone) {
      return ''
    }

    const raw = String(phone).trim()
    const digits = raw.replace(/\D/g, '')

    let formatted = raw

    if (digits.length === 9 && digits.startsWith('02')) {
      formatted =
        `${digits.slice(0, 2)}-${digits.slice(2, 5)}-${digits.slice(5)}`
    } else if (digits.length === 10) {
      if (digits.startsWith('02')) {
        formatted =
          `${digits.slice(0, 2)}-${digits.slice(2, 6)}-${digits.slice(6)}`
      } else {
        formatted =
          `${digits.slice(0, 3)}-${digits.slice(3, 6)}-${digits.slice(6)}`
      }
    } else if (digits.length === 11) {
      formatted =
        `${digits.slice(0, 3)}-${digits.slice(3, 7)}-${digits.slice(7)}`
    }

    return `="${formatted}"`
  }

  const escapeCsvValue = (value) => {
    if (value === null || value === undefined) {
      return ''
    }

    const text = String(value)

    if (
      text.includes(',') ||
      text.includes('"') ||
      text.includes('\n') ||
      text.includes('\r')
    ) {
      return `"${text.replaceAll('"', '""')}"`
    }

    return text
  }

  const exportCsv = () => {
    const targetContacts =
      selectedIds.length > 0
        ? contacts.filter((contact) =>
            selectedIds.includes(contact.id)
          )
        : contacts

    if (targetContacts.length === 0) {
      setError('내보낼 연락처가 없습니다.')
      return
    }

    setError('')
    setCopyMessage('')

    const headers = [
      '기업/기관',
      '담당자',
      '부서',
      '직책',
      '이메일',
      '전화번호',
      '출처',
    ]

    const rows = targetContacts.map((contact) => [
      contact.organizationName,
      contact.personName,
      contact.department,
      contact.position,
      contact.email,
      formatPhoneForCsv(contact.phone),
      contact.sourceUrl,
    ])

    const csv = [
      headers.map(escapeCsvValue).join(','),
      ...rows.map((row) =>
        row.map(escapeCsvValue).join(',')
      ),
    ].join('\r\n')

    const blob = new Blob(
      [`\uFEFF${csv}`],
      {
        type: 'text/csv;charset=utf-8;',
      }
    )

    const url = URL.createObjectURL(blob)
    const link = document.createElement('a')

    const now = new Date()

    const date = [
      now.getFullYear(),
      String(now.getMonth() + 1).padStart(2, '0'),
      String(now.getDate()).padStart(2, '0'),
    ].join('-')

    const time = [
      String(now.getHours()).padStart(2, '0'),
      String(now.getMinutes()).padStart(2, '0'),
    ].join('')

    link.href = url
    link.download =
      `contact-research-${date}-${time}.csv`

    document.body.appendChild(link)
    link.click()
    link.remove()

    URL.revokeObjectURL(url)
  }

  const displayValue = (value) => {
    return value && value.trim() ? value : '-'
  }

  const allSelected =
    contacts.length > 0 &&
    selectedIds.length === contacts.length

  if (status !== 'COMPLETED') {
    return null
  }

  if (loading) {
    return (
      <section className="contacts-section">
        <p className="contacts-message">
          수집 결과를 불러오는 중...
        </p>
      </section>
    )
  }

  return (
    <section className="contacts-section">
      <div className="contacts-title-row">
        <h2>수집 결과</h2>
        <span>{contacts.length}건</span>
      </div>

      {contacts.length > 0 && (
        <div className="contacts-selection-bar">
          <span>
            선택된 연락처 {selectedIds.length}건
          </span>

          <div className="selection-actions">
            <button
              type="button"
              className="copy-button"
              onClick={copySelectedContacts}
              disabled={
                selectedIds.length === 0 ||
                excluding
              }
            >
              선택 복사
            </button>

            <button
              type="button"
              className="csv-button"
              onClick={exportCsv}
              disabled={excluding}
            >
              CSV 다운로드
            </button>

            <button
              type="button"
              className="exclude-button"
              onClick={excludeSelectedContacts}
              disabled={
                selectedIds.length === 0 ||
                excluding
              }
            >
              {excluding
                ? '제외 중...'
                : '선택 제외'}
            </button>
          </div>
        </div>
      )}

      {error && (
        <p className="contacts-error">
          {error}
        </p>
      )}

      {copyMessage && (
        <p className="contacts-success">
          {copyMessage}
        </p>
      )}

      {contacts.length === 0 ? (
        <p className="contacts-message">
          수집된 연락처가 없습니다.
        </p>
      ) : (
        <div className="contacts-table-wrap">
          <table className="contacts-table">
            <thead>
              <tr>
                <th className="checkbox-column">
                  <input
                    type="checkbox"
                    checked={allSelected}
                    onChange={toggleAll}
                    aria-label="전체 연락처 선택"
                  />
                </th>

                <th>기업/기관</th>
                <th>담당자</th>
                <th>부서</th>
                <th>직책</th>
                <th>이메일</th>
                <th>전화번호</th>
                <th>출처</th>
                <th>관리</th>
              </tr>
            </thead>

            <tbody>
              {contacts.map((contact) => {
                const editing =
                  editingId === contact.id

                const selected =
                  selectedIds.includes(contact.id)

                return (
                  <tr key={contact.id}>
                    <td className="checkbox-column">
                      <input
                        type="checkbox"
                        checked={selected}
                        onChange={() =>
                          toggleContact(contact.id)
                        }
                        aria-label={`${contact.id}번 연락처 선택`}
                      />
                    </td>

                    <td>
                      {editing ? (
                        <input
                          name="organizationName"
                          value={
                            editForm.organizationName
                          }
                          onChange={handleChange}
                        />
                      ) : (
                        displayValue(
                          contact.organizationName
                        )
                      )}
                    </td>

                    <td>
                      {editing ? (
                        <input
                          name="personName"
                          value={editForm.personName}
                          onChange={handleChange}
                        />
                      ) : (
                        displayValue(
                          contact.personName
                        )
                      )}
                    </td>

                    <td>
                      {editing ? (
                        <input
                          name="department"
                          value={editForm.department}
                          onChange={handleChange}
                        />
                      ) : (
                        displayValue(
                          contact.department
                        )
                      )}
                    </td>

                    <td>
                      {editing ? (
                        <input
                          name="position"
                          value={editForm.position}
                          onChange={handleChange}
                        />
                      ) : (
                        displayValue(
                          contact.position
                        )
                      )}
                    </td>

                    <td>
                      {editing ? (
                        <input
                          name="email"
                          value={editForm.email}
                          onChange={handleChange}
                        />
                      ) : (
                        displayValue(
                          contact.email
                        )
                      )}
                    </td>

                    <td>
                      {editing ? (
                        <input
                          name="phone"
                          value={editForm.phone}
                          onChange={handleChange}
                        />
                      ) : (
                        displayValue(
                          contact.phone
                        )
                      )}
                    </td>

                    <td>
                      {contact.sourceUrl ? (
                        <a
                          href={contact.sourceUrl}
                          target="_blank"
                          rel="noreferrer"
                        >
                          출처 보기
                        </a>
                      ) : (
                        '-'
                      )}
                    </td>

                    <td>
                      {editing ? (
                        <div className="table-actions">
                          <button
                            type="button"
                            onClick={() =>
                              saveEdit(contact.id)
                            }
                            disabled={
                              saving || excluding
                            }
                          >
                            저장
                          </button>

                          <button
                            type="button"
                            onClick={cancelEdit}
                            disabled={
                              saving || excluding
                            }
                          >
                            취소
                          </button>
                        </div>
                      ) : (
                        <div className="table-actions">
                          <button
                            type="button"
                            className="copy-button"
                            onClick={() =>
                              copyContacts([contact])
                            }
                            disabled={excluding}
                          >
                            복사
                          </button>

                          <button
                            type="button"
                            className="edit-button"
                            onClick={() =>
                              startEdit(contact)
                            }
                            disabled={excluding}
                          >
                            수정
                          </button>
                        </div>
                      )}
                    </td>
                  </tr>
                )
              })}
            </tbody>
          </table>
        </div>
      )}
    </section>
  )
}

export default ContactTable