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
 *   24 Nov 2022 (chaubold): created
 */
package org.knime.python3;

import java.util.ArrayList;
import java.util.EventObject;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.eclipse.equinox.internal.p2.engine.PhaseEvent;
import org.eclipse.equinox.internal.p2.engine.RollbackOperationEvent;
import org.eclipse.equinox.internal.provisional.p2.core.eventbus.ProvisioningListener;
import org.eclipse.equinox.p2.engine.PhaseSetFactory;
import org.knime.core.node.NodeLogger;

/**
 * Gate that controls Python kernel creation. Allows to block kernel creation and to kill all Python processes if needed
 *
 * @author Carsten Haubold, KNIME GmbH, Konstanz, Germany
 */
@SuppressWarnings("restriction")
public class PythonKernelCreationGate implements ProvisioningListener {

    /**
     * Interface for listeners to the {@link PythonKernelCreationGate} opening and closing
     */
    public static interface PythonKernelCreationGateListener {
        /**
         * Called as soon as creating kernels becomes possible
         */
        void onPythonKernelCreationGateOpen();

        /**
         * Called as soon as creating kernels becomes blocked
         */
        void onPythonKernelCreationGateClose();
    }

    /**
     * The singleton instance of the Gate
     */
    public static final PythonKernelCreationGate INSTANCE = new PythonKernelCreationGate();

    public void blockPythonCreation() {
        if (m_blockCount.getAndIncrement() == 0) {
            m_kernelLock.writeLock().lock();

            synchronized (m_listeners) {
                m_listeners.forEach(PythonKernelCreationGateListener::onPythonKernelCreationGateClose);
            }
        }
    }

    public void allowPythonCreation() {
        if (m_blockCount.getAndDecrement() == 1) {
            m_kernelLock.writeLock().unlock();

            synchronized (m_listeners) {
                m_listeners.forEach(PythonKernelCreationGateListener::onPythonKernelCreationGateOpen);
            }
        }
    }

    public boolean isPythonKernelCreationAllowed() {
        return !m_kernelLock.isWriteLocked();
    }

    public void awaitPythonKernelCreationAllowedInterruptibly() throws InterruptedException {
        m_kernelLock.readLock().lockInterruptibly();
        m_kernelLock.readLock().unlock();
    }

    public void awaitPythonKernelCreationAllowed() {
        m_kernelLock.readLock().lock();
        m_kernelLock.readLock().unlock();
    }

    public void registerListener(final PythonKernelCreationGateListener listener) {
        synchronized (m_listeners) {
            m_listeners.add(listener);
        }
    }

    public void deregisterListener(final PythonKernelCreationGateListener listener) {
        synchronized (m_listeners) {
            m_listeners.remove(listener);
        }
    }

    /**
     * Called whenever a ProvisioningEvent is fired by Eclipse's event bus
     */
    @Override
    public void notify(final EventObject o) {
        if (o instanceof PhaseEvent && ((PhaseEvent)o).getPhaseId().equals(PhaseSetFactory.PHASE_INSTALL)
            && ((PhaseEvent)o).getType() == PhaseEvent.TYPE_START) {
            // lock if we enter the "install" phase
            LOGGER.info("Blocking Python process startup during installation");
            INSTANCE.blockPythonCreation();
        } else if (o instanceof PhaseEvent && ((PhaseEvent)o).getPhaseId().equals(PhaseSetFactory.PHASE_CONFIGURE)
            && ((PhaseEvent)o).getType() == PhaseEvent.TYPE_START) {
            // "configure" is the normal phase after install, so we can unlock Python processes again
            LOGGER.info("Allowing Python process startup again after installation");
            INSTANCE.allowPythonCreation();
        } else if (o instanceof RollbackOperationEvent) {
            // According to org.eclipse.equinox.internal.p2.engine.Engine.perform() -> L92,
            // a RollbackOperationEvent will be fired if an operation failed, and this event is only fired in that case,
            // so we unlock if we are currently locked.
            if (!INSTANCE.isPythonKernelCreationAllowed()) {
                LOGGER.info("Allowing Python process startup again after installation failed");
                INSTANCE.allowPythonCreation();
            }
        }
    }

    private static final NodeLogger LOGGER = NodeLogger.getLogger(PythonKernelCreationGate.class);

    private ReentrantReadWriteLock m_kernelLock = new ReentrantReadWriteLock();

    private AtomicInteger m_blockCount = new AtomicInteger(0);

    private final List<PythonKernelCreationGateListener> m_listeners = new ArrayList<>();

}