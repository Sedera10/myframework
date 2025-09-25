rm -rf build mon-framework.jar

mkdir -p build

javac -d build -cp lib/jakarta.servlet-api-6.0.0.jar src/framework/FrontServlet.java

jar cvf my-framework.jar -C build .

echo "✅ mon-framework.jar généré avec succès !"
