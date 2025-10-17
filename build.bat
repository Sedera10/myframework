@echo off
REM Encodage UTF-8 pour Windows
chcp 65001 >nul

REM Nettoyage des anciens fichiers
rmdir /S /Q build 2>nul
del /Q myframework.jar 2>nul

REM Création du dossier build
mkdir build

REM Compilation
javac -encoding UTF-8 -d build -cp lib\jakarta.servlet-api-6.0.0.jar src\fw\FrontServlet.java

REM Création du JAR
jar cvf myframework.jar -C build .

echo ✅ myframework.jar généré avec succès !
pause
