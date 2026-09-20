const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || '/api'

async function request(path = '', options = {}) {
  const response = await fetch(`${API_BASE_URL}/tickets${path}`, {
    headers: { 'Content-Type': 'application/json', ...options.headers },
    ...options,
  })

  if (!response.ok) {
    const error = await response.json().catch(() => ({ message: 'The server could not complete the request.' }))
    throw error
  }

  return response.status === 204 ? null : response.json()
}

export const ticketApi = {
  list(filters = {}) {
    const params = new URLSearchParams()
    if (filters.status) params.set('status', filters.status)
    if (filters.priority) params.set('priority', filters.priority)
    const query = params.toString()
    return request(query ? `?${query}` : '')
  },
  create(ticket) {
    return request('', { method: 'POST', body: JSON.stringify(ticket) })
  },
  update(id, ticket, role) {
    return request(`/${id}`, {
      method: 'PUT',
      headers: {
        'X-User-Role': role
      },
      body: JSON.stringify(ticket)
    })
  },
  activities(id) {
  return request(`/${id}/activities`)
  },
  remove(id, role) {
    return request(`/${id}`, {
      method: 'DELETE',
      headers: {
        'X-User-Role': role
      }
    })
  },
}
