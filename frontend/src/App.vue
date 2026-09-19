<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { ticketApi } from './services/ticketApi'

const tickets = ref([])
const loading = ref(true)
const saving = ref(false)
const error = ref('')
const showForm = ref(false)
const editingId = ref(null)
const filters = reactive({ status: '', priority: '' })
const form = reactive({
  title: '',
  description: '',
  status: 'NEW',
  category: 'OTHER',
  priority: 'MEDIUM'
})
const fieldErrors = ref({})

const statuses = [
  'NEW',
  'ASSIGNED',
  'IN_PROGRESS',
  'WAITING_FOR_USER',
  'RESOLVED',
  'CLOSED'
]

const categories = [
  'HARDWARE',
  'SOFTWARE',
  'NETWORK',
  'ACCOUNT',
  'ACCESS_REQUEST',
  'OTHER'
]

const priorities = ['LOW', 'MEDIUM', 'HIGH','CRITICAL']
const counts = computed(() => ({
  total: tickets.value.length,
  new: tickets.value.filter((ticket) => ticket.status === 'NEW').length,
  active: tickets.value.filter((ticket) =>
    ['ASSIGNED', 'IN_PROGRESS', 'WAITING_FOR_USER'].includes(ticket.status)
  ).length,
  resolved: tickets.value.filter((ticket) => ticket.status === 'RESOLVED').length,
}))

const label = (value) =>
  value.replace(/_/g, ' ').toLowerCase().replace(/\b\w/g, (letter) => letter.toUpperCase())
const formatDate = (value) => new Intl.DateTimeFormat('en', { dateStyle: 'medium', timeStyle: 'short' }).format(new Date(value))

async function loadTickets() {
  loading.value = true
  error.value = ''
  try {
    tickets.value = await ticketApi.list(filters)
  } catch (apiError) {
    error.value = apiError.message || 'Could not load tickets. Is the backend running?'
  } finally {
    loading.value = false
  }
}

function openCreate() {
  editingId.value = null
  Object.assign(form, {
  title: '',
  description: '',
  status: 'NEW',
  category: 'OTHER',
  priority: 'MEDIUM'
})
  fieldErrors.value = {}
  showForm.value = true
}

function openEdit(ticket) {
  editingId.value = ticket.id
  Object.assign(form, ticket)
  fieldErrors.value = {}
  showForm.value = true
}

async function saveTicket() {
  saving.value = true
  fieldErrors.value = {}
  try {
    if (editingId.value) await ticketApi.update(editingId.value, form)
    else await ticketApi.create(form)
    showForm.value = false
    await loadTickets()
  } catch (apiError) {
    fieldErrors.value = apiError.validationErrors || {}
    error.value = apiError.message || 'Could not save the ticket.'
  } finally {
    saving.value = false
  }
}

async function deleteTicket(ticket) {
  if (!window.confirm(`Delete “${ticket.title}”? This cannot be undone.`)) return
  try {
    await ticketApi.remove(ticket.id)
    await loadTickets()
  } catch (apiError) {
    error.value = apiError.message || 'Could not delete the ticket.'
  }
}

let filterTimer
watch(filters, () => {
  clearTimeout(filterTimer)
  filterTimer = setTimeout(loadTickets, 200)
})
onMounted(loadTickets)
</script>

<template>
  <div class="app-shell">
    <aside class="sidebar">
      <div class="brand"><span class="brand-mark">H</span><div><strong>ServiceFlow</strong><small>IT service workspace</small></div></div>
      <nav><a class="active" href="#"><span>▦</span> Tickets</a></nav>
      <div class="sidebar-note"><span class="pulse"></span><div><strong>System operational</strong><small>API connected locally</small></div></div>
    </aside>

    <main>
      <header><div><p class="eyebrow">IT SERVICE OVERVIEW</p><h1>Ticket dashboard</h1><p>Track requests, priorities and resolution progress.</p></div><button class="primary" @click="openCreate">＋ New ticket</button></header>

      <section class="stats">
        <article><span class="stat-icon blue">▦</span><div><small>Total tickets</small><strong>{{ counts.total }}</strong></div></article>
        <article><span class="stat-icon amber">○</span><div><small>New</small><strong>{{ counts.new }}</strong></div></article>
        <article><span class="stat-icon violet">↻</span><div><small>Active</small><strong>{{ counts.active }}</strong></div></article>
        <article><span class="stat-icon green">✓</span><div><small>Resolved</small><strong>{{ counts.resolved }}</strong></div></article>
      </section>

      <p v-if="error" class="alert">{{ error }} <button aria-label="Dismiss" @click="error = ''">×</button></p>

      <section class="panel">
        <div class="panel-heading"><div><h2>All tickets</h2><p>{{ tickets.length }} results in the current view</p></div><div class="filters"><select v-model="filters.status" aria-label="Filter by status"><option value="">All statuses</option><option v-for="status in statuses" :key="status" :value="status">{{ label(status) }}</option></select><select v-model="filters.priority" aria-label="Filter by priority"><option value="">All priorities</option><option v-for="priority in priorities" :key="priority" :value="priority">{{ label(priority) }}</option></select></div></div>

        <div v-if="loading" class="empty"><div class="spinner"></div><p>Loading tickets…</p></div>
        <div v-else-if="!tickets.length" class="empty"><span>✓</span><h3>No tickets found</h3><p>Adjust the filters or create your first support ticket.</p><button class="secondary" @click="openCreate">Create ticket</button></div>
        <div v-else class="table-wrap"><table><thead><tr><th>Ticket</th><th>Category</th><th>Status</th><th>Priority</th><th>Created</th><th><span class="sr-only">Actions</span></th></tr></thead><tbody><tr v-for="ticket in tickets" :key="ticket.id"><td><strong>{{ ticket.title }}</strong><p>{{ ticket.description }}</p><small>#{{ String(ticket.id).padStart(4, '0') }}</small></td><td><span class="badge" :class="`status-${ticket.status.toLowerCase()}`"><i></i>{{ label(ticket.status) }}</span></td><td><span class="priority" :class="ticket.priority.toLowerCase()">{{ label(ticket.priority) }}</span></td><td>{{ formatDate(ticket.createdAt) }}</td><td class="actions"><button title="Edit ticket" @click="openEdit(ticket)">Edit</button><button class="danger" title="Delete ticket" @click="deleteTicket(ticket)">Delete</button></td></tr></tbody></table></div>
      </section>
    </main>

    <div v-if="showForm" class="modal-backdrop" @mousedown.self="showForm = false">
      <form class="modal" @submit.prevent="saveTicket"><div class="modal-heading"><div><p class="eyebrow">{{ editingId ? 'UPDATE REQUEST' : 'NEW REQUEST' }}</p><h2>{{ editingId ? 'Edit ticket' : 'Create a ticket' }}</h2></div><button type="button" class="close" aria-label="Close" @click="showForm = false">×</button></div>
        <label>Title<input v-model="form.title" maxlength="120" placeholder="Briefly describe the issue" required /><small v-if="fieldErrors.title" class="error-text">{{ fieldErrors.title }}</small></label>
        <label>Description<textarea v-model="form.description" maxlength="2000" rows="5" placeholder="Add context, symptoms and useful details" required></textarea><small v-if="fieldErrors.description" class="error-text">{{ fieldErrors.description }}</small></label>
        <label>
  Category
  <select v-model="form.category">
    <option
      v-for="category in categories"
      :key="category"
      :value="category"
    >
      {{ label(category) }}
    </option>
  </select>
</label>
        <div class="form-row"><label>Status<select v-model="form.status"><option v-for="status in statuses" :key="status" :value="status">{{ label(status) }}</option></select></label><label>Priority<select v-model="form.priority"><option v-for="priority in priorities" :key="priority" :value="priority">{{ label(priority) }}</option></select></label></div>
        <div class="modal-actions"><button type="button" class="secondary" @click="showForm = false">Cancel</button><button class="primary" :disabled="saving">{{ saving ? 'Saving…' : editingId ? 'Save changes' : 'Create ticket' }}</button></div>
      </form>
    </div>
  </div>
</template>
