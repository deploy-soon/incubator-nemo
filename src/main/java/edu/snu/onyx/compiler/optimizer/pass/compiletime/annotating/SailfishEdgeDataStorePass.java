/*
 * Copyright (C) 2017 Seoul National University
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package edu.snu.onyx.compiler.optimizer.pass.compiletime.annotating;

import edu.snu.onyx.common.dag.DAG;
import edu.snu.onyx.compiler.ir.IREdge;
import edu.snu.onyx.compiler.ir.IRVertex;
import edu.snu.onyx.compiler.ir.executionproperty.ExecutionProperty;
import edu.snu.onyx.compiler.ir.executionproperty.edge.DataStoreProperty;
import edu.snu.onyx.runtime.executor.data.stores.LocalFileStore;
import edu.snu.onyx.runtime.executor.data.stores.MemoryStore;
import edu.snu.onyx.runtime.executor.datatransfer.communication.ScatterGather;

/**
 * A pass to support Sailfish-like shuffle by tagging edges.
 * This pass handles the DataStore ExecutionProperty.
 */
public final class SailfishEdgeDataStorePass extends AnnotatingPass {
  public static final String SIMPLE_NAME = "SailfishEdgeDataStorePass";

  public SailfishEdgeDataStorePass() {
    super(ExecutionProperty.Key.DataStore);
  }

  @Override
  public DAG<IRVertex, IREdge> apply(final DAG<IRVertex, IREdge> dag) {
    dag.getVertices().forEach(vertex -> {
      // Find the merger vertex inserted by reshaping pass.
      if (dag.getIncomingEdgesOf(vertex).stream().anyMatch(irEdge ->
          ScatterGather.class.equals(irEdge.getProperty(ExecutionProperty.Key.DataCommunicationPattern)))) {
        dag.getIncomingEdgesOf(vertex).forEach(edgeToMerger -> {
          if (ScatterGather.class.equals(edgeToMerger.getProperty(ExecutionProperty.Key.DataCommunicationPattern))) {
            // Pass data through memory to the merger vertex.
            edgeToMerger.setProperty(DataStoreProperty.of(MemoryStore.class));
          }
        });
        dag.getOutgoingEdgesOf(vertex).forEach(edgeFromMerger ->
            // Merge the input data and write it immediately to the remote disk.
            edgeFromMerger.setProperty(DataStoreProperty.of(LocalFileStore.class)));
      }
    });
    return dag;
  }
}