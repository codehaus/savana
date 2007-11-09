@ECHO OFF
SETLOCAL


:setupVariables
SET BIN_DIR=%SVNSCRIPTS_HOME%\bin
SET LIB_DIR=%SVNSCRIPTS_HOME%\lib
SET OUT_DIR=%SVNSCRIPTS_HOME%\out
SET RES_DIR=%SVNSCRIPTS_HOME%\res
SET CLASSPATH=%LIB_DIR%\commons-lang-2.1.jar;%LIB_DIR%\commons-logging-1.0.4.jar;%LIB_DIR%\ganymed.jar;%LIB_DIR%\svnkit-cli.jar;%LIB_DIR%\svnkit-javahl.jar;%LIB_DIR%\svnkit.jar;%LIB_DIR%\svnscripts.jar;%LIB_DIR%\log4j-1.2.9.jar;%OUT_DIR%\svnscripts.jar;%RES_DIR%


:parseArguments
IF "%1"=="" GOTO usage
IF "%1"=="/?" GOTO usage
IF "%1"=="-?" GOTO usage
IF "%1"=="/h" GOTO usage
IF "%1"=="-h" GOTO usage
IF "%1"=="/help" GOTO usage
IF "%1"=="-help" GOTO usage
IF "%1"=="help" GOTO usage
SET SCRIPT_NAME=%1
SHIFT

:getScriptClass
IF "%SCRIPT_NAME%"=="createmetadatafile" GOTO createMetadataFile

GOTO unknownScript


GOTO run
:createMetadataFile
SET SCRIPT_CLASS=org.codehaus.savana.scripts.admin.CreateMetadataFile
GOTO run


:run
java -classpath "%CLASSPATH%" org.codehaus.savana.scripts.SVNScript "%SCRIPT_CLASS%" %1 %2 %3 %4 %5 %6 %7 %8 %9
GOTO end


:unknownScript
ECHO Unknown command: %SCRIPT_NAME%
GOTO usage


:usage
ECHO usage^: ssadmin ^<subcommand^> ^[args^]
ECHO.
ECHO Available subcommands:
ECHO     createmetadatafile
GOTO end


:end
ENDLOCAL