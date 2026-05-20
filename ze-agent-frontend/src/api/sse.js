import { API_BASE_URL } from './http'

function buildUrl(path, params = {}) {
  const normalizedBase = API_BASE_URL.endsWith('/') ? API_BASE_URL.slice(0, -1) : API_BASE_URL
  const normalizedPath = path.startsWith('/') ? path : `/${path}`
  const url = new URL(`${normalizedBase}${normalizedPath}`, window.location.origin)

  Object.entries(params).forEach(([key, value]) => {
    if (value !== undefined && value !== null && value !== '') {
      url.searchParams.set(key, value)
    }
  })

  return url.toString()
}

export function createChatEventSource(path, params, handlers = {}) {
  const eventSource = new EventSource(buildUrl(path, params))
  let closed = false

  const close = () => {
    if (!closed) {
      closed = true
      eventSource.close()
    }
  }

  eventSource.onmessage = (event) => {
    if (event.data === '[DONE]') {
      handlers.onDone?.()
      close()
      return
    }

    handlers.onMessage?.(event.data)
  }

  eventSource.onerror = (event) => {
    handlers.onError?.(event)
    close()
  }

  eventSource.addEventListener('complete', () => {
    handlers.onDone?.()
    close()
  })

  return {
    close,
  }
}
