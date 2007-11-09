@ECHO OFF
SETLOCAL


:setupVariables
SET BIN_DIR=%SVNSCRIPTS_HOME%\bin
SET LIB_DIR=%SVNSCRIPTS_HOME%\lib
SET OUT_DIR=%SVNSCRIPTS_HOME%\out
SET RES_DIR=%SVNSCRIPTS_HOME%\res

:setupClasspath
setlocal EnableDelayedExpansion
SET CLASSPATH=%SVNSCRIPTS_HOME%
FOR %%J IN ("%LIB_DIR%\*.jar") DO SET CLASSPATH=!CLASSPATH!;%%J
rem FOR %%J IN ("%LIB_DIR%/*.jar") DO ECHO [%%J] 
endlocal & set CLASSPATH=%CLASSPATH%

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
IF "%SCRIPT_NAME%"=="createreleasebranch" GOTO createReleaseBranch
IF "%SCRIPT_NAME%"=="crb" GOTO createReleaseBranch

IF "%SCRIPT_NAME%"=="createuserbranch" GOTO createUserBranch
IF "%SCRIPT_NAME%"=="cub" GOTO createUserBranch
IF "%SCRIPT_NAME%"=="cb" GOTO createUserBranch

IF "%SCRIPT_NAME%"=="deleteuserbranch" GOTO deleteUserBranch
IF "%SCRIPT_NAME%"=="dub" GOTO deleteUserBranch
IF "%SCRIPT_NAME%"=="db" GOTO deleteUserBranch

IF "%SCRIPT_NAME%"=="diffchangesfromsource" GOTO diffChangesFromSource
IF "%SCRIPT_NAME%"=="diff" GOTO diffChangesFromSource

IF "%SCRIPT_NAME%"=="listchangesfromsource" GOTO listChangesFromSource
IF "%SCRIPT_NAME%"=="listchanges" GOTO listChangesFromSource
IF "%SCRIPT_NAME%"=="lc" GOTO listChangesFromSource

IF "%SCRIPT_NAME%"=="listuserbranches" GOTO listUserBranches
IF "%SCRIPT_NAME%"=="listbranches" GOTO listUserBranches
IF "%SCRIPT_NAME%"=="lb" GOTO listUserBranches

IF "%SCRIPT_NAME%"=="listworkingcopyinfo" GOTO listWorkingCopyInfo
IF "%SCRIPT_NAME%"=="lwci" GOTO listWorkingCopyInfo
IF "%SCRIPT_NAME%"=="workingcopyinfo" GOTO listWorkingCopyInfo
IF "%SCRIPT_NAME%"=="wci" GOTO listWorkingCopyInfo
IF "%SCRIPT_NAME%"=="info" GOTO listWorkingCopyInfo

IF "%SCRIPT_NAME%"=="promote" GOTO promote

IF "%SCRIPT_NAME%"=="synchronize" GOTO synchronize
IF "%SCRIPT_NAME%"=="sync" GOTO synchronize
IF "%SCRIPT_NAME%"=="resync" GOTO synchronize

IF "%SCRIPT_NAME%"=="reverttosource" GOTO revertToSource
IF "%SCRIPT_NAME%"=="rs" GOTO revertToSource
IF "%SCRIPT_NAME%"=="revert" GOTO revertToSource

IF "%SCRIPT_NAME%"=="setbranch" GOTO setBranch
IF "%SCRIPT_NAME%"=="sb" GOTO setBranch

GOTO unknownScript

:createReleaseBranch
SET SCRIPT_CLASS=org.codehaus.savana.scripts.CreateBranch
GOTO run
:createUserBranch
SET SCRIPT_CLASS=org.codehaus.savana.scripts.CreateUserBranch
GOTO run
:deleteUserBranch
SET SCRIPT_CLASS=org.codehaus.savana.scripts.DeleteUserBranch
GOTO run
:diffChangesFromSource
SET SCRIPT_CLASS=org.codehaus.savana.scripts.DiffChangesFromSource
GOTO run
:listChangesFromSource
SET SCRIPT_CLASS=org.codehaus.savana.scripts.ListChangesFromSource
GOTO run
:listUserBranches
SET SCRIPT_CLASS=org.codehaus.savana.scripts.ListUserBranches
GOTO run
:listWorkingCopyInfo
SET SCRIPT_CLASS=org.codehaus.savana.scripts.ListWorkingCopyInfo
GOTO run
:promote
SET SCRIPT_CLASS=org.codehaus.savana.scripts.Promote
GOTO run
:synchronize
SET SCRIPT_CLASS=org.codehaus.savana.scripts.Synchronize
GOTO run
:revertToSource
SET SCRIPT_CLASS=org.codehaus.savana.scripts.RevertToSource
GOTO run
:setBranch
SET SCRIPT_CLASS=org.codehaus.savana.scripts.SetBranch
GOTO run


:run
java -classpath "%CLASSPATH%" -Dsavana.home=%SAVANA_HOME% org.codehaus.savana.scripts.SVNScript "%SCRIPT_CLASS%" %1 %2 %3 %4 %5 %6 %7 %8 %9
GOTO end


:unknownScript
ECHO Unknown command: %SCRIPT_NAME%
GOTO usage


:usage
ECHO usage^: ss ^<subcommand^> ^[args^]
ECHO.
ECHO Available subcommands:
ECHO     createreleasebranch (crb)
ECHO     createuserbranch (cub, cb)
ECHO     deleteuserbranch (dub, db)
ECHO     listchanges (lc, diff)
ECHO     listuserbranches (lb, lub)
ECHO     listworkingcopyinfo (lwci, workingcopyinfo, wci, info)
ECHO     promote
ECHO     reverttosource (rs, revert)
ECHO     setbranch (sb)
ECHO     synchronize (sync, resync)
GOTO end


:end
ENDLOCAL
