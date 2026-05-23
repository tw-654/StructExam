class SandboxWebSocket {
  constructor() {
    this.ws = null
    this.sessionId = null
    this.listeners = {
      output: [],
      error: [],
      status: []
    }
    this.reconnectAttempts = 0
    this.maxReconnectAttempts = 3
  }

  connect() {
    return new Promise((resolve, reject) => {
      const wsUrl = `ws://localhost:8083/ws/sandbox`
      this.ws = new WebSocket(wsUrl)

      this.ws.onopen = () => {
        console.log('Sandbox WebSocket connected')
        this.reconnectAttempts = 0
        resolve()
      }

      this.ws.onclose = () => {
        console.log('Sandbox WebSocket closed')
        this.notifyStatus('DISCONNECTED')
      }

      this.ws.onerror = (error) => {
        console.error('Sandbox WebSocket error:', error)
        reject(error)
      }

      this.ws.onmessage = (event) => {
        try {
          const message = JSON.parse(event.data)
          this.handleMessage(message)
        } catch (e) {
          console.error('Failed to parse WebSocket message:', e)
        }
      }
    })
  }

  handleMessage(message) {
    switch (message.type) {
      case 'OUTPUT':
        this.notifyOutput(message.data, false)
        break
      case 'ERROR':
        this.notifyError(message.data)
        break
      case 'STATUS':
        this.notifyStatus(message.status)
        break
    }
  }

  start(code, language, timeout = 60) {
    if (!this.ws || this.ws.readyState !== WebSocket.OPEN) {
      throw new Error('WebSocket not connected')
    }

    const msg = {
      type: 'START',
      code: code,
      language: language,
      timeout: timeout
    }

    this.ws.send(JSON.stringify(msg))
  }

  sendInput(input) {
    if (!this.ws || this.ws.readyState !== WebSocket.OPEN) {
      throw new Error('WebSocket not connected')
    }

    const msg = {
      type: 'INPUT',
      sessionId: this.sessionId,
      data: input
    }

    this.ws.send(JSON.stringify(msg))
  }

  terminate() {
    if (!this.ws || this.ws.readyState !== WebSocket.OPEN) {
      return
    }

    const msg = {
      type: 'TERMINATE',
      sessionId: this.sessionId
    }

    this.ws.send(JSON.stringify(msg))
  }

  disconnect() {
    if (this.ws) {
      this.terminate()
      this.ws.close()
      this.ws = null
    }
  }

  onOutput(callback) {
    this.listeners.output.push(callback)
  }

  onError(callback) {
    this.listeners.error.push(callback)
  }

  onStatus(callback) {
    this.listeners.status.push(callback)
  }

  notifyOutput(data, isError) {
    this.listeners.output.forEach(cb => cb(data, isError))
  }

  notifyError(error) {
    this.listeners.error.forEach(cb => cb(error))
  }

  notifyStatus(status) {
    this.listeners.status.forEach(cb => cb(status))
  }
}

export default new SandboxWebSocket()
