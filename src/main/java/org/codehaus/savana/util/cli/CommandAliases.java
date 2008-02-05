package org.codehaus.savana.util.cli;

import java.io.IOException;
import java.util.*;

/**
 * Savana - Transactional Workspaces for Subversion
 * Copyright (C) 2006  Bazaarvoice Inc.
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
 * @author Bryon Jacob (bryon@jacob.net)
 */
public class CommandAliases {
    private final Map<String, String> aliasMap = new HashMap<String, String>();
    private final Properties props = new Properties();

    public CommandAliases() {
        try {
            props.load(CommandAliases.class.getClassLoader().getResourceAsStream("commands.properties"));
        } catch (IOException e) {
            throw new RuntimeException(e); // TODO: create a "fatal exception" to throw here.
        }
        for (Map.Entry entry : props.entrySet()) {
            String className = String.valueOf(entry.getKey());
            String[] aliases = String.valueOf(entry.getValue()).split(",");
            for (String alias : aliases) {
                aliasMap.put(alias, className);
            }
        }
    }

    public Class loadCommandClassByNameOrAlias(String name) throws ClassNotFoundException {
        String className = aliasMap.get(name);
        return Class.forName(className == null ? name : className);
    }

    public List<String> getCommandList() {
        List<String> commandList = new ArrayList<String>();
        for (Object val : props.values()) {
            String[] vals = String.valueOf(val).split(",", 2);
            commandList.add(vals[0] +
                            ((vals.length > 1) ?
                             ("(" + vals[1] + ")") :
                             ""));
        }
        Collections.sort(commandList);
        return commandList;
    }

}
