package app;

import java.io.IOException;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet(urlPatterns = "/submit")
public final class SubmitServlet extends HttpServlet {

    private static final long serialVersionUID = 5479232901304133051L;

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        String a = request.getParameter("a");
        response.getWriter().println("received a=" + a);
    }

}
