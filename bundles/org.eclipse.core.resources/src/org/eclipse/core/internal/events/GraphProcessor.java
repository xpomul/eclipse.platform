/*******************************************************************************
 * Copyright (c) 2018 Red Hat Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Mickael Istria (Red Hat Inc.)
 *******************************************************************************/
package org.eclipse.core.internal.events;

import java.util.*;
import java.util.function.BiConsumer;
import org.eclipse.core.internal.resources.ComputeProjectOrder;
import org.eclipse.core.internal.resources.ComputeProjectOrder.Digraph;
import org.eclipse.core.internal.resources.ComputeProjectOrder.Digraph.Edge;
import org.eclipse.core.internal.resources.ComputeProjectOrder.VertexOrder;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobGroup;

/**
 *
 */
class GraphProcessor<T> {

	final private Digraph<T> graph;
	final private Set<T> toProcess;
	final private Set<T> processing;
	final private Set<T> processed;
	final private VertexOrder<T> sequentialOrder;
	final private JobGroup buildJobGroup;
	final BiConsumer<T, GraphProcessor<T>> processor;

	GraphProcessor(Digraph<T> graph1, Class<T> clazz, final BiConsumer<T, GraphProcessor<T>> processor, JobGroup buildJobGroup) {
		this.graph = graph1;
		this.processor = processor;
		this.buildJobGroup = buildJobGroup;
		toProcess = new HashSet<>(graph.vertexMap.keySet());
		processing = new HashSet<>();
		processed = new HashSet<>();
		sequentialOrder = ComputeProjectOrder.computeVertexOrder(graph, clazz);
	}

	private boolean complete() {
		return processed.size() == graph.vertexList.size();
	}

	private boolean allTriggered() {
		return toProcess.isEmpty();
	}

	private void markProcessing(T item) {
		if (!toProcess.remove(item)) {
			throw new IllegalArgumentException();
		}
		processing.add(item);
	}

	void markProcessed(T item) {
		if (!processing.remove(item)) {
			throw new IllegalArgumentException();
		}
		processed.add(item);
	}

	private Set<T> computeReadyVertexes() {
		Set<T> res = new HashSet<>(toProcess);
		for (T item : toProcess) {
			for (Edge<T> edge : graph.getEdges()) {
				if (edge.to == item && !processed.contains(edge.from)) {
					res.remove(item);
				}
			}
		}
		if (res.isEmpty() && !isProcessing()) { // nothing ready, nothing running: a cycle!
			for (T id : sequentialOrder.vertexes) {
				if (!isProcessed(id)) {
					return Collections.singleton(id);
				}
			}
		}
		return res;
	}

	private boolean isProcessing() {
		return !processing.isEmpty();
	}

	private boolean isProcessed(T item) {
		return processed.contains(item);
	}

	public T[] getSequentialOrder() {
		return this.sequentialOrder.vertexes;
	}

	public void processGraphWithParallelJobs() {
		if (!complete()) {
			if (!allTriggered()) {
				Set<T> readyToBuild = computeReadyVertexes();
				readyToBuild.forEach(item -> triggerJob(item));
			}
		}
	}

	private void triggerJob(T item) {
		synchronized (this) {
			markProcessing(item);
		}
		Job buildJob = new Job(item.toString()) {

			@Override
			protected IStatus run(IProgressMonitor monitor) {
				processor.accept(item, GraphProcessor.this);
				synchronized (GraphProcessor.this) {
					markProcessed(item);
					// do it as part of Job so we're sure following jobs are triggered before this one completes,
					// so we can safely rely on join(family)
					processGraphWithParallelJobs();
				}
				return Status.OK_STATUS;
			}

			@Override
			public boolean belongsTo(Object family) {
				return super.belongsTo(family) || family == GraphProcessor.this;
			}
		};
		buildJob.setJobGroup(buildJobGroup);
		buildJob.schedule();
	}

}