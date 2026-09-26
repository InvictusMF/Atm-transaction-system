@echo off
title ATM System - PDF Report Generator
echo ====================================================
echo Generating Single PDF Output with All Screenshots...
echo ====================================================
if not exist bin mkdir bin
javac -d bin src\com\atm\model\*.java src\com\atm\db\*.java src\com\atm\service\*.java src\com\atm\ui\*.java src\com\atm\ui\screens\*.java src\com\atm\pdf\*.java src\com\atm\Main.java
if %errorlevel% neq 0 (
    echo Compilation failed!
    pause
    exit /b %errorlevel%
)
java -cp bin com.atm.Main --generate-pdf
echo.
echo Check the generated PDF: ATM_Transaction_Simulation_System_Report.pdf
pause
