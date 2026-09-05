@echo off
rem 运行 app 模块单元测试
cd /d "%~dp0"
call gradlew.bat :app:testDebugUnitTest 2>&1
exit /b %ERRORLEVEL%