/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME AG, Zurich, Switzerland
 *  Website: http://www.knime.com; Email: contact@knime.com
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License, Version 3, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, see <http://www.gnu.org/licenses>.
 *
 *  Additional permission under GNU GPL version 3 section 7:
 *
 *  KNIME interoperates with ECLIPSE solely via ECLIPSE's plug-in APIs.
 *  Hence, KNIME and ECLIPSE are both independent programs and are not
 *  derived from each other. Should, however, the interpretation of the
 *  GNU GPL Version 3 ("License") under any applicable laws result in
 *  KNIME and ECLIPSE being a combined program, KNIME AG herewith grants
 *  you the additional permission to use and propagate KNIME together with
 *  ECLIPSE with only the license terms in place for ECLIPSE applying to
 *  ECLIPSE and the GNU GPL Version 3 applying for KNIME, provided the
 *  license terms of ECLIPSE themselves allow for the respective use and
 *  propagation of ECLIPSE together with KNIME.
 *
 *  Additional permission relating to nodes for KNIME that extend the Node
 *  Extension (and in particular that are based on subclasses of NodeModel,
 *  NodeDialog, and NodeView) and that only interoperate with KNIME through
 *  standard APIs ("Nodes"):
 *  Nodes are deemed to be separate and independent programs and to not be
 *  covered works.  Notwithstanding anything to the contrary in the
 *  License, the License does not apply to Nodes, you are not required to
 *  license Nodes under the License, and you are granted a license to
 *  prepare and propagate Nodes, in each case even if such Nodes are
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ---------------------------------------------------------------------
 *
 * History
 *   Oct 14, 2024 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.python3.nodes.ports.extension;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * A map from {@link Class} to values that searches for matches in the class hierarchy if there is not exact match. The
 * search first recursively searches all interfaces and only if that produces no results moves up to the super class.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
final class ClassHierarchyMap<T, V> {

    private final Map<Class<? extends T>, V> m_byType = new HashMap<>();

    private final Class<T> m_upperTypeBound;

    ClassHierarchyMap(final Class<T> upperTypeBound) {
        m_upperTypeBound = upperTypeBound;
    }

    V put(final Class<? extends T> type, final V value) {
        return m_byType.put(type, value);
    }

    /**
     * Gets the value for type. First checks if there is a value for the exact type. If there is no exact match, all
     * direct interfaces of the type that satisfy the upper type bound are (recursively) checked. If that does not
     * succeed, the super process is recursively repeated for the super class.
     *
     * @param type to get the corresponding value for
     * @return the value for type
     */
    V getHierarchyAware(final Class<? extends T> type) {
        V value = m_byType.get(type);
        if (value != null) {
            return value;
        }

        var valueForInterface = Stream.of(type.getInterfaces())//
            .filter(m_upperTypeBound::isAssignableFrom)//
            .map(i -> getHierarchyAware(i.asSubclass(m_upperTypeBound)))//
            .filter(Objects::nonNull)//
            .findFirst();
        if (valueForInterface.isPresent()) {
            return valueForInterface.get();
        }
        var superClass = type.getSuperclass();
        if (superClass != null && m_upperTypeBound.isAssignableFrom(superClass)) {
            return getHierarchyAware(superClass.asSubclass(m_upperTypeBound));
        }
        return null;
    }

}