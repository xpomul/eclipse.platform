/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.internal.ccvs.ui.actions;

import java.lang.reflect.InvocationTargetException;
import java.util.*;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.mapping.*;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.core.subscribers.Subscriber;
import org.eclipse.team.core.subscribers.SubscriberResourceMappingContext;
import org.eclipse.team.internal.ccvs.core.CVSProviderPlugin;
import org.eclipse.team.internal.ccvs.ui.CVSUIPlugin;
import org.eclipse.ui.PlatformUI;


/**
 * A specialized workspace actions that operates on resource traversals
 * instead of resources/
 */
public abstract class WorkspaceTraversalAction extends WorkspaceAction {

    /**
     * Override to use the roots of the traversals as the selected resources.
     * On it's own, this would be enough to make the actions work but all the operations
     * would be deep (which is bad) so subclasses will need to look for traversals
     * when executed.
     * @see org.eclipse.team.internal.ccvs.ui.actions.WorkspaceAction#getSelectedResources()
     */
    protected IResource[] getSelectedResourcesWithOverlap() {
        try {
            // Get all the traversals since enablement may be based on entire selection
            ResourceTraversal[] traversals = getSelectedTraversals(null, null);
            Set resources = new HashSet();
            for (int i = 0; i < traversals.length; i++) {
                ResourceTraversal traversal = traversals[i];
                resources.addAll(Arrays.asList(traversal.getResources()));
            }
            return (IResource[]) resources.toArray(new IResource[resources.size()]);
        } catch (TeamException e) {
            CVSUIPlugin.log(e);
            return new IResource[0];
        }
    }
    

    /**
     * Return the selected mappings that contain resources 
     * within a CVS managed project.
     * @return the selected mappings that contain resources 
     * within a CVS managed project
     */
    protected ResourceMapping[] getCVSResourceMappings() {
        return getSelectedResourceMappings(CVSProviderPlugin.getTypeId());
    }
    
    protected static IResource[] getRootTraversalResources(ResourceMapping[] mappings, ResourceMappingContext context, IProgressMonitor monitor) throws CoreException {
        List result = new ArrayList();
        for (int i = 0; i < mappings.length; i++) {
            ResourceMapping mapping = mappings[i];
            ResourceTraversal[] traversals = mapping.getTraversals(context, monitor);
            for (int j = 0; j < traversals.length; j++) {
                ResourceTraversal traversal = traversals[j];
                result.addAll(Arrays.asList(traversal.getResources()));
            }
        }
        return (IResource[]) result.toArray(new IResource[result.size()]);
    }

    protected Subscriber getWorkspaceSubscriber() {
        return CVSProviderPlugin.getPlugin().getCVSWorkspaceSubscriber();
    }
    
    protected IResource[] getResourcesToCompare(final Subscriber subscriber) throws InvocationTargetException {
        return getResourcesToCompare(getCVSResourceMappings(), subscriber);
    }
    
    public static IResource[] getResourcesToCompare(final ResourceMapping[] mappings, final Subscriber subscriber) throws InvocationTargetException {
        // Determine what resources need to be synchronized.
        // Use a resource mapping context to include any relevant remote resources
        final IResource[][] resources = new IResource[][] { null };
        try {
            PlatformUI.getWorkbench().getProgressService().busyCursorWhile(new IRunnableWithProgress() {
                public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
                    try {
                        resources[0] = getRootTraversalResources(
                                mappings, 
                                SubscriberResourceMappingContext.getCompareContext(subscriber), 
                                monitor);
                    } catch (CoreException e) {
                        throw new InvocationTargetException(e);
                    }
                }
            
            });
        } catch (InterruptedException e) {
            // Canceled
            return null;
        }
        return resources[0];
    }
    
    public static IResource[] getProjects(IResource[] resources) {
        Set projects = new HashSet();
        for (int i = 0; i < resources.length; i++) {
            IResource resource = resources[i];
            projects.add(resource.getProject());
        }
        return (IResource[]) projects.toArray(new IResource[projects.size()]);
    }
    
    public static boolean isLogicalModel(ResourceMapping[] mappings) {
        for (int i = 0; i < mappings.length; i++) {
            ResourceMapping mapping = mappings[i];
            if (! (mapping.getModelObject() instanceof IResource) ) {
                return true;
            }
        }
        return false;
    }
}
