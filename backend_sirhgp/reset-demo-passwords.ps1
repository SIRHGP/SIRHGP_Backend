$ErrorActionPreference = 'Stop'

# Reset only the five built-in demo accounts. The shared password is read
# securely and passed to the initializer through process-local environment
# variables; it is never written to the repository or printed to the console.
$securePassword = Read-Host 'Nouveau mot de passe commun des comptes de démonstration' -AsSecureString
if ($securePassword.Length -lt 12) {
    $securePassword.Dispose()
    throw 'Le mot de passe doit contenir au moins 12 caractères.'
}

$sharedPassword = [System.Net.NetworkCredential]::new('', $securePassword).Password
$securePassword.Dispose()

$env:SPRING_PROFILES_ACTIVE = 'dev'
$env:APP_INIT_DATA = 'true'
$env:APP_DEFAULT_USERS_SUPERADMIN_PASSWORD = $sharedPassword
$env:APP_DEFAULT_USERS_ADMIN_PASSWORD = $sharedPassword
$env:APP_DEFAULT_USERS_MANAGER_PASSWORD = $sharedPassword
$env:APP_DEFAULT_USERS_STAFF_PASSWORD = $sharedPassword
$env:APP_DEFAULT_USERS_STAGIAIRE_PASSWORD = $sharedPassword
$sharedPassword = $null

& (Join-Path $PSScriptRoot 'start-local.ps1')
