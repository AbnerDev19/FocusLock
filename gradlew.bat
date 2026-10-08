@echo off
setlocal
set ROOT_DIR=%~dp0
set GRADLE_VERSION=8.9
set GRADLE_HOME=%ROOT_DIR%.gradle-dist\gradle-%GRADLE_VERSION%
if exist "%GRADLE_HOME%\bin\gradle.bat" goto run
if not exist "%ROOT_DIR%.gradle-dist" mkdir "%ROOT_DIR%.gradle-dist"
set ARCHIVE=%ROOT_DIR%.gradle-dist\gradle-%GRADLE_VERSION%-bin.zip
if not exist "%ARCHIVE%" (
  powershell -NoProfile -ExecutionPolicy Bypass -Command "Invoke-WebRequest -UseBasicParsing 'https://services.gradle.org/distributions/gradle-%GRADLE_VERSION%-bin.zip' -OutFile '%ARCHIVE%'"
  if errorlevel 1 exit /b 1
)
powershell -NoProfile -ExecutionPolicy Bypass -Command "Expand-Archive -Force '%ARCHIVE%' '%ROOT_DIR%.gradle-dist'"
if errorlevel 1 exit /b 1
:run
call "%GRADLE_HOME%\bin\gradle.bat" %*
endlocal
