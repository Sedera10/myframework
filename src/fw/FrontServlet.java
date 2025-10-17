package fw;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URL;

public class FrontServlet extends HttpServlet {

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        findResource(req, resp);
    }

    private void findResource(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String path = req.getRequestURI().substring(req.getContextPath().length());
        URL resource = getServletContext().getResource(path);

        // Vérifie si la ressource existe physiquement dans webapp
        boolean resourceExist = (resource != null);

        // Si c’est un fichier statique (html, jsp, css, js, image, etc.)
        if (resourceExist && !path.endsWith(".class")) {

            // 🧠 Important : ne pas faire forward vers soi-même !
            // Envoie directement la ressource réelle
            getServletContext().getNamedDispatcher("default").forward(req, resp);
            return;
        }

        // Sinon, traitement applicatif (page ou route logique)
        resp.setContentType("text/html;charset=UTF-8");
        try (PrintWriter out = resp.getWriter()) {
            out.println("<html>");
            out.println("<head><title>FrontServlet</title></head>");
            out.println("<body>");
            out.println("<h1>Réponse FrontServlet</h1>");
            out.println("<p>Ressource demandée : " + path + "</p>");
            if (!resourceExist) {
                out.println("<p style='color:red'>Ressource introuvable</p>");
            }
            out.println("</body>");
            out.println("</html>");
        }
    }
}
