@if "%DEBUG%"=="" @echo off
@rem Gradle startup script for Windows

set DIRNAME=%~dp0
if "%DIRNAME%"=="" set DIRNAME=.
set APP_BASE_NAME=%~n0

if defined JAVA_HOME set JAVA_EXE=%JAVA_HOME%\bin\java.exe
if not defined JAVA_HOME set JAVA_EXE=java.exe

"%JAVA_EXE%" "-Xmx2048m" "-Dfile.encoding=UTF-8" -classpath "%DIRNAME%gradle\wrapper\gradle-wrapper.jar" org.gradle.wrapper.GradleWrapperMain %*
