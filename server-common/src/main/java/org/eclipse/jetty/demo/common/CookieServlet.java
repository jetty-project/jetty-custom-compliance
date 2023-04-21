package org.eclipse.jetty.demo.common;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class CookieServlet extends HttpServlet
{
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException
    {
        resp.setCharacterEncoding("utf-8");
        resp.setContentType("text/plain");

        Properties props = new Properties();

        Cookie[] cookies = req.getCookies();
        if (cookies == null)
        {
            props.put("cookies.length", "null");
        }
        else
        {
            props.put("cookies.length", Objects.toString(cookies.length));
            for (Cookie cookie : cookies)
            {
                props.put(String.format("cookie.%s", cookie.getName()), cookie.getValue());
            }
        }

        @SuppressWarnings("unchecked")
        List<String> complianceViolations = (List<String>)req.getAttribute("org.eclipse.jetty.http.compliance.violations");
        if (complianceViolations != null)
        {
            String violationCount = Objects.toString(complianceViolations.size());
            resp.setHeader("X-Compliance-Violations", violationCount);
            props.put("violations.length", violationCount);
            int i = 0;
            for (String violation : complianceViolations)
            {
                props.put("violation." + i++, violation);
            }
        }

        OutputStream out = resp.getOutputStream();
        props.store(out, "From " + CookieServlet.class.getName());
        out.flush();
    }
}
