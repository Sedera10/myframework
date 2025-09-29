@echo off
REM Encodage UTF-8 pour Windows
chcp 65001 >nul

REM Nettoyage des anciens fichiers
rmdir /S /Q build 2>nul
del /Q framework.jar 2>nul

REM Création du dossier build
mkdir build

REM Compilation
javac -encoding UTF-8 -d build -cp lib\javax.servlet-api-4.0.1.jar src\fw\FrontServlet.java

REM Création du JAR
jar cvf framework.jar -C build .

echo ✅ framework.jar généré avec succès !
pause
