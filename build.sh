rm -rf build mon-framework.jar

mkdir -p build

javac -d build -cp lib/jakarta.servlet-api-6.0.0.jar src/myframework/fw/FrontServlet.java
javac -d build -cp lib/jakarta.servlet-api-6.0.0.jar src/myframework/annotation/MyMapping.java

jar cvf myframework.jar -C build .

echo "✅ myframework.jar généré avec succès !"
