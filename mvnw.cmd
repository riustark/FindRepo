@ECHO OFF
@REM Official Maven Wrapper script with fallback support
IF EXIST "%~dp0.tools\maven\bin\mvn.cmd" (
    "%~dp0.tools\maven\bin\mvn.cmd" %*
) ELSE (
    mvn %*
)
