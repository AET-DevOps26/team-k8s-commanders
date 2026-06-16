export type Route = '/' | '/login' | '/register' | '/patient' | '/admin'

export function dashboardPath(role: string): Route {
  return role === 'ADMIN' ? '/admin' : '/patient'
}

export function getInitialRoute(): Route {
  const path = window.location.pathname

  if (
    path === '/login' ||
    path === '/register' ||
    path === '/patient' ||
    path === '/admin'
  ) {
    return path
  }

  return '/'
}
