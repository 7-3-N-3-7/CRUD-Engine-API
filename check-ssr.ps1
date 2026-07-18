$html = (Invoke-WebRequest -Uri http://localhost:3000 -UseBasicParsing).Content
Write-Host "Page length: $($html.Length) chars"

# Check if translation values appear (not raw keys)
$checks = @("Architecture Overview", "API Tester", "CRUD Engine", "nav.architecture", "nav.apitester", "architecture.title")
foreach ($term in $checks) {
    if ($html -match [regex]::Escape($term)) {
        Write-Host "FOUND:   $term"
    } else {
        Write-Host "MISSING: $term"
    }
}
