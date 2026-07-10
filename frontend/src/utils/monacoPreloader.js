import * as monaco from 'monaco-editor'

let monacoReady = false
let resolveReady = null

export const monacoReadyPromise = new Promise(resolve => {
  resolveReady = resolve
})

export const preloadMonaco = async () => {
  if (monacoReady) return

  try {
    await Promise.all([
      import('monaco-editor/esm/vs/editor/editor.main.js'),
      import('monaco-editor/esm/vs/basic-languages/cpp/cpp.contribution'),
      import('monaco-editor/esm/vs/basic-languages/java/java.contribution'),
      import('monaco-editor/esm/vs/basic-languages/python/python.contribution')
    ])

    monaco.languages.typescript.javascriptDefaults.setDiagnosticsOptions({
      noSemanticValidation: true,
      noSyntaxValidation: false
    })

    monacoReady = true
    if (resolveReady) {
      resolveReady()
    }
  } catch (error) {
    console.error('Failed to preload Monaco:', error)
  }
}

export const isMonacoReady = () => monacoReady

export { monaco }