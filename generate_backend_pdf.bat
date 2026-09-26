@echo off
title Apex Bank ATM Backend & Database PDF Generator
echo ====================================================
echo Generating Backend Implementation & Database Integration PDF Report...
echo ====================================================
if not exist bin mkdir bin
javac -encoding UTF-8 -d bin src\com\atm\model\*.java src\com\atm\db\*.java src\com\atm\service\*.java src\com\atm\ui\*.java src\com\atm\ui\screens\*.java src\com\atm\pdf\*.java src\com\atm\Main.java
if %errorlevel% neq 0 (
    echo Compilation failed! Please ensure JDK 21 is installed and on PATH.
    pause
    exit /b %errorlevel%
)
java -Dfile.encoding=UTF-8 -cp bin com.atm.Main --backend-pdf
echo.
echo ====================================================
echo Report generated successfully!
echo File: ATM_Backend_And_Database_Integration_Report.pdf
echo ====================================================
pause
