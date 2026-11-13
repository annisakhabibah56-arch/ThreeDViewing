@echo off
setlocal
cd /d "%~dp0"
call :pastikan lwjgl-stb-3.3.3.jar
if errorlevel 1 exit /b 1
call :pastikan lwjgl-stb-3.3.3-natives-windows.jar
if errorlevel 1 exit /b 1
if not exist build mkdir build
if not exist app mkdir app
javac --release 17 -encoding UTF-8 -cp "lib/*" -d build ^
 src/main/java/com/grafkom/viewing3d/Camera3D.java ^
 src/main/java/com/grafkom/viewing3d/PerspectiveProjection.java ^
 src/main/java/com/grafkom/viewing3d/ProjectionTaxonomyManager.java ^
 src/main/java/com/grafkom/viewing3d/StereoAnaglyphRenderer.java ^
 src/main/java/com/grafkom/viewing3d/Resources.java ^
 src/main/java/com/grafkom/viewing3d/HSRManager.java ^
 src/main/java/com/grafkom/viewing3d/ShaderProgram.java ^
 src/main/java/com/grafkom/viewing3d/Texture2D.java ^
 src/main/java/com/grafkom/viewing3d/ShadowMapFBO.java ^
 src/main/java/com/grafkom/viewing3d/MeshBuilder.java ^
 src/main/java/com/grafkom/viewing3d/Mesh3D.java ^
 src/main/java/com/grafkom/viewing3d/SceneRenderer.java ^
 src/main/java/com/grafkom/viewing3d/ThreeDViewing.java
if errorlevel 1 exit /b 1
xcopy /E /I /Y /Q "src\main\resources" "build" >nul
if errorlevel 1 exit /b 1
jar "-J-Djava.io.tmpdir=%CD%/build" --create --file app/ThreeDViewing.jar -C build .
if errorlevel 1 exit /b 1
echo Build berhasil. Jalankan run.cmd.
exit /b 0

:pastikan
rem Library STBImage (modul Praktikum 3) diunduh otomatis dari Maven Central bila belum ada di folder lib.
if exist "lib\%~1" exit /b 0
echo Mengunduh %~1 dari Maven Central ...
powershell -NoProfile -ExecutionPolicy Bypass -Command "[Net.ServicePointManager]::SecurityProtocol=[Net.SecurityProtocolType]::Tls12; Invoke-WebRequest -UseBasicParsing -Uri 'https://repo1.maven.org/maven2/org/lwjgl/lwjgl-stb/3.3.3/%~1' -OutFile 'lib\%~1'"
if errorlevel 1 (
 if exist "lib\%~1" del "lib\%~1"
 echo Gagal mengunduh %~1 - pastikan internet aktif atau unduh manual ke folder lib.
 exit /b 1
)
exit /b 0
