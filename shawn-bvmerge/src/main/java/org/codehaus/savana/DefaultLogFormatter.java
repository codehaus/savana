/*
 * Savana - Transactional Workspaces for Subversion
 * Copyright (C) 2009  Bazaarvoice Inc.
 * <p/>
 * This file is part of Savana.
 * <p/>
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 * <p/>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * <p/>
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 *
 * Third party components of this software are provided or made available only subject
 * to their respective licenses. The relevant components and corresponding
 * licenses are listed in the "licenses" directory in this distribution. In any event,
 * the disclaimer of warranty and limitation of liability provision in this Agreement
 * will apply to all Software in this distribution.
 *
 * ====================================================================
 * Copyright (c) 2004-2008 TMate Software Ltd.  All rights reserved.
 *
 * This software is licensed as described in the file COPYING, which
 * you should have received as part of this distribution.  The terms
 * are also available at http://svnkit.com/license.html.
 * If newer versions of this license are posted there, you may use a
 * newer version instead, at your option.
 * ====================================================================
 */
package org.codehaus.savana;

import org.apache.commons.lang.exception.ExceptionUtils;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;


/**
 * Default formatter for the Java logger infrastructure.
 * <p>
 * Similar to the SVNKit DefaultSVNDebugFormatter class but uses OS-specific line
 * separators and adds the date to the timestamp. 
 */
public class DefaultLogFormatter extends Formatter {

    private static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

    public String format(LogRecord record) {
        StringBuilder sb = new StringBuilder();

        // log message
        String message = formatMessage(record);
        sb.append("[");
        Date date = new Date(record.getMillis());
        synchronized (DATE_FORMAT) {
            sb.append(DATE_FORMAT.format(date));
        }
        sb.append("] ");
        sb.append(message);
        sb.append(System.getProperty("line.separator"));

        // log exception
        if (record.getThrown() != null) {
            try {
                sb.append(ExceptionUtils.getStackTrace(record.getThrown()));
            } catch (Exception e) {
            }
        }

        return sb.toString();
    }
}
