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
 *   Jun 30, 2022 (benjamin): created
 */
package org.knime.python3.scripting.nodes2;

import java.util.Optional;
import java.util.Set;

import org.knime.core.webui.data.RpcDataService;
import org.knime.core.webui.node.dialog.NodeDialog;
import org.knime.core.webui.node.dialog.NodeSettingsService;
import org.knime.core.webui.node.dialog.SettingsType;
import org.knime.core.webui.page.Page;

/**
 * The node dialog implementation of the Python Scripting nodes.
 *
 * @author Benjamin Wilhelm, KNIME GmbH, Berlin, Germany
 */
@SuppressWarnings("restriction") // the UIExtension node dialog API is still restricted
public final class PythonScriptNodeDialog implements NodeDialog {

    private final PythonScriptingService m_scriptingService;

    /**
     * Create a new scripting editor dialog
     *
     * @param hasView if the node has an output view
     */
    public PythonScriptNodeDialog(final boolean hasView) {
        m_scriptingService = new PythonScriptingService(hasView);
    }

    @Override
    public Set<SettingsType> getSettingsTypes() {
        return Set.of(SettingsType.MODEL);
    }

    @Override
    public Page getPage() {
        return Page //
            .builder(PythonScriptNodeDialog.class, "js-src/dist", "index.html") //
            .addResourceDirectory("assets") //
            .addResourceDirectory("monacoeditorwork") //
            .addResource(m_scriptingService::openHtmlPreview, "preview.html") //
            .build();
    }

    @Override
    public Optional<RpcDataService> createRpcDataService() {
        return Optional.of(RpcDataService.builder() //
            .addService("ScriptingService", m_scriptingService.getJsonRpcService()) //
            .onDeactivate(m_scriptingService::onDeactivate) //
            .build());
    }

    @Override
    public NodeSettingsService getNodeSettingsService() {
        return PythonScriptNodeSettings.createNodeSettingsService();
    }

    @Override
    public boolean canBeEnlarged() {
        return true;
    }
}
