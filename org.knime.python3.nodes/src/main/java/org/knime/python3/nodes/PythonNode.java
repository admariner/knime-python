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
 *   Feb 22, 2022 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.python3.nodes;

import java.util.List;

import org.knime.core.node.NodeDescription;
import org.knime.core.node.port.PortType;
import org.knime.python3.nodes.extension.ExtensionNode;
import org.knime.python3.nodes.extension.ExtensionNodeSetFactory.PortSpecifier;
import org.knime.python3.nodes.ports.PythonPortTypeRegistry;

/**
 * Represents a PythonNode.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
public final class PythonNode implements ExtensionNode {

    private final String m_id;

    private final String m_categoryPath;

    private final String m_afterId;

    private final String[] m_keywords;

    private final List<PortSpecifier> m_inputPortSpecifiers;

    private final List<PortSpecifier> m_outputPortSpecifiers;

    private final int m_numViews;

    private final NodeDescription m_description;

    private final boolean m_isDeprecated;

    private final boolean m_isHidden;

    private final ExtensionNodeView[] m_viewResources;

    /**
     * Constructor.
     *
     * @param id of the node
     * @param categoryPath path to the category the node is contained in in the node repository
     * @param afterId id of the node after which to insert this node
     * @param keywords
     * @param description the node's description
     * @param numViews
     * @param isDeprecated whether the node is deprecated
     * @param isHidden whether the node is hidden from node repository
     * @param viewResources
     * @param inputPortSpecifiers
     * @param outputPortSpecifiers
     */
    public PythonNode(final String id, //
        final String categoryPath, //
        final String afterId, //
        final String[] keywords, //
        final NodeDescription description, //
        final int numViews, //
        final boolean isDeprecated, //
        final boolean isHidden, //
        final ExtensionNodeView[] viewResources, //
        final List<PortSpecifier> inputPortSpecifiers, //
        final List<PortSpecifier> outputPortSpecifiers) {

        m_id = id;
        m_categoryPath = categoryPath;
        m_afterId = afterId;
        m_keywords = keywords;
        m_description = description;

        m_numViews = numViews;
        m_isDeprecated = isDeprecated;
        m_isHidden = isHidden;
        m_viewResources = viewResources;

        m_inputPortSpecifiers = inputPortSpecifiers;
        m_outputPortSpecifiers = outputPortSpecifiers;

    }

    /**
     * @return id of the node
     */
    @Override
    public String getId() {
        return m_id;
    }

    /**
     * @return category path
     */
    @Override
    public String getCategoryPath() {
        return m_categoryPath;
    }

    /**
     * @return id of the node after which to insert this node
     */
    @Override
    public String getAfterId() {
        return m_afterId;
    }

    /**
     * @return keywords
     */
    @Override
    public String[] getKeywords() {
        return m_keywords;
    }

    /**
     * @return the nodes description
     */
    @Override
    public NodeDescription getNodeDescription() {
        return m_description;
    }

    @Override
    public PortSpecifier[] getInputPorts() {
        return m_inputPortSpecifiers.toArray(PortSpecifier[]::new);

    }

    @Override
    public PortSpecifier[] getOutputPorts() {
        return m_outputPortSpecifiers.toArray(PortSpecifier[]::new);
    }

    /**
     * @return The number of views offered by this node
     */
    @Override
    public int getNumViews() {
        return m_numViews;
    }

    @Override
    public boolean isDeprecated() {
        return m_isDeprecated;
    }

    @Override
    public boolean isHidden() {
        return m_isHidden;
    }

    @Override
    public ExtensionNodeView[] getExtensionNodeView() {
        return m_viewResources;
    }

    /**
     * Returns an array of input port types for all input ports.
     *
     * @return an array of PortType representing the types of the input ports.
     */
    public PortType[] getInputPortTypes() {
        return m_inputPortSpecifiers.stream() //
            .map(portSpecifier -> PythonPortTypeRegistry.getPortTypeForIdentifier(portSpecifier.typeString()))
            .toArray(PortType[]::new);
    }

    /**
     * Returns an array of output port types for all output ports.
     *
     * @return an array of PortType representing the types of the output ports.
     */
    public PortType[] getOutputPortTypes() {
        return m_outputPortSpecifiers.stream() //
            .map(portSpecifier -> PythonPortTypeRegistry.getPortTypeForIdentifier(portSpecifier.typeString()))
            .toArray(PortType[]::new);
    }
}
