package fw;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.PrintWriter;

public class FrontServlet extends HttpServlet {

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        resp.setContentType("text/html;charset=UTF-8");
        try (PrintWriter out = resp.getWriter()) {
            out.println("<html>");
            out.println("<head><title>FrontServlet</title></head>");
            out.println("<body>");
            out.println("<h1>Réponse depuis FrontServlet !</h1>");
            out.println("<p>Méthode HTTP utilisée : " + req.getMethod() + "</p>");
            out.println("<p>URL : " + req.getRequestURL() + "</p>");
            out.println("</body>");
            out.println("</html>");
        }
    }
}
