#!/bin/bash
# -*- coding: UTF-8 -*-

# 🔄 Nettoyage des anciens fichiers
rm -rf build
rm -f myframework.jar

# 📁 Création du dossier build
mkdir -p build

# 🧱 Compilation
javac -encoding UTF-8 -d build -cp lib/javax.servlet-api-4.0.1.jar src/fw/FrontServlet.java

# 📦 Création du JAR
jar cvf myframework.jar -C build .

echo "✅ myframework.jar généré avec succès !"
