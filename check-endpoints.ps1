$results = @(
  @{ name='/api/translations';       url='http://localhost:3000/api/translations' },
  @{ name='/api/icons/logo';         url='http://localhost:3000/api/icons/logo' },
  @{ name='/api/icons/architecture'; url='http://localhost:3000/api/icons/architecture' },
  @{ name='/api/icons/api';          url='http://localhost:3000/api/icons/api' },
  @{ name='/api/translations (SSR)'; url='http://localhost:3000/' }
)

Write-Host ""
Write-Host "Endpoint health check:"
Write-Host "-------------------------------------"
foreach ($ep in $results) {
  try {
    $r = Invoke-WebRequest -Uri $ep.url -UseBasicParsing -TimeoutSec 5
    Write-Host ("  ✅  HTTP " + $r.StatusCode + "  " + $ep.name)
  } catch {
    Write-Host ("  ❌  ERROR  " + $ep.name + " → " + $_.Exception.Message)
  }
}
Write-Host "-------------------------------------"
