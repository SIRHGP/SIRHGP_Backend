$ErrorActionPreference = 'Stop'
& (Join-Path $PSScriptRoot 'run-local.ps1')
exit $LASTEXITCODE