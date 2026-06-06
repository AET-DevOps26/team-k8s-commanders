import { useEffect, useState } from 'react'
import type { UserCreate, UserProfile, UserRole, UserStats } from '../clientApi'
import {
  createUser,
  getUserStats,
  listUsers,
  replaceUser,
} from '../clientApi'
import { StatusPanel } from '../components/StatusPanel'
import { ShellNav } from '../components/ShellNav'
import './admin.css'
import { AdminStats } from './AdminStats'
import { ConfirmDialog } from './ConfirmDialog'
import { CreateUserDialog } from './CreateUserDialog'
import { EditUserDialog } from './EditUserDialog'
import { PAGE_SIZE } from './constants'
import type { AdminData, AdminDashboardProps } from './types'
import { UserTable } from './UserTable'

export function AdminDashboard({
  session,
  onLogout,
  onNavigate,
}: AdminDashboardProps) {
  const [data, setData] = useState<AdminData | null>(null)
  const [stats, setStats] = useState<UserStats | null>(null)
  const [page, setPage] = useState(0)
  const [isLoading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [actionError, setActionError] = useState('')
  const [busyId, setBusyId] = useState<string | null>(null)
  const [editing, setEditing] = useState<UserProfile | null>(null)
  const [showCreate, setShowCreate] = useState(false)
  const [confirmingDeactivate, setConfirmingDeactivate] =
    useState<UserProfile | null>(null)

  const token = session.accessToken
  const isAdmin = session.user.role === 'ADMIN'

  async function reload() {
    setLoading(true)
    setError('')

    try {
      const [usersResponse, statsResponse] = await Promise.all([
        listUsers(token, page, PAGE_SIZE),
        getUserStats(token),
      ])

      setData({
        users: usersResponse.content,
        total: usersResponse.page.totalElements,
        page: usersResponse.page.page,
        totalPages: usersResponse.page.totalPages,
      })
      setStats(statsResponse)
    } catch (loadError) {
      setError(
        loadError instanceof Error
          ? loadError.message
          : 'Admin data could not be loaded',
      )
      setData(null)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    if (!isAdmin) {
      return
    }

    let isActive = true

    async function load() {
      setLoading(true)
      setError('')

      try {
        const [usersResponse, statsResponse] = await Promise.all([
          listUsers(token, page, PAGE_SIZE),
          getUserStats(token),
        ])

        if (isActive) {
          setData({
            users: usersResponse.content,
            total: usersResponse.page.totalElements,
            page: usersResponse.page.page,
            totalPages: usersResponse.page.totalPages,
          })
          setStats(statsResponse)
        }
      } catch (loadError) {
        if (isActive) {
          setError(
            loadError instanceof Error
              ? loadError.message
              : 'Admin data could not be loaded',
          )
          setData(null)
        }
      } finally {
        if (isActive) {
          setLoading(false)
        }
      }
    }

    load()

    return () => {
      isActive = false
    }
  }, [isAdmin, token, page])

  async function handleRoleChange(user: UserProfile, role: UserRole) {
    setActionError('')
    setBusyId(user.id)

    try {
      await replaceUser(user.id, { ...user, role }, token)
      await reload()
    } catch (changeError) {
      setActionError(
        changeError instanceof Error
          ? changeError.message
          : 'Role change failed',
      )
    } finally {
      setBusyId(null)
    }
  }

  async function handleDeactivate(user: UserProfile) {
    setActionError('')
    setBusyId(user.id)

    try {
      // Deactivate via the same PUT path as Edit (set enabled=false) rather than
      // the dedicated DELETE endpoint.
      await replaceUser(user.id, { ...user, enabled: false }, token)
      setConfirmingDeactivate(null)
      await reload()
    } catch (deactivateError) {
      setActionError(
        deactivateError instanceof Error
          ? deactivateError.message
          : 'Deactivation failed',
      )
      setConfirmingDeactivate(null)
    } finally {
      setBusyId(null)
    }
  }

  async function handleActivate(user: UserProfile) {
    setActionError('')
    setBusyId(user.id)

    try {
      await replaceUser(user.id, { ...user, enabled: true }, token)
      await reload()
    } catch (activateError) {
      setActionError(
        activateError instanceof Error
          ? activateError.message
          : 'Activation failed',
      )
    } finally {
      setBusyId(null)
    }
  }

  async function handleSaveEdit(updated: UserProfile) {
    setActionError('')
    setBusyId(updated.id)

    try {
      await replaceUser(updated.id, updated, token)
      setEditing(null)
      await reload()
    } catch (saveError) {
      setActionError(
        saveError instanceof Error ? saveError.message : 'Update failed',
      )
    } finally {
      setBusyId(null)
    }
  }

  async function handleCreate(payload: UserCreate) {
    setActionError('')

    try {
      await createUser(payload, token)
      setShowCreate(false)
      await reload()
    } catch (createError) {
      setActionError(
        createError instanceof Error ? createError.message : 'Create failed',
      )
      throw createError
    }
  }

  if (!isAdmin) {
    return (
      <main className="landing-page app-page">
        <ShellNav session={session} onNavigate={onNavigate} onLogout={onLogout} />
        <section className="empty-state">
          <p className="eyebrow">Admin dashboard</p>
          <h1>Admin account required.</h1>
          <p>This area is restricted to administrators.</p>
          <button className="primary-button" onClick={onLogout} type="button">
            Logout
          </button>
        </section>
      </main>
    )
  }

  return (
    <main className="landing-page app-page">
      <ShellNav session={session} onNavigate={onNavigate} onLogout={onLogout} />
      <section className="dashboard-shell">
        <header className="patient-hero">
          <div>
            <p className="eyebrow">Admin dashboard</p>
            <h1>User management</h1>
            <p>
              Manage roles, profiles, and account access across all CareDesk
              users.
            </p>
          </div>
          <button
            className="primary-button"
            onClick={() => setShowCreate(true)}
            type="button"
          >
            New user
          </button>
        </header>

        {stats && <AdminStats stats={stats} />}

        {isLoading && <StatusPanel title="Loading users" />}
        {error && <StatusPanel title="Admin API unavailable" text={error} />}
        {actionError && <StatusPanel title="Action failed" text={actionError} />}

        {data && (
          <UserTable
            busyId={busyId}
            currentUserId={session.user.id}
            data={data}
            isLoading={isLoading}
            onActivate={handleActivate}
            onDeactivate={setConfirmingDeactivate}
            onEdit={setEditing}
            onPageChange={setPage}
            onRoleChange={handleRoleChange}
          />
        )}
      </section>

      {editing && (
        <EditUserDialog
          busy={busyId === editing.id}
          onCancel={() => setEditing(null)}
          onSave={handleSaveEdit}
          user={editing}
        />
      )}

      {showCreate && (
        <CreateUserDialog
          onCancel={() => setShowCreate(false)}
          onCreate={handleCreate}
        />
      )}

      {confirmingDeactivate && (
        <ConfirmDialog
          busy={busyId === confirmingDeactivate.id}
          confirmLabel="Deactivate"
          danger
          message={`${confirmingDeactivate.name} will no longer be able to sign in. Their records are kept and the account can be re-enabled later.`}
          onCancel={() => setConfirmingDeactivate(null)}
          onConfirm={() => handleDeactivate(confirmingDeactivate)}
          title={`Deactivate ${confirmingDeactivate.name}?`}
        />
      )}
    </main>
  )
}
