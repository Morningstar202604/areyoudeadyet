@echo off
rem 运行全项目 KtLint 检查
cd /d "%~dp0"
call gradlew.bat ktlintCheck 2>&1
exit /b %ERRORLEVEL%