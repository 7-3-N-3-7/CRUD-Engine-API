$j = Get-Content -Raw token.json | ConvertFrom-Json
$parts = $j.access_token.Split('.')
$payload = $parts[1]
$padLen = $payload.Length % 4
if ($padLen -ne 0) { $payload = $payload + ('=' * (4 - $padLen)) }
$decoded = [System.Text.Encoding]::UTF8.GetString([System.Convert]::FromBase64String($payload))
$claims = $decoded | ConvertFrom-Json
Write-Host "iss:                $($claims.iss)"
Write-Host "preferred_username: $($claims.preferred_username)"
Write-Host "tenant claim:       $($claims.tenant)"
Write-Host "realm_access roles: $($claims.realm_access.roles -join ', ')"
