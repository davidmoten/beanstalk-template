package app;

import java.io.IOException;

import javax.json.Json;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

@WebServlet(urlPatterns = "/info")
public final class InfoServlet extends HttpServlet {

    private static final long serialVersionUID = -8403570868273621200L;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        String now = DateTime.now(DateTimeZone.UTC).toString();
        String json = Json.createObjectBuilder().add("version", Version.version())
                .add("datetime", now).build().toString();
        response.setContentType("application/json");
        response.getWriter().println(json);
    }

}
