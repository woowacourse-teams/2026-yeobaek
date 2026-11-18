$ErrorActionPreference = 'Stop'

try {
    [Console]::InputEncoding = [Text.UTF8Encoding]::new($false)
    $OutputEncoding = [Text.UTF8Encoding]::new($false)
    $hookInput = [Console]::In.ReadToEnd()
    $collector = Join-Path (Split-Path -Parent $PSScriptRoot) '작업_사용량.py'
    $runtimePython = Join-Path $env:USERPROFILE '.cache/codex-runtimes/codex-primary-runtime/dependencies/python/python.exe'
    $pythonCommand = $null
    $pythonArguments = @()

    if ($env:YEOBAEK_USAGE_PYTHON) {
        $pythonCommand = $env:YEOBAEK_USAGE_PYTHON
    } elseif (Test-Path -LiteralPath $runtimePython) {
        $pythonCommand = $runtimePython
    } else {
        foreach ($candidate in @('python3', 'python', 'py')) {
            $resolved = Get-Command $candidate -ErrorAction SilentlyContinue
            if ($resolved) {
                $pythonCommand = $resolved.Source
                if ($candidate -eq 'py') {
                    $pythonArguments = @('-3')
                }
                break
            }
        }
    }

    if (-not $pythonCommand) {
        throw 'Python runtime unavailable for translation usage collection.'
    }

    $env:PYTHONIOENCODING = 'utf-8'
    $hookInput | & $pythonCommand @pythonArguments $collector hook
    if ($LASTEXITCODE -ne 0) {
        throw "Translation usage collector exited with code $LASTEXITCODE."
    }
    Write-Output '{}'
} catch {
    [Console]::Error.WriteLine("Translation usage collection pending: $($_.Exception.Message)")
    Write-Output '{}'
}
