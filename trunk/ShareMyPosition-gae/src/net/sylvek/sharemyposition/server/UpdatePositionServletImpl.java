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

import com.google.appengine.api.memcache.ErrorHandlers;
import com.google.appengine.api.memcache.MemcacheService;
import com.google.appengine.api.memcache.MemcacheServiceFactory;

import java.io.IOException;
import java.util.logging.Level;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @author smaucourt@gmail.com
 * 
 */
@SuppressWarnings("serial")
public class UpdatePositionServletImpl extends HttpServlet {

    public static final String PARAMETER_POSITION = "pos";

    public static final String PARAMETER_UUID = "uuid";

    public static final String PARAMETER_TRACKED = "tracked";

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
    {
        final String position = request.getParameter(PARAMETER_POSITION);
        final String uuid = request.getParameter(PARAMETER_UUID);
        final MemcacheService syncCache = MemcacheServiceFactory.getMemcacheService();
        syncCache.setErrorHandler(ErrorHandlers.getConsistentLogAndContinue(Level.INFO));
        syncCache.put(uuid, Cache.to(position));
    }

    public static class Cache {

        private static final String SEPARATOR = "::";

        public String position;

        public long uptime;

        private Cache()
        {
        }

        public static String to(final String position)
        {
            final Cache c = new Cache();
            c.position = position;
            c.uptime = System.currentTimeMillis();
            return c.position + SEPARATOR + c.uptime;
        }

        public static Cache from(final Object from)
        {
            final Cache c = new Cache();
            if (from instanceof String) {
                final String[] s = ((String) from).split(SEPARATOR, 2);
                c.position = s[0];
                c.uptime = Long.parseLong(s[1]);
            }
            return c;
        }

        @Override
        public String toString()
        {
            String[] coords = position.split(",", 2);
            double latitude = Double.parseDouble(coords[0]);
            double longitude = Double.parseDouble(coords[1]);
            StringBuilder sb = new StringBuilder();
            sb.append("{")
                    .append("\"latitude\":")
                    .append(latitude)
                    .append(",")
                    .append("\"longitude\":")
                    .append(longitude)
                    .append(",")
                    .append("\"uptime\":")
                    .append(uptime)
                    .append("}");
            return sb.toString();
        }
    }
}
