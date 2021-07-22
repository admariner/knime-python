/*
 * ------------------------------------------------------------------------
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
 * ------------------------------------------------------------------------
 *
 * History
 *   Sep 25, 2014 (Patrick Winter): created
 */
package org.knime.python2.kernel;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.RunnableFuture;

import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.port.database.DatabaseQueryConnectionSettings;
import org.knime.core.node.workflow.CredentialsProvider;
import org.knime.core.node.workflow.FlowVariable;
import org.knime.python2.PythonCommand;
import org.knime.python2.PythonKernelTester;
import org.knime.python2.PythonKernelTester.PythonKernelTestResult;
import org.knime.python2.PythonModuleSpec;
import org.knime.python2.PythonVersion;
import org.knime.python2.extensions.serializationlibrary.interfaces.TableChunker;
import org.knime.python2.extensions.serializationlibrary.interfaces.TableCreator;
import org.knime.python2.extensions.serializationlibrary.interfaces.TableCreatorFactory;
import org.knime.python2.generic.ImageContainer;
import org.knime.python2.kernel.messaging.TaskHandler;
import org.knime.python2.port.PickledObject;

/**
 * Provides operations on a Python kernel running in another process.
 *
 * @author Patrick Winter, KNIME AG, Zurich, Switzerland
 * @author Clemens von Schwerin, KNIME GmbH, Konstanz, Germany
 * @author Marcel Wiedenmann, KNIME GmbH, Konstanz, Germany
 * @author Christian Dietz, KNIME GmbH, Konstanz, Germany
 */
public class PythonKernel implements AutoCloseable {

    static void testInstallation(final PythonCommand command,
        final Collection<PythonModuleSpec> additionalRequiredModules) throws PythonInstallationTestException {
        final PythonKernelTestResult testResult = command.getPythonVersion() == PythonVersion.PYTHON3
            ? PythonKernelTester.testPython3Installation(command, additionalRequiredModules, false)
            : PythonKernelTester.testPython2Installation(command, additionalRequiredModules, false);
        if (testResult.hasError()) {
            throw new PythonInstallationTestException(
                "Could not start Python kernel. Error during Python installation test: " + testResult.getErrorLog(),
                testResult);
        }
    }

    private final PythonKernelBackend m_delegate;

    /**
     * @see OldPythonKernelBackend#OldPythonKernelBackend(PythonCommand)
     */
    public PythonKernel(final PythonCommand command) throws PythonIOException {
        m_delegate = new OldPythonKernelBackend(command);
    }

    /**
     * Creates a new Python kernel by starting a Python process and connecting to it.
     * <P>
     * Important: Call the {@link #close()} method when this kernel is no longer needed to shut down the Python process
     * in the background.
     *
     * @param kernelOptions The {@link PythonKernelOptions} according to which this kernel instance is configured.
     * @throws PythonInstallationTestException See {@link #PythonKernel(PythonCommand)} and
     *             {@link #setOptions(PythonKernelOptions)}.
     * @throws PythonIOException See {@link #PythonKernel(PythonCommand)} and {@link #setOptions(PythonKernelOptions)}.
     * @deprecated Use {@link #PythonKernel(PythonCommand)} followed by {@link #setOptions(PythonKernelOptions)}
     *             instead. The latter ignores the deprecated Python version and command entries of
     *             {@link PythonKernelOptions}
     */
    @Deprecated
    public PythonKernel(final PythonKernelOptions kernelOptions) throws PythonIOException {
        this(kernelOptions.getUsePython3() //
            ? kernelOptions.getPython3Command() //
            : kernelOptions.getPython2Command());
        setOptions(kernelOptions);
    }

    /**
     * @see org.knime.python2.kernel.PythonKernelBackend#getPythonCommand()
     */
    public PythonCommand getPythonCommand() {
        return m_delegate.getPythonCommand();
    }

    /**
     * @see org.knime.python2.kernel.PythonKernelBackend#getOptions()
     */
    public PythonKernelOptions getOptions() {
        return m_delegate.getOptions();
    }

    /**
     * @see org.knime.python2.kernel.PythonKernelBackend#setOptions(org.knime.python2.kernel.PythonKernelOptions)
     */
    public void setOptions(final PythonKernelOptions options) throws PythonIOException {
        m_delegate.setOptions(options);
    }

    /**
     * @see org.knime.python2.kernel.PythonKernelBackend#registerTaskHandler(java.lang.String,
     *      org.knime.python2.kernel.messaging.TaskHandler)
     */
    public boolean registerTaskHandler(final String taskCategory, final TaskHandler<?> handler) {
        return m_delegate.registerTaskHandler(taskCategory, handler);
    }

    /**
     * @see org.knime.python2.kernel.PythonKernelBackend#unregisterTaskHandler(java.lang.String)
     */
    public boolean unregisterTaskHandler(final String taskCategory) {
        return m_delegate.unregisterTaskHandler(taskCategory);
    }

    /**
     *
     *
     * @see org.knime.python2.kernel.PythonKernelBackend#putFlowVariables(java.lang.String, java.util.Collection)
     */
    public void putFlowVariables(final String name, final Collection<FlowVariable> flowVariables)
        throws PythonIOException {
        m_delegate.putFlowVariables(name, flowVariables);
    }

    /**
     * @see org.knime.python2.kernel.PythonKernelBackend#getFlowVariables(java.lang.String)
     */
    public Collection<FlowVariable> getFlowVariables(final String name) throws PythonIOException {
        return m_delegate.getFlowVariables(name);
    }

    /**
     * @see org.knime.python2.kernel.PythonKernelBackend#putDataTable(java.lang.String,
     *      org.knime.core.node.BufferedDataTable, org.knime.core.node.ExecutionMonitor, int)
     */
    public void putDataTable(final String name, final BufferedDataTable table, final ExecutionMonitor executionMonitor,
        final int rowLimit) throws PythonIOException, CanceledExecutionException {
        m_delegate.putDataTable(name, table, executionMonitor, rowLimit);
    }

    /**
     * @see org.knime.python2.kernel.PythonKernelBackend#putDataTable(java.lang.String,
     *      org.knime.core.node.BufferedDataTable, org.knime.core.node.ExecutionMonitor)
     */
    public void putDataTable(final String name, final BufferedDataTable table, final ExecutionMonitor executionMonitor)
        throws PythonIOException, CanceledExecutionException {
        m_delegate.putDataTable(name, table, executionMonitor);
    }

    /**
     * @see org.knime.python2.kernel.PythonKernelBackend#putData(java.lang.String,
     *      org.knime.python2.extensions.serializationlibrary.interfaces.TableChunker, int,
     *      org.knime.python2.kernel.PythonCancelable)
     */
    public void putData(final String name, final TableChunker tableChunker, final int rowsPerChunk,
        final PythonCancelable cancelable) throws PythonIOException, PythonCanceledExecutionException {
        m_delegate.putData(name, tableChunker, rowsPerChunk, cancelable);
    }

    /**
     * @see org.knime.python2.kernel.PythonKernelBackend#getDataTable(java.lang.String,
     *      org.knime.core.node.ExecutionContext, org.knime.core.node.ExecutionMonitor)
     */
    public BufferedDataTable getDataTable(final String name, final ExecutionContext exec,
        final ExecutionMonitor executionMonitor) throws PythonIOException, CanceledExecutionException {
        return m_delegate.getDataTable(name, exec, executionMonitor);
    }

    /**
     * @see org.knime.python2.kernel.PythonKernelBackend#getData(java.lang.String,
     *      org.knime.python2.extensions.serializationlibrary.interfaces.TableCreatorFactory,
     *      org.knime.python2.kernel.PythonCancelable)
     */
    public TableCreator<?> getData(final String name, final TableCreatorFactory tableCreatorFactory,
        final PythonCancelable cancelable) throws PythonIOException, PythonCanceledExecutionException {
        return m_delegate.getData(name, tableCreatorFactory, cancelable);
    }

    /**
     * @see org.knime.python2.kernel.PythonKernelBackend#putObject(java.lang.String,
     *      org.knime.python2.port.PickledObject)
     */
    public void putObject(final String name, final PickledObject object) throws PythonIOException {
        m_delegate.putObject(name, object);
    }

    /**
     * @see org.knime.python2.kernel.PythonKernelBackend#putObject(java.lang.String,
     *      org.knime.python2.port.PickledObject, org.knime.core.node.ExecutionMonitor)
     */
    public void putObject(final String name, final PickledObject object, final ExecutionMonitor executionMonitor)
        throws PythonIOException, CanceledExecutionException {
        m_delegate.putObject(name, object, executionMonitor);
    }

    /**
     * @see org.knime.python2.kernel.PythonKernelBackend#getObject(java.lang.String,
     *      org.knime.core.node.ExecutionMonitor)
     */
    public PickledObject getObject(final String name, final ExecutionMonitor executionMonitor)
        throws PythonIOException, CanceledExecutionException {
        return m_delegate.getObject(name, executionMonitor);
    }

    /**
     * @see org.knime.python2.kernel.PythonKernelBackend#putSql(java.lang.String,
     *      org.knime.core.node.port.database.DatabaseQueryConnectionSettings,
     *      org.knime.core.node.workflow.CredentialsProvider, java.util.Collection)
     */
    public void putSql(final String name, final DatabaseQueryConnectionSettings conn, final CredentialsProvider cp,
        final Collection<String> jars) throws PythonIOException {
        m_delegate.putSql(name, conn, cp, jars);
    }

    /**
     * @see org.knime.python2.kernel.PythonKernelBackend#getSql(java.lang.String)
     */
    public String getSql(final String name) throws PythonIOException {
        return m_delegate.getSql(name);
    }

    /**
     * @see org.knime.python2.kernel.PythonKernelBackend#getImage(java.lang.String)
     */
    public ImageContainer getImage(final String name) throws PythonIOException {
        return m_delegate.getImage(name);
    }

    /**
     * @see org.knime.python2.kernel.PythonKernelBackend#getImage(java.lang.String,
     *      org.knime.core.node.ExecutionMonitor)
     */
    public ImageContainer getImage(final String name, final ExecutionMonitor executionMonitor)
        throws PythonIOException, CanceledExecutionException {
        return m_delegate.getImage(name, executionMonitor);
    }

    /**
     *
     * @see org.knime.python2.kernel.PythonKernelBackend#listVariables()
     */
    public List<Map<String, String>> listVariables() throws PythonIOException {
        return m_delegate.listVariables();
    }

    /**
     * @see org.knime.python2.kernel.PythonKernelBackend#autoComplete(java.lang.String, int, int)
     */
    public List<Map<String, String>> autoComplete(final String sourceCode, final int line, final int column)
        throws PythonIOException {
        return m_delegate.autoComplete(sourceCode, line, column);
    }

    /**
     * @see org.knime.python2.kernel.PythonKernelBackend#createExecutionTask(org.knime.python2.kernel.messaging.TaskHandler,
     *      java.lang.String)
     */
    public <T> RunnableFuture<T> createExecutionTask(final TaskHandler<T> handler, final String sourceCode) {
        return m_delegate.createExecutionTask(handler, sourceCode);
    }

    /**
     * @see org.knime.python2.kernel.PythonKernelBackend#execute(java.lang.String)
     */
    public String[] execute(final String sourceCode) throws PythonIOException {
        return m_delegate.execute(sourceCode);
    }

    /**
     * @see org.knime.python2.kernel.PythonKernelBackend#execute(java.lang.String,
     *      org.knime.python2.kernel.PythonCancelable)
     */
    public String[] execute(final String sourceCode, final PythonCancelable cancelable)
        throws PythonIOException, CanceledExecutionException {
        return m_delegate.execute(sourceCode, cancelable);
    }

    /**
     * @see org.knime.python2.kernel.PythonKernelBackend#executeAsync(java.lang.String)
     */
    public String[] executeAsync(final String sourceCode) throws PythonIOException {
        return m_delegate.executeAsync(sourceCode);
    }

    /**
     * @see org.knime.python2.kernel.PythonKernelBackend#executeAsync(java.lang.String,
     *      org.knime.python2.kernel.PythonCancelable)
     */
    public String[] executeAsync(final String sourceCode, final PythonCancelable cancelable)
        throws PythonIOException, CanceledExecutionException {
        return m_delegate.executeAsync(sourceCode, cancelable);
    }

    /**
     * @see org.knime.python2.kernel.PythonKernelBackend#resetWorkspace()
     */
    public void resetWorkspace() throws PythonIOException {
        m_delegate.resetWorkspace();
    }

    /**
     * @see org.knime.python2.kernel.PythonKernelBackend#close()
     */
    @Override
    public void close() throws PythonKernelCleanupException {
        m_delegate.close();
    }

    /**
     * @see org.knime.python2.kernel.PythonKernelBackend#addStdoutListener(org.knime.python2.kernel.PythonOutputListener)
     */
    public void addStdoutListener(final PythonOutputListener listener) {
        m_delegate.addStdoutListener(listener);
    }

    /**
     * @see org.knime.python2.kernel.PythonKernelBackend#addStderrorListener(org.knime.python2.kernel.PythonOutputListener)
     */
    public void addStderrorListener(final PythonOutputListener listener) {
        m_delegate.addStderrorListener(listener);
    }

    /**
     * @see org.knime.python2.kernel.PythonKernelBackend#removeStdoutListener(org.knime.python2.kernel.PythonOutputListener)
     */
    public void removeStdoutListener(final PythonOutputListener listener) {
        m_delegate.removeStdoutListener(listener);
    }

    /**
     * @see org.knime.python2.kernel.PythonKernelBackend#removeStderrorListener(org.knime.python2.kernel.PythonOutputListener)
     */
    public void removeStderrorListener(final PythonOutputListener listener) {
        m_delegate.removeStderrorListener(listener);
    }

    /**
     * @see org.knime.python2.kernel.PythonKernelBackend#getDefaultStdoutListener()
     */
    public PythonOutputListener getDefaultStdoutListener() {
        return m_delegate.getDefaultStdoutListener();
    }

    /**
     * @see org.knime.python2.kernel.PythonKernelBackend#getCommands()
     */
    public PythonCommands getCommands() {
        return m_delegate.getCommands();
    }
}
