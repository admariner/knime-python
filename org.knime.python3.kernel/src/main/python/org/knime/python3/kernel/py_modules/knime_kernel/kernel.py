# -*- coding: utf-8 -*-
# ------------------------------------------------------------------------
#  Copyright by KNIME AG, Zurich, Switzerland
#  Website: http://www.knime.com; Email: contact@knime.com
#
#  This program is free software; you can redistribute it and/or modify
#  it under the terms of the GNU General Public License, Version 3, as
#  published by the Free Software Foundation.
#
#  This program is distributed in the hope that it will be useful, but
#  WITHOUT ANY WARRANTY; without even the implied warranty of
#  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
#  GNU General Public License for more details.
#
#  You should have received a copy of the GNU General Public License
#  along with this program; if not, see <http://www.gnu.org/licenses>.
#
#  Additional permission under GNU GPL version 3 section 7:
#
#  KNIME interoperates with ECLIPSE solely via ECLIPSE's plug-in APIs.
#  Hence, KNIME and ECLIPSE are both independent programs and are not
#  derived from each other. Should, however, the interpretation of the
#  GNU GPL Version 3 ("License") under any applicable laws result in
#  KNIME and ECLIPSE being a combined program, KNIME AG herewith grants
#  you the additional permission to use and propagate KNIME together with
#  ECLIPSE with only the license terms in place for ECLIPSE applying to
#  ECLIPSE and the GNU GPL Version 3 applying for KNIME, provided the
#  license terms of ECLIPSE themselves allow for the respective use and
#  propagation of ECLIPSE together with KNIME.
#
#  Additional permission relating to nodes for KNIME that extend the Node
#  Extension (and in particular that are based on subclasses of NodeModel,
#  NodeDialog, and NodeView) and that only interoperate with KNIME through
#  standard APIs ("Nodes"):
#  Nodes are deemed to be separate and independent programs and to not be
#  covered works.  Notwithstanding anything to the contrary in the
#  License, the License does not apply to Nodes, you are not required to
#  license Nodes under the License, and you are granted a license to
#  prepare and propagate Nodes, in each case even if such Nodes are
#  propagated with or for interoperation with KNIME.  The owner of a Node
#  may freely choose the license terms applicable to such Node, including
#  when such Node is propagated with or for interoperation with KNIME.
# ------------------------------------------------------------------------

"""
@author Marcel Wiedenmann, KNIME GmbH, Konstanz, Germany
"""

import sys

import knime
import pyarrow as pa

from io import StringIO


class PythonKernel(knime.client.EntryPoint):

    def __init__(self):
        self._workspace = {}

    def putTableIntoWorkspace(self, name: str, data_source):
        source = knime.data.mapDataProvider(data_source)
        try:
            batches = (source[i] for i in range(len(source)))
            data_frame = pa.Table.from_batches(batches).to_pandas()
            self._workspace[name] = data_frame
        finally:
            source.close()

    def getTableFromWorkspace(self, name: str, data_sink):
        sink = knime.data.mapDataCallback(data_sink)
        try:
            data_frame = self._workspace[name]
            batches = pa.Table.from_pandas(data_frame).to_batches()
            for batch in batches:
                sink.write(batch)
        finally:
            sink.close()

    def execute(self, source_code: str):
        # Log outputs/errors(/warnings) to both stdout/stderr and variables. Note that we do _not_ catch any otherwise
        # uncaught exceptions here for the purpose of logging. That is, uncaught exceptions will regularly lead to the
        # exceptional termination of this method and need to be handled, and possibly logged, by the callers of this
        # method.
        stdout = sys.stdout
        output = StringIO()
        sys.stdout = PythonKernel._TeeingLogger(stdout, output)
        stderr = sys.stderr
        error = StringIO()
        sys.stderr = PythonKernel._TeeingLogger(stderr, error)

        try:
            exec(source_code, self._workspace)
        finally:
            sys.stdout = stdout
            sys.stderr = stderr

        return [output.getvalue(), error.getvalue()]

    class _TeeingLogger(object):

        def __init__(self, original, branch):
            self._original = original
            self._branch = branch

        def write(self, message):
            self._original.write(message)
            self._branch.write(message)

        def writelines(self, sequence):
            self._original.writelines(sequence)
            self._branch.writelines(sequence)

        def flush(self):
            self._original.flush()
            self._branch.flush()

        def isatty(self):
            return False

    class Java:
        implements = ["org.knime.python3.kernel.NewPythonKernelBackendProxy"]


if __name__ == "__main__":
    try:
        # Hook into warning delivery.
        import warnings

        default_showwarning = warnings.showwarning


        def showwarning_hook(message, category, filename, lineno, file=None, line=None):
            """
            Copied from warnings.showwarning.
            We use this hook to prefix warning messages with "[WARN]". This makes them easier identifiable on Java
            side and helps printing them using the correct log level.
            Providing a custom hook is supported as per the API documentations:
            https://docs.python.org/2/library/warnings.html#warnings.showwarning
            https://docs.python.org/3/library/warnings.html#warnings.showwarning
            """
            try:
                if file is None:
                    file = sys.stderr
                    if file is None:
                        # sys.stderr is None when run with pythonw.exe - warnings get lost
                        return
                try:
                    # Do not change the prefix. Expected on Java side.
                    file.write("[WARN]" + warnings.formatwarning(message, category, filename, lineno, line))
                except OSError:
                    pass  # the file (probably stderr) is invalid - this warning gets lost.
            except Exception:
                # Fall back to default implementation.
                return default_showwarning(message, category, filename, lineno, file, line)


        warnings.showwarning = showwarning_hook
    except Exception:
        pass

    knime.client.connectToJava(PythonKernel())
