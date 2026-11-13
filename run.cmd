@echo off
setlocal
cd /d "%~dp0"
call build.cmd
if errorlevel 1 exit /b 1
java -cp "app\ThreeDViewing.jar;lib\*" com.grafkom.viewing3d.ThreeDViewing
if errorlevel 1 (
 echo.
 echo Aplikasi gagal. Pastikan Java 17+ 64-bit terpasang dan driver OpenGL 3.3 tersedia.
 pause
)
