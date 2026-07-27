@echo off
rem ---------------------------------------------------------------------------
rem  MD5Deep GUI - zero-install build for Windows 10/11
rem  Uses the C# compiler that ships with the .NET Framework 4.8 runtime,
rem  which is preinstalled on Windows 11 Pro. No SDK or Visual Studio needed.
rem  Just double-click this file; it produces MD5DeepGUI.exe next to it.
rem ---------------------------------------------------------------------------
setlocal
set CSC=%WINDIR%\Microsoft.NET\Framework64\v4.0.30319\csc.exe
if not exist "%CSC%" set CSC=%WINDIR%\Microsoft.NET\Framework\v4.0.30319\csc.exe
if not exist "%CSC%" (
    echo Could not find the .NET Framework C# compiler at
    echo   %WINDIR%\Microsoft.NET\Framework64\v4.0.30319\csc.exe
    echo Install ".NET Framework 4.8" or build with the .NET SDK instead:
    echo   dotnet build -c Release
    pause
    exit /b 1
)

"%CSC%" /nologo /target:winexe /optimize+ ^
    /out:"%~dp0MD5DeepGUI.exe" ^
    /win32manifest:"%~dp0app.manifest" ^
    /r:System.dll /r:System.Core.dll /r:System.Drawing.dll /r:System.Windows.Forms.dll ^
    "%~dp0src\*.cs"

if errorlevel 1 (
    echo.
    echo Build FAILED.
    pause
    exit /b 1
)
echo.
echo Build OK: %~dp0MD5DeepGUI.exe
pause
