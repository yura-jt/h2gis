/**
 * h2spatial is a library that brings spatial support to the H2 Java database.
 *
 * h2spatial is distributed under GPL 3 license. It is produced by the "Atelier
 * SIG" team of the IRSTV Institute <http://www.irstv.fr/> CNRS FR 2488.
 *
 * Copyright (C) 2007-2012 IRSTV (FR CNRS 2488)
 *
 * h2patial is free software: you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * h2spatial is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * h2spatial. If not, see <http://www.gnu.org/licenses/>.
 *
 * For more information, please consult: <http://www.orbisgis.org/>
 * or contact directly: info_at_ orbisgis.org
 */
package org.h2gis.h2spatial.internal.function.spatial.crs;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import org.cts.parser.proj.ProjKeyParameters;
import org.cts.registry.Registry;
import org.cts.registry.RegistryException;

/**
 * This classe build a registry based on a spatial_ref_sys table stored in the
 * H2 database.
 *
 * @author ebocher
 */
public class SpatialRefRegistry implements Registry {

    private Connection connection;
    Pattern regex = Pattern.compile("\\s+");
    final String crsPS = "SELECT proj4text, auth_name FROM SPATIAL_REF_SYS where srid=?";

    @Override
    public String getRegistryName() {
        return "epsg";
    }

    @Override
    public Map<String, String> getParameters(String code) throws RegistryException {
        try {
            PreparedStatement prepStmt = connection.prepareStatement(crsPS);
            prepStmt.setInt(1, Integer.valueOf(code));
            ResultSet rs = prepStmt.executeQuery();
            if (rs.next()) {
                String proj4Text = rs.getString(1);
                String[] tokens = regex.split(proj4Text);
                Map<String, String> v = new HashMap<String, String>();
                for (String token : tokens) {
                    String[] keyValue = token.split("=");
                    if (keyValue.length == 2) {
                        String key = formatKey(keyValue[0]);
                        ProjKeyParameters.checkUnsupported(key);
                        v.put(key, keyValue[1]);
                    } else {
                        String key = formatKey(token);
                        ProjKeyParameters.checkUnsupported(key);
                        v.put(key, null);
                    }
                }
                if (!v.containsKey(ProjKeyParameters.title)) {
                    v.put(ProjKeyParameters.title, rs.getString(2) + ":" + code);
                }
                prepStmt.close();
                return v;
            }
        } catch (SQLException ex) {
            throw new RegistryException("Cannot obtain the CRS parameters", ex);
        }
        return null;
    }

    /**
     * Remove + char if exists
     *
     * @param key
     * @return
     */
    private static String formatKey(String key) {
        String formatKey = key;
        if (key.startsWith("+")) {
            formatKey = key.substring(1);
        }
        return formatKey;
    }

    @Override
    public Set<String> getSupportedCodes() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    void setConnection(Connection connection) {
        this.connection = connection;
    }
}
