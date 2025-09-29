rm -rf build mon-framework.jar

mkdir -p build

javac -encoding UTF-8 -d build -cp lib\javax.servlet-api-4.0.1.jar src\fw\FrontServlet.java

jar cvf my-framework.jar -C build .

echo "✅ mon-framework.jar généré avec succès !"
