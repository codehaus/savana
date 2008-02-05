@ECHO OFF

REM  Savana - Transactional Workspaces for Subversion
REM  Copyright (C) 2006  Bazaarvoice Inc.
REM
REM  This file is part of Savana.
REM
REM  This program is free software; you can redistribute it and/or
REM  modify it under the terms of the GNU Lesser General Public License
REM  as published by the Free Software Foundation; either version 3
REM  of the License, or (at your option) any later version.
REM
REM  This program is distributed in the hope that it will be useful,
REM  but WITHOUT ANY WARRANTY; without even the implied warranty of
REM  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
REM  GNU Lesser General Public License for more details.
REM
REM  You should have received a copy of the GNU Lesser General Public License
REM  along with this program; if not, write to the Free Software
REM  Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
REM
REM  Third party components of this software are provided or made available only subject
REM  to their respective licenses. The relevant components and corresponding
REM  licenses are listed in the "licenses" directory in this distribution. In any event,
REM  the disclaimer of warranty and limitation of liability provision in this Agreement
REM  will apply to all Software in this distribution.

SETLOCAL

:setupVariables
SET BIN_DIR=%SAVANA_HOME%\bin
SET LIB_DIR=%SAVANA_HOME%\lib
SET OUT_DIR=%SAVANA_HOME%\out
SET RES_DIR=%SAVANA_HOME%\res

:setupClasspath
setlocal EnableDelayedExpansion
SET CLASSPATH=%SAVANA_HOME%
FOR %%J IN ("%LIB_DIR%\*.jar") DO SET CLASSPATH=!CLASSPATH!;%%J
rem FOR %%J IN ("%LIB_DIR%/*.jar") DO ECHO [%%J] 
endlocal & set CLASSPATH=%CLASSPATH%

:parseArguments
SET SCRIPT_NAME=%1
SHIFT

java -classpath "%CLASSPATH%" -Dsavana.home=%SAVANA_HOME% org.codehaus.savana.scripts.SVNScript "%SCRIPT_NAME%" %1 %2 %3 %4 %5 %6 %7 %8 %9
GOTO end

:end
ENDLOCAL
