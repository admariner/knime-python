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
 *   Jul 22, 2021 (marcel): created
 */
package org.knime.python3.kernel;

import java.io.Flushable;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.RunnableFuture;

import org.knime.core.columnar.arrow.ArrowBatchReadStore;
import org.knime.core.columnar.batch.BatchReadable;
import org.knime.core.columnar.batch.DelegatingBatchReadable;
import org.knime.core.data.columnar.table.ColumnarContainerTable;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.BufferedDataTable.KnowsRowCountTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.port.database.DatabaseQueryConnectionSettings;
import org.knime.core.node.workflow.CredentialsProvider;
import org.knime.core.node.workflow.FlowVariable;
import org.knime.python2.PythonCommand;
import org.knime.python2.extensions.serializationlibrary.interfaces.TableChunker;
import org.knime.python2.extensions.serializationlibrary.interfaces.TableCreator;
import org.knime.python2.extensions.serializationlibrary.interfaces.TableCreatorFactory;
import org.knime.python2.generic.ImageContainer;
import org.knime.python2.kernel.PythonCancelable;
import org.knime.python2.kernel.PythonCanceledExecutionException;
import org.knime.python2.kernel.PythonIOException;
import org.knime.python2.kernel.PythonKernelBackend;
import org.knime.python2.kernel.PythonKernelCleanupException;
import org.knime.python2.kernel.PythonKernelOptions;
import org.knime.python2.kernel.PythonOutputListener;
import org.knime.python2.kernel.messaging.TaskHandler;
import org.knime.python2.port.PickledObject;
import org.knime.python3.PythonDataCallback;
import org.knime.python3.PythonDataProvider;
import org.knime.python3.PythonExtension;
import org.knime.python3.PythonGateway;
import org.knime.python3.PythonModuleKnime;
import org.knime.python3.PythonPath;
import org.knime.python3.PythonPath.PythonPathBuilder;
import org.knime.python3.arrow.PythonArrowDataUtils;
import org.knime.python3.arrow.PythonModuleKnimeArrow;

/**
 * @author Marcel Wiedenmann, KNIME GmbH, Konstanz, Germany
 */
public final class NewPythonKernelBackend implements PythonKernelBackend {

    private final PythonGateway<NewPythonKernelBackendProxy> m_gateway;

    private final NewPythonKernelBackendProxy m_proxy;

    private PythonKernelOptions m_currentOptions;

    public NewPythonKernelBackend(final PythonCommand command) {
        final String launcherPath = Paths.get(System.getProperty("user.dir"), "py", "kernel_launcher.py").toString(); // TODO
        final PythonPath pythonPath = new PythonPathBuilder() //
            .add(PythonModuleKnime.getPythonModule()) //
            .add(PythonModuleKnimeArrow.getPythonModule()) //
            .add(PythonModuleKnime.getPythonModuleFor(NewPythonKernelBackend.class)) //
            .build();
        final List<PythonExtension> extensions = Collections.emptyList();
        // final List<PythonExtension> extensions =  Collections.singletonList(PythonArrowExtension.INSTANCE); // TODO
        // TODO: conversion between python2 and python3 "PythonCommand"s

        m_gateway = new PythonGateway<NewPythonKernelBackendProxy>(command, launcherPath,
            NewPythonKernelBackendProxy.class, extensions, pythonPath);
        m_proxy = m_gateway.getEntryPoint();
    }

    @Override
    public PythonCommand getPythonCommand() {
        // TODO: conversion between python2 and python3 "PythonCommand"s
        throw new IllegalStateException("not yet implemented"); // TODO: implement
    }

    @Override
    public PythonKernelOptions getOptions() {
        return m_currentOptions;
    }

    @Override
    public void setOptions(final PythonKernelOptions options) throws PythonIOException {
        m_currentOptions = options;
        // TODO: actually apply options
    }

    @Override
    public void putFlowVariables(final String name, final Collection<FlowVariable> flowVariables)
        throws PythonIOException {
        throw new UnsupportedOperationException("not yet implemented"); // TODO: NYI

    }

    @Override
    public Collection<FlowVariable> getFlowVariables(final String name) throws PythonIOException {
        throw new UnsupportedOperationException("not yet implemented"); // TODO: NYI

    }

    @Override
    public void putDataTable(final String name, final BufferedDataTable table, final ExecutionMonitor executionMonitor,
        final int rowLimit) throws PythonIOException, CanceledExecutionException {
        throw new UnsupportedOperationException("not yet implemented"); // TODO: NYI

    }

    @Override
    public void putDataTable(final String name, final BufferedDataTable table, final ExecutionMonitor executionMonitor)
        throws PythonIOException, CanceledExecutionException {
        // TODO: make this step cancelable (e.g. in case flushing caches or similar takes long)? If so, what are we
        // supposed to do upon cancellation? Pass on to the caches? Or simply continue execution here and let them
        // continue in the background?
        try {
            m_proxy.putTableIntoWorkspace(name, tableToProvider(table));
        } catch (IOException ex) {
            throw new PythonIOException(ex);
        }
    }

    @Override
    public void putData(final String name, final TableChunker tableChunker, final int rowsPerChunk,
        final PythonCancelable cancelable) throws PythonIOException, PythonCanceledExecutionException {
        throw new UnsupportedOperationException("not yet implemented"); // TODO: NYI
    }

    @Override
    public BufferedDataTable getDataTable(final String name, final ExecutionContext exec,
        final ExecutionMonitor executionMonitor) throws PythonIOException, CanceledExecutionException {
        // TODO: cancellation
        final PythonDataCallback callback = PythonArrowDataUtils.createCallback(/* TODO */);
        m_proxy.getTableFromWorkspace(name, callback);
        return callbackToTable(callback);
    }

    @Override
    public TableCreator<?> getData(final String name, final TableCreatorFactory tableCreatorFactory,
        final PythonCancelable cancelable) throws PythonIOException, PythonCanceledExecutionException {
        throw new UnsupportedOperationException("not yet implemented"); // TODO: NYI

    }

    @Override
    public void putObject(final String name, final PickledObject object) throws PythonIOException {
        throw new UnsupportedOperationException("not yet implemented"); // TODO: NYI

    }

    @Override
    public void putObject(final String name, final PickledObject object, final ExecutionMonitor executionMonitor)
        throws PythonIOException, CanceledExecutionException {
        throw new UnsupportedOperationException("not yet implemented"); // TODO: NYI

    }

    @Override
    public PickledObject getObject(final String name, final ExecutionMonitor executionMonitor)
        throws PythonIOException, CanceledExecutionException {
        throw new UnsupportedOperationException("not yet implemented"); // TODO: NYI

    }

    @Override
    public void putSql(final String name, final DatabaseQueryConnectionSettings conn, final CredentialsProvider cp,
        final Collection<String> jars) throws PythonIOException {
        throw new UnsupportedOperationException("Database support has been discontinued with the new Python back end.");
    }

    @Override
    public String getSql(final String name) throws PythonIOException {
        throw new UnsupportedOperationException("Database support has been discontinued with the new Python back end.");
    }

    @Override
    public ImageContainer getImage(final String name) throws PythonIOException {
        throw new UnsupportedOperationException("not yet implemented"); // TODO: NYI

    }

    @Override
    public ImageContainer getImage(final String name, final ExecutionMonitor executionMonitor)
        throws PythonIOException, CanceledExecutionException {
        throw new UnsupportedOperationException("not yet implemented"); // TODO: NYI

    }

    @Override
    public List<Map<String, String>> listVariables() throws PythonIOException {
        throw new UnsupportedOperationException("not yet implemented"); // TODO: NYI

    }

    @Override
    public List<Map<String, String>> autoComplete(final String sourceCode, final int line, final int column)
        throws PythonIOException {
        throw new UnsupportedOperationException("not yet implemented"); // TODO: NYI

    }

    @Override
    public <T> RunnableFuture<T> createExecutionTask(final TaskHandler<T> handler, final String sourceCode) {
        throw new UnsupportedOperationException("not yet implemented"); // TODO: NYI

    }

    @Override
    public String[] execute(final String sourceCode) throws PythonIOException {
        return m_proxy.execute(sourceCode).toArray(String[]::new);
    }

    @Override
    public String[] execute(final String sourceCode, final PythonCancelable cancelable)
        throws PythonIOException, CanceledExecutionException {
        // TODO: cancellation
        return m_proxy.execute(sourceCode).toArray(String[]::new);
    }

    @Override
    public String[] executeAsync(final String sourceCode) throws PythonIOException {
        throw new UnsupportedOperationException("not yet implemented"); // TODO: NYI

    }

    @Override
    public String[] executeAsync(final String sourceCode, final PythonCancelable cancelable)
        throws PythonIOException, CanceledExecutionException {
        throw new UnsupportedOperationException("not yet implemented"); // TODO: NYI

    }

    @Override
    public void resetWorkspace() throws PythonIOException {
        throw new UnsupportedOperationException("not yet implemented"); // TODO: NYI

    }

    @Override
    public void close() throws PythonKernelCleanupException {
        try {
            m_gateway.close();
        } catch (final Exception ex) {
            throw new PythonKernelCleanupException(ex);
        }
    }

    @Override
    public void addStdoutListener(final PythonOutputListener listener) {
        throw new UnsupportedOperationException("not yet implemented"); // TODO: NYI

    }

    @Override
    public void addStderrorListener(final PythonOutputListener listener) {
        throw new UnsupportedOperationException("not yet implemented"); // TODO: NYI

    }

    @Override
    public void removeStdoutListener(final PythonOutputListener listener) {
        throw new UnsupportedOperationException("not yet implemented"); // TODO: NYI

    }

    @Override
    public void removeStderrorListener(final PythonOutputListener listener) {
        throw new UnsupportedOperationException("not yet implemented"); // TODO: NYI

    }

    @Override
    public PythonOutputListener getDefaultStdoutListener() {
        throw new UnsupportedOperationException("not yet implemented"); // TODO: NYI
    }

    // TODO: move to Arrow plugin
    private static PythonDataProvider tableToProvider(final BufferedDataTable table) throws IOException {
        final KnowsRowCountTable delegate = table.getDelegate();
        BatchReadable readable = null;
        if (delegate instanceof ColumnarContainerTable) {
            readable = ((ColumnarContainerTable)delegate).getStore();
            // Traverse decorators until reaching the actual underlying store which must be an Arrow store in order for
            // IPC to work. During traversal, flush caches to disk to make the data available to Python.
            // TODO: ideally, we want to be able to flush per batch/up to some batch index. Once this is possible, defer
            // flushing until actually needed.
            // TODO: do we need to flush all caches or only the outermost one which in turn triggers the inner ones?
            while (readable instanceof DelegatingBatchReadable) {
                if (readable instanceof Flushable) {
                    ((Flushable)readable).flush();
                }
                readable = ((DelegatingBatchReadable)readable).getDelegateBatchReadable();
            }
        }
        if (readable instanceof ArrowBatchReadStore) {
            return PythonArrowDataUtils.createProvider((ArrowBatchReadStore)readable);
        } else {
            throw new IllegalStateException(
                "Arrow IPC requires the Arrow implementation of the columnar table back end to be enabled.");
        }
    }

    // TODO: move to Arrow plugin
    private static BufferedDataTable callbackToTable(final PythonDataCallback callback) {

    }
}
