# 运行 app 模块单元测试
$root = Split-Path -Parent $MyInvocation.MyCommand.Path
Push-Location $root
try {
    .\gradlew.bat :app:testDebugUnitTest 2>&1
    if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
} finally {
    Pop-Location
}