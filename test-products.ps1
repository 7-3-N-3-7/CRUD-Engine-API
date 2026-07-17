curl.exe -s -X POST http://localhost:8081/auth/realms/crud-realm/protocol/openid-connect/token `
  -H "Content-Type: application/x-www-form-urlencoded" `
  -d "client_id=test-client&grant_type=client_credentials&client_secret=test-client-secret" `
  -o token.json

$t = (Get-Content -Raw token.json | ConvertFrom-Json).access_token
Write-Host "Token length: $($t.Length)"

curl.exe -s http://localhost:8080/api/products -H "Authorization: Bearer $t"
