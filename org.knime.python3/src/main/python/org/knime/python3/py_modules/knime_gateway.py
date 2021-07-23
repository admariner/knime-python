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
@author Benjamin Wilhelm, KNIME GmbH, Konstanz, Germany
"""
import os
import sys
import importlib
from collections.abc import Iterable

from py4j.clientserver import ClientServer, JavaParameters, PythonParameters

client_server = None

DATA_SOURCES = {}
DATA_SINKS = {}


class EntryPoint:
    """The base class for all entry points.

    Methods in this class and subclasses are called by a Java process and are therefore
    entry points into the Python process.
    """

    def getPid(self):  # NOSONAR: Method name defined in Java interface
        return os.getpid()

    def registerExtensions(self, extensions):  # NOSONAR: Method name defined in Java interface
        # Note: Import errors are given back to the Java caller
        for ext in extensions:
            importlib.import_module(ext)

    class Java:
        implements = ["org.knime.python3.PythonEntryPoint"]


def connect_to_knime(entry_point: EntryPoint):
    # The first two program arguments are always the ports
    java_port = int(sys.argv[1])
    python_port = int(sys.argv[2])

    # Create the client server
    global client_server
    client_server = ClientServer(
        java_parameters=JavaParameters(port=java_port),
        python_parameters=PythonParameters(
            port=python_port, propagate_java_exceptions=True
        ),
        python_server_entry_point=entry_point,
    )


def data_source(identifier: str):
    # TODO docstring
    def f(mapper):
        DATA_SOURCES[identifier] = mapper
        return mapper

    return f


def data_sink(identifier: str):
    # TODO docstring
    def f(mapper):
        DATA_SINKS[identifier] = mapper
        return mapper

    return f


def map_data_source(java_data_source):
    """Map a Java object which provides data to an Python object which gives access to the data
    using a Pythonic API.

    The mapper for a type of data must be decorated with the decorator `@knime_gateway.data_source(identifier)`.

    Args:
        java_data_source: The Java object providing data. The object must implement the method `getIdentifier`
            which must return the indentifer of the decorated mapper. Or a list of such objects.

    Returns:
        A Python object which provides the data. Or a list of such objects.

    Raises:
        ValueError: If no mapper is registered for the type of data source.
    """

    def map(d):
        identifier = d.getIdentifier()
        if identifier not in DATA_SOURCES:
            raise ValueError(
                f"No mapper registerd for identifier {identifier}. "
                "Are you missing a KNIME Extension?"
                "If this is your own extension make sure to register a mapper by "
                "decorating a function with `@knime_gateway.data_source(identifier)`."
            )
        return DATA_SOURCES[identifier](d)

    if isinstance(java_data_source, Iterable):
        return [map(d) for d in java_data_source]
    else:
        return map(java_data_source)


def map_data_sink(java_data_sink):
    """Map a Java object which collects data to an Python object with a Pythonic API.

    The mapper for a type of data must be decorated with the decorator `@knime_gateway.data_sink(identifier)`.

    Args:
        java_data_sink: The Java object collecting data. The object must implement the method `getIdentifier`
            which must return the indentifer of the decorated mapper. Or a list of such objects.

    Returns:
        A Python object which collects the data. Or a list of such objects.

    Raises:
        ValueError: If no mapper is registered for the type of data sink.
    """

    def map(d):
        identifier = d.getIdentifier()
        if identifier not in DATA_SOURCES:
            raise ValueError(
                f"No mapper registerd for identifier {identifier}. "
                "Are you missing a KNIME Extension?"
                "If this is your own extension make sure to register a mapper by "
                "decorating a function with `@knime_gateway.data_sink(identifier)`."
            )
        return DATA_SINKS[identifier](d)

    if isinstance(java_data_sink, Iterable):
        return [map(d) for d in java_data_sink]
    else:
        return map(java_data_sink)
