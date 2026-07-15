@echo off
cd /d "C:\Users\LOQ\StudioProjects\Pundar-stellar"
call gradlew.bat clean assembleDebug
echo.
echo BUILD RESULT: %ERRORLEVEL%
pause

