@echo off

REM  Savana - Transactional Workspaces for Subversion
REM  Copyright (C) 2006-2009 Bazaarvoice Inc.
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

setlocal

set DEFAULT_SAVANA_HOME=%~dp0..

if "%SAVANA_HOME%"=="" set SAVANA_HOME=%DEFAULT_SAVANA_HOME%

rem Add everything in the /lib directory to the classpath
setlocal EnableDelayedExpansion
set SAVANA_CLASSPATH=
for %%J in ("%SAVANA_HOME%\lib\*.jar") do set SAVANA_CLASSPATH=!SAVANA_CLASSPATH!;%%J
endlocal & set SAVANA_CLASSPATH=%SAVANA_CLASSPATH:~1%

set SAVANA_MAINCLASS=org.codehaus.savana.scripts.SAV
set SAVANA_OPTIONS=-Xms128M -Xmx1024M -Djava.util.logging.config.file="%SAVANA_HOME%/logging.properties"

java %SAVANA_OPTIONS% -cp "%SAVANA_CLASSPATH%" %SAVANA_MAINCLASS% %*

endlocal