package app;

import static app.util.BasicAuthenticator.isAuthorized;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import app.util.Amazon;

//leave commented out if using client certificates
//@WebFilter(urlPatterns = { "/submit" })
public final class AuthenticationFilter implements Filter {

    private static final String AUTHENTICATION_BUCKET = "amsa-authentication";

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        // do nothing
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        if (isAuthorized((HttpServletRequest) request, Amazon.s3(), AUTHENTICATION_BUCKET)) {
            chain.doFilter(request, response);
        } else {
            ((HttpServletResponse) response).sendError(HttpServletResponse.SC_UNAUTHORIZED);
        }
    }

    @Override
    public void destroy() {
        // do nothing
    }

}
