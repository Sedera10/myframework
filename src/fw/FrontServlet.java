package fw;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.PrintWriter;

public class FrontServlet extends HttpServlet {

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

            resp.setContentType("text/html;charset=UTF=8");
            try (PrintWriter out = resp.getWriter()) {
                    out.println("<html>");
                    out.println("<head><title>FrontServlet</title></head>");
                    out.println("<body>");
                    out.println("<h1>Reponse depuis FrontServlet !</h1>");
                    out.println("<p>Methode HTTP utilisée : " + req.getMethod() + "</p>");
                    out.println("<p>URL : " + req.getRequestURL() + "</p>");
                    out.println("</body>");
                    out.println("</html>");
                } 
            }

    }

}
