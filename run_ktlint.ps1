# 运行全项目 KtLint 检查
$root = Split-Path -Parent $MyInvocation.MyCommand.Path
Push-Location $root
try {
    .\gradlew.bat ktlintCheck 2>&1
    if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
} finally {
    Pop-Location
}