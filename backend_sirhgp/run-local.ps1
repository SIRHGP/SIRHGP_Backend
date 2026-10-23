$ErrorActionPreference = 'Stop'

$env:SPRING_PROFILES_ACTIVE = 'dev'
if ([string]::IsNullOrWhiteSpace($env:DB_URL) -or $env:DB_URL -eq '${DB_URL}') {
    $env:DB_URL = 'jdbc:mysql://localhost:3306/sirhgp_db?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true'
}
if ([string]::IsNullOrWhiteSpace($env:DB_USERNAME) -or $env:DB_USERNAME -eq '${DB_USERNAME}') {
    $env:DB_USERNAME = Read-Host 'Utilisateur MySQL'
}
if ([string]::IsNullOrWhiteSpace($env:DB_USERNAME) -or $env:DB_USERNAME -eq '${DB_USERNAME}') {
    throw 'DB_USERNAME ne peut pas être vide.'
}
if ([string]::IsNullOrWhiteSpace($env:DB_PASSWORD) -or $env:DB_PASSWORD -eq '${DB_PASSWORD}') {
    $securePassword = Read-Host 'Mot de passe MySQL' -AsSecureString
    $env:DB_PASSWORD = [System.Net.NetworkCredential]::new('', $securePassword).Password
}
if ([string]::IsNullOrWhiteSpace($env:DB_PASSWORD) -or $env:DB_PASSWORD -eq '${DB_PASSWORD}') {
    throw 'DB_PASSWORD ne peut pas être vide.'
}
if (-not $env:JWT_SECRET) {
    $jwtBytes = New-Object byte[] 32
    $random = [System.Security.Cryptography.RandomNumberGenerator]::Create()
    try {
        $random.GetBytes($jwtBytes)
    } finally {
        $random.Dispose()
    }
    $env:JWT_SECRET = [Convert]::ToBase64String($jwtBytes)
}
try {
    $configuredJwtBytes = [Convert]::FromBase64String($env:JWT_SECRET)
    if ($configuredJwtBytes.Length -lt 32) {
        throw 'Le secret décodé est trop court.'
    }
} catch {
    throw 'JWT_SECRET doit être en Base64 et représenter au moins 32 octets aléatoires.'
}

$maven = Get-Command 'mvn.cmd' -ErrorAction SilentlyContinue
if ($maven) {
    # Remove stale classes that can be left by IDE compilers before starting.
    & $maven.Source -o clean spring-boot:run
} else {
    & (Join-Path $PSScriptRoot 'mvnw.cmd') -o clean spring-boot:run
}
exit $LASTEXITCODE
