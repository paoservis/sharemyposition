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

import net.sylvek.sharemyposition.server.UpdatePositionServletImpl.Cache;

import org.apache.http.HttpStatus;

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
public class RetrievePositionServletImpl extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
    {
        final String uuid = request.getParameter(UpdatePositionServletImpl.PARAMETER_UUID);
        if (uuid == null || uuid.trim().equals("")) {
            response.sendError(HttpStatus.SC_BAD_REQUEST, "?uuid= mandatory");
        }
        
        final MemcacheService syncCache = MemcacheServiceFactory.getMemcacheService();
        syncCache.setErrorHandler(ErrorHandlers.getConsistentLogAndContinue(Level.INFO));
        final Object inMemory = syncCache.get(uuid);
        
        if (inMemory == null) {
            response.sendError(HttpStatus.SC_NOT_FOUND, "element not found");
        }
        
        final Cache cache = Cache.from(inMemory);
        response.getWriter().write(cache.toString());
    }
    
}
