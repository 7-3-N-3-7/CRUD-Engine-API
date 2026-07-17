$config = @"
Host github.com
    HostName github.com
    User git
    IdentityFile ~/.ssh/yubiPassKey--16-07-26-sk
    IdentitiesOnly yes
"@
Set-Content -Path "$env:USERPROFILE\.ssh\config" -Value $config
Write-Host "SSH config updated"

# Now push
Set-Location "c:\Users\vase_\Desktop\Crud_application\crud-frontend"
git push -u origin main --force
