/*
 * Copyright (C) 2010  Sylvain Maucourt (smaucourt@gmail.com)
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307
 *
 */
package net.sylvek.sharemyposition.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@SuppressWarnings("serial")
public class ShortyUrlServletImpl extends HttpServlet {

    private static final String TINY_URI = "http://tinyurl.com/api-create.php?url=";

    private static final Logger log = Logger.getLogger(ShortyUrlServletImpl.class.getName());

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
    {
        try {
            String url = req.getParameter("url");
            if (url == null) {
                log.severe("url is empty!");
                throw new ServletException("url is empty");
            }

            /**
             * error req.getParameter doesn't work properly
             */
            if (!url.contains("&geocode")) {
                log.info("rewrite the url, we add an &");
                url = url.replace("geocode", "&geocode");
            }

            log.info("shorting for " + url);
            URL tinyUrl = new URL(TINY_URI + URLEncoder.encode(url, "UTF-8"));
            BufferedReader reader = new BufferedReader(new InputStreamReader(tinyUrl.openStream()));
            String line = reader.readLine();
            reader.close();
            if (line == null) {
                log.warning("tinyurl send us nothing for " + url);
                line = url;
            } else {
                resp.getWriter().print(line);
                resp.getWriter().flush();
            }
        } catch (MalformedURLException e) {
            throw new ServletException(e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
    {
        throw new ServletException("unsupported post method");
    }
}
