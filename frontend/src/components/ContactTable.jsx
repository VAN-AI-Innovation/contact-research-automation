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

  useEffect(() => {
    if (status !== 'COMPLETED' || !jobId) {
      return
    }

    loadContacts()
  }, [status, jobId])

  const loadContacts = async () => {
    setLoading(true)
    setError('')

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
      setError('수정 내용을 저장하지 못했습니다. 입력값을 확인해주세요.')
    } finally {
      setSaving(false)
    }
  }

  const displayValue = (value) => {
    return value && value.trim() ? value : '-'
  }

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

      {error && (
        <p className="contacts-error">
          {error}
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
                const editing = editingId === contact.id

                return (
                  <tr key={contact.id}>
                    <td>
                      {editing ? (
                        <input
                          name="organizationName"
                          value={editForm.organizationName}
                          onChange={handleChange}
                        />
                      ) : (
                        displayValue(contact.organizationName)
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
                        displayValue(contact.personName)
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
                        displayValue(contact.department)
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
                        displayValue(contact.position)
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
                        displayValue(contact.email)
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
                        displayValue(contact.phone)
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
                            onClick={() => saveEdit(contact.id)}
                            disabled={saving}
                          >
                            저장
                          </button>

                          <button
                            type="button"
                            onClick={cancelEdit}
                            disabled={saving}
                          >
                            취소
                          </button>
                        </div>
                      ) : (
                        <button
                          type="button"
                          className="edit-button"
                          onClick={() => startEdit(contact)}
                        >
                          수정
                        </button>
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
