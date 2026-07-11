export type Route =
  | '/'
  | '/login'
  | '/register'
  | '/patient'
  | '/patient/profile'
  | '/patient/book'
  | '/admin'
  | '/doctor'
  | '/doctor/schedule'
  | '/doctor/patients'

export function dashboardPath(role: string): Route {
  if (role === 'ADMIN') {
    return '/admin'
  }
  if (role === 'DOCTOR') {
    return '/doctor'
  }
  return '/patient'
}

export function getInitialRoute(): Route {
  const path = window.location.pathname

  if (
    path === '/login' ||
    path === '/register' ||
    path === '/patient' ||
    path === '/patient/profile' ||
    path === '/patient/book' ||
    path === '/admin' ||
    path === '/doctor' ||
    path === '/doctor/schedule' ||
    path === '/doctor/patients'
  ) {
    return path
  }

  return '/'
}
