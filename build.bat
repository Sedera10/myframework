@echo off
REM Encodage UTF-8 pour Windows
chcp 65001 >nul

REM Nettoyage des anciens fichiers
rmdir /S /Q build 2>nul
del /Q my-framework.jar 2>nul

REM Création du dossier build
mkdir build

REM Compilation
javac -encoding UTF-8 -d build -cp lib\jakarta.servlet-api-6.0.0.jar src\framework\FrontServlet.java

REM Création du JAR
jar cvf my-framework.jar -C build .

echo ✅ mon-framework.jar généré avec succès !
pause
