/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.ant.internal.ui.antsupport.logger.util;

import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Stack;
import java.util.Vector;

import org.apache.tools.ant.BuildEvent;
import org.apache.tools.ant.Location;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Target;
import org.apache.tools.ant.Task;

public class AntDebugState implements IDebugBuildLogger {
	
	private IDebugBuildLogger fLogger;
	private Stack fTasks= new Stack();
	private Task fCurrentTask;
	private Task fStepOverTask;
	private Task fStepIntoTask;
	private Task fLastTaskFinished;
    
	//properties set before execution
    private Map fInitialProperties= null;
	private Map fProperties= null;
    
    private Map fTargetToBuildSequence= null;
    private Target fTargetToExecute= null;
    private Target fTargetExecuting= null;
	
	private boolean fConsiderTargetBreakpoints= false;
	private boolean fShouldSuspend;
	private boolean fClientSuspend= false;
	private boolean fStepIntoSuspend= false;
	private boolean fIsAfterTaskEvent= false;
	
	public AntDebugState(IDebugBuildLogger logger) {
		fLogger= logger;
	}

	public void waitIfSuspended() {
		fLogger.waitIfSuspended();
	}

	public Task getLastTaskFinished() {
		return fLastTaskFinished;
	}

	public void setLastTaskFinished(Task lastTaskFinished) {
		fLastTaskFinished= lastTaskFinished;

	}

	public Task getCurrentTask() {
		return fCurrentTask;
	}

	public void setCurrentTask(Task currentTask) {
		fCurrentTask= currentTask;

	}

	public Map getInitialProperties() {
		return fInitialProperties;
	}

	public void setInitialProperties(Map initialProperties) {
		fInitialProperties= initialProperties;
	}

	public Task getStepOverTask() {
		return fStepOverTask;
	}

	public void setStepOverTask(Task stepOverTask) {
		fStepOverTask= stepOverTask;

	}

	public boolean considerTargetBreakpoints() {
		return fConsiderTargetBreakpoints;
	}

	public void setConsiderTargetBreakpoints(boolean considerTargetBreakpoints) {
		fConsiderTargetBreakpoints= considerTargetBreakpoints;
	}

	public void setTasks(Stack tasks) {
		fTasks= tasks;
	}

	public Stack getTasks() {
		return fTasks;
	}

	public void setShouldSuspend(boolean shouldSuspend) {
		fShouldSuspend= shouldSuspend;
	}

	public boolean shouldSuspend() {
		return fShouldSuspend;
	}

	public Map getTargetToBuildSequence() {
		return fTargetToBuildSequence;
	}

	public void setTargetToBuildSequence(Map sequence) {
		fTargetToBuildSequence= sequence;
	}

	public void setTargetToExecute(Target target) {
		fTargetToExecute= target;
	}

	public void setTargetExecuting(Target target) {
		fTargetExecuting= target;
	}

	public Target getTargetToExecute() {
		return fTargetToExecute;
	}
	
	public Target getTargetExecuting() {
		return fTargetExecuting;
	}

	public boolean isStepIntoSuspend() {
		return isAfterTaskEvent() && fStepIntoSuspend;
	}

	public void setStepIntoSuspend(boolean stepIntoSuspend) {
		fStepIntoSuspend = stepIntoSuspend;
	}

	public boolean isClientSuspend() {
		return fClientSuspend;
	}

	public void setClientSuspend(boolean clientSuspend) {
		fClientSuspend = clientSuspend;
	}

	public Task getStepIntoTask() {
		return fStepIntoTask;
	}

	public void setStepIntoTask(Task stepIntoTask) {
		fStepIntoTask = stepIntoTask;
	}
	
	public void resume() {
		fLogger.notifyAll();
	}

	public Map getProperties() {
		return fProperties;
	}
	
	public Location getBreakpointLocation() {
		if (isAfterTaskEvent() && getCurrentTask() != null) {
			return getCurrentTask().getLocation();
		}
		if (considerTargetBreakpoints() && getTargetExecuting() != null) {
			return getLocation(getTargetExecuting());
		}
		return null;
	}

	public boolean isAfterTaskEvent() {
		return fIsAfterTaskEvent;
	}

	public void setAfterTaskEvent(boolean isAfterTaskEvent) {
		fIsAfterTaskEvent = isAfterTaskEvent;
	}
	
	public void taskStarted(BuildEvent event) {
		setAfterTaskEvent(true);
		if (getInitialProperties() == null) {//implicit or top level target does not fire targetStarted()
			setInitialProperties(event.getProject().getProperties());
		}
		
		setCurrentTask(event.getTask());
		setConsiderTargetBreakpoints(false);
		getTasks().push(getCurrentTask());
		waitIfSuspended();
	}
	

    public void taskFinished() {
        setLastTaskFinished((Task)getTasks().pop());
        setCurrentTask(null);
        String taskName= getLastTaskFinished().getTaskName();
        if (getStepOverTask() != null && ("antcall".equals(taskName) || "ant".equals(taskName) || "macrodef".equals(taskName))) { //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            setShouldSuspend(true);
        }
        waitIfSuspended();
    }

    public void stepOver() {
       setStepOverTask(getCurrentTask());
        if (getCurrentTask() == null) {
            //stepping over target breakpoint
           setShouldSuspend(true);
        }
        resume();
    }

    public void targetStarted(BuildEvent event) {
		setAfterTaskEvent(false);
        if (getInitialProperties() == null) {
            setInitialProperties(event.getProject().getProperties());
        }
        if (getTargetToBuildSequence() == null) {
            setTargetToBuildSequence(new HashMap());
            setTargetToExecute(initializeBuildSequenceInformation(event, getTargetToBuildSequence()));
        }
        
        setTargetExecuting(event.getTarget());
        if (event.getTarget().equals(getTargetToExecute())) {
            //the dependancies of the target to execute have been met
            //prepare for the next target
            Vector targets= (Vector) event.getProject().getReference("eclipse.ant.targetVector"); //$NON-NLS-1$
            if (!targets.isEmpty()) {
                setTargetToExecute((Target) event.getProject().getTargets().get(targets.remove(0)));
            } else {
                setTargetToExecute(null);
            }
        }
        setConsiderTargetBreakpoints(true);
    }

	public int getLineNumber(Location location) {
	    try { //succeeds with Ant newer than 1.6
	        return location.getLineNumber();
	    } catch (NoSuchMethodError e) {
	        //Ant before 1.6
	        String locationString= location.toString();
	        if (locationString.length() == 0) {
	            return 0;
	        }
	        //filename: lineNumber: ("c:\buildfile.xml: 12: ")
	        int lastIndex= locationString.lastIndexOf(':');
	        int index =locationString.lastIndexOf(':', lastIndex - 1);
	        if (index != -1) {
	            try {
	                return Integer.parseInt(locationString.substring(index+1, lastIndex));
	            } catch (NumberFormatException nfe) {
	                return 0;
	            }
	        }
	        return 0;
	    }
	}

	public static Location getLocation(Target target) {
	    try {//succeeds with Ant newer than 1.6.2
	        return target.getLocation();
	    } catch (NoSuchMethodError e) {
	        return Location.UNKNOWN_LOCATION;
	    }
	}

	public String getFileName(Location location) {
	    try {//succeeds with Ant newer than 1.6
	        return location.getFileName();
	    } catch (NoSuchMethodError e) {
	        //Ant before 1.6
	        String locationString= location.toString();
	        if (locationString.length() == 0) {
	            return null;
	        }
	        //filename: lineNumber: ("c:\buildfile.xml: 12: ")          
	        int lastIndex= locationString.lastIndexOf(':');
	        int index =locationString.lastIndexOf(':', lastIndex-1);
	        if (index == -1) {
	            index= lastIndex; //only the filename is known
	        }
	        if (index != -1) {
	        //bug 84403
	            //if (locationString.startsWith("file:")) { //$NON-NLS-1$
	              //  return FileUtils.newFileUtils().fromURI(locationString);
	            //}
	            //remove file:
	            return locationString.substring(5, index);
	        }
	        return null;
	    }
	}

	private void appendToStack(StringBuffer stackRepresentation, String targetName, String taskName, Location location) {
	    stackRepresentation.append(targetName);
	    stackRepresentation.append(DebugMessageIds.MESSAGE_DELIMITER);
	    stackRepresentation.append(taskName);
	    stackRepresentation.append(DebugMessageIds.MESSAGE_DELIMITER);
	    
	    stackRepresentation.append(getFileName(location));
	    stackRepresentation.append(DebugMessageIds.MESSAGE_DELIMITER);
	    stackRepresentation.append(getLineNumber(location));
	    stackRepresentation.append(DebugMessageIds.MESSAGE_DELIMITER);
	}

	public void marshalStack(StringBuffer stackRepresentation) {
		Stack tasks= getTasks();
		
	    stackRepresentation.append(DebugMessageIds.STACK);
	    stackRepresentation.append(DebugMessageIds.MESSAGE_DELIMITER);
	    
		Target targetToExecute= getTargetToExecute();
		Target targetExecuting= getTargetExecuting();
		if (tasks.isEmpty()) {
			appendToStack(stackRepresentation, targetExecuting.getName(), "", getLocation(targetExecuting)); //$NON-NLS-1$
		} else {
			for (int i = tasks.size() - 1; i >= 0 ; i--) {
				Task task= (Task) tasks.get(i);
				appendToStack(stackRepresentation, task.getOwningTarget().getName(), task.getTaskName(), task.getLocation());
			}
		}
	    //target dependancy stack 
	     if (targetToExecute != null) {
	     	Vector buildSequence= (Vector) getTargetToBuildSequence().get(targetToExecute);
	     	int startIndex= buildSequence.indexOf(targetExecuting) + 1;
	     	int dependancyStackDepth= buildSequence.indexOf(targetToExecute);
	     	
	     	Target stackTarget;
	     	for (int i = startIndex; i <= dependancyStackDepth; i++) {
	     		stackTarget= (Target) buildSequence.get(i);
	            if (stackTarget.dependsOn(targetExecuting.getName())) {
	     		    appendToStack(stackRepresentation, stackTarget.getName(), "", getLocation(stackTarget)); //$NON-NLS-1$
	            }
	     	}
	     }
	}

	public void marshallProperties(StringBuffer propertiesRepresentation, boolean marshallLineSep) {
		if (getTasks().isEmpty()) {
			return;
		}
	    propertiesRepresentation.append(DebugMessageIds.PROPERTIES);
	    propertiesRepresentation.append(DebugMessageIds.MESSAGE_DELIMITER);
		
		Project project= ((Task)getTasks().peek()).getProject();
		Map lastProperties= getProperties(); 
		
	    Map currentProperties= project.getProperties();
	    if (lastProperties != null && currentProperties.size() == lastProperties.size()) {
	        //no new properties
	        return;
	    }
	    
		Map initialProperties= getInitialProperties();
	    Map currentUserProperties= project.getUserProperties();
	    Iterator iter= currentProperties.keySet().iterator();
	    String propertyName;
	    String propertyValue;
	    while (iter.hasNext()) {
	        propertyName = (String) iter.next();
	        if (!marshallLineSep && propertyName.equals("line.separator")) { //$NON-NLS-1$
	        	continue;
	        }
	        if (lastProperties == null || lastProperties.get(propertyName) == null) { //new property
	            propertiesRepresentation.append(propertyName.length());
	            propertiesRepresentation.append(DebugMessageIds.MESSAGE_DELIMITER);
	            propertiesRepresentation.append(propertyName);
	            propertiesRepresentation.append(DebugMessageIds.MESSAGE_DELIMITER);
	            propertyValue= (String) currentProperties.get(propertyName);
	            propertiesRepresentation.append(propertyValue.length());
	            propertiesRepresentation.append(DebugMessageIds.MESSAGE_DELIMITER);
	            propertiesRepresentation.append(propertyValue);
	            propertiesRepresentation.append(DebugMessageIds.MESSAGE_DELIMITER);
	            if (initialProperties.get(propertyName) != null) { //properties set before the start of the build
	                if (currentUserProperties.get(propertyName) == null) {
	                    propertiesRepresentation.append(DebugMessageIds.PROPERTY_SYSTEM);
	                } else {
	                    propertiesRepresentation.append(DebugMessageIds.PROPERTY_USER);
	                }
	            } else if (currentUserProperties.get(propertyName) == null){
	                propertiesRepresentation.append(DebugMessageIds.PROPERTY_RUNTIME);
	            } else {
	                propertiesRepresentation.append(DebugMessageIds.PROPERTY_USER);
	            }
	            propertiesRepresentation.append(DebugMessageIds.MESSAGE_DELIMITER);
	        }
	    }
	    
	    propertiesRepresentation.deleteCharAt(propertiesRepresentation.length() - 1);
		fProperties= currentProperties;
	}

	public Target initializeBuildSequenceInformation(BuildEvent event, Map targetToBuildSequence) {
	    Project antProject= event.getProject();
	    Vector targets= (Vector) antProject.getReference("eclipse.ant.targetVector"); //$NON-NLS-1$
	    Iterator itr= targets.iterator();
	    Hashtable allTargets= antProject.getTargets();
	    String targetName;
	    Vector sortedTargets;
	    while (itr.hasNext()) {
	        targetName= (String) itr.next();
	        sortedTargets= antProject.topoSort(targetName, allTargets);
	        targetToBuildSequence.put(allTargets.get(targetName), sortedTargets);
	    }
	    //the target to execute
	    return (Target) allTargets.get(targets.remove(0));
	}
}