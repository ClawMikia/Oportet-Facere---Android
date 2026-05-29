@echo off
setlocal
set DIRNAME=%~dp0
set APP_BASE_NAME=%~n0
set JAVA_EXE=%JAVA_HOME%\bin\java.exe
if not defined JAVA_HOME (
  set JAVA_EXE=java
)
set CLASSPATH=%DIRNAME%gradle\wrapper\gradle-wrapper.jar
"%JAVA_EXE%" -cp "%CLASSPATH%" org.gradle.wrapper.GradleWrapperMain %*
