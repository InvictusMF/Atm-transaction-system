@echo off
title Apex Bank ATM Simulator
echo ====================================================
echo Starting Apex National Bank ATM Simulation System...
echo ====================================================
if not exist bin mkdir bin
javac -d bin src\com\atm\model\*.java src\com\atm\db\*.java src\com\atm\service\*.java src\com\atm\ui\*.java src\com\atm\ui\screens\*.java src\com\atm\pdf\*.java src\com\atm\Main.java
if %errorlevel% neq 0 (
    echo Compilation failed! Please ensure JDK is on PATH.
    pause
    exit /b %errorlevel%
)
java -cp bin com.atm.Main
pause
