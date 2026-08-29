# Push to all three code hosting platforms (PowerShell)
# Usage: .\push-all.ps1 [branch]

param(
    [string]$Branch = "main"
)

Write-Host "🚀 Pushing to GitHub..." -ForegroundColor Green
git push origin $Branch

Write-Host ""
Write-Host "🚀 Pushing to Gitee..." -ForegroundColor Cyan
git push gitee $Branch

Write-Host ""
Write-Host "🚀 Pushing to GitCode..." -ForegroundColor Yellow
git push gitcode $Branch

Write-Host ""
Write-Host "✅ All platforms updated!" -ForegroundColor Green
