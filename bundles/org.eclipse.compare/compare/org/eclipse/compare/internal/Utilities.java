/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.compare.internal;

import java.io.*;
import java.text.MessageFormat;
import java.util.*;
import java.util.List;

import org.eclipse.swt.widgets.*;

import org.eclipse.core.commands.util.ListenerList;
import org.eclipse.core.resources.IEncodedStorage;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourceAttributes;
import org.eclipse.core.resources.ResourcesPlugin;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.util.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.jface.dialogs.ErrorDialog;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;

import org.eclipse.ui.*;

import org.eclipse.compare.CompareConfiguration;
import org.eclipse.compare.CompareUI;
import org.eclipse.compare.IEncodedStreamContentAccessor;
import org.eclipse.compare.IStreamContentAccessor;

/**
 * Convenience and utility methods.
 */
public class Utilities {
	
	private static final IPath ICONS_PATH= new Path("$nl$/icons/full/"); //$NON-NLS-1$
	
	public static void registerAction(IKeyBindingService kbs, IAction a, String id) {
		if (kbs != null) {
			a.setActionDefinitionId(id);
			kbs.registerAction(a);
		}
	}
	
	public static IWorkbenchPartSite findSite(Control c) {
		while (c != null && !c.isDisposed()) {
			Object data= c.getData();
			if (data instanceof IWorkbenchPart)
				return ((IWorkbenchPart)data).getSite();
			c= c.getParent();
		}
		return null;
	}

	public static IActionBars findActionBars(Control c) {
		while (c != null && !c.isDisposed()) {
			Object data= c.getData();
			if (data instanceof CompareEditor)
				return ((CompareEditor)data).getActionBars();
				
			// PR 1GDVZV7: ITPVCM:WIN98 - CTRL + C does not work in Java source compare
			if (data instanceof IViewPart)
				return ((IViewPart)data).getViewSite().getActionBars();
			// end PR 1GDVZV7
			
			c= c.getParent();
		}
		return null;
	}

	public static void setEnableComposite(Composite composite, boolean enable) {
		Control[] children= composite.getChildren();
		for (int i= 0; i < children.length; i++)
			children[i].setEnabled(enable);
	}

	public static boolean getBoolean(CompareConfiguration cc, String key, boolean dflt) {
		if (cc != null) {
			Object value= cc.getProperty(key);
			if (value instanceof Boolean)
				return ((Boolean) value).booleanValue();
		}
		return dflt;
	}
	
	public static void firePropertyChange(ListenerList ll, Object source, String property, Object old, Object newValue) {
		if (ll != null) {
			PropertyChangeEvent event= null;
			Object[] listeners= ll.getListeners();
			for (int i= 0; i < listeners.length; i++) {
				IPropertyChangeListener l= (IPropertyChangeListener) listeners[i];
				if (event == null)
					event= new PropertyChangeEvent(source, property, old, newValue);
				l.propertyChange(event);
			}
		}
	}

	public static boolean okToUse(Widget widget) {
		return widget != null && !widget.isDisposed();
	}
	
	private static ArrayList internalGetResources(ISelection selection, Class type) {
		
		ArrayList tmp= new ArrayList();

		if (selection instanceof IStructuredSelection) {
		
			Object[] s= ((IStructuredSelection)selection).toArray();
				
			for (int i= 0; i < s.length; i++) {
				
				IResource resource= null;
				
				Object o= s[i];
				if (type.isInstance(o)) {
					resource= (IResource) o;
						
				} else if (o instanceof IAdaptable) {
					IAdaptable a= (IAdaptable) o;
					Object adapter= a.getAdapter(IResource.class);
					if (type.isInstance(adapter))
						resource= (IResource) adapter;
				}
				
				if (resource != null && resource.isAccessible())
					tmp.add(resource);
			}
		}
		
		return tmp;
	}
	
	/*
	 * Convenience method: extract all accessible <code>IResources</code> from given selection.
	 * Never returns null.
	 */
	public static IResource[] getResources(ISelection selection) {
		ArrayList tmp= internalGetResources(selection, IResource.class);
		return (IResource[]) tmp.toArray(new IResource[tmp.size()]);
	}
	
	/*
	 * Convenience method: extract all accessible <code>IFiles</code> from given selection.
	 * Never returns null.
	 */
	public static IFile[] getFiles(ISelection selection) {
		ArrayList tmp= internalGetResources(selection, IFile.class);
		return (IFile[]) tmp.toArray(new IFile[tmp.size()]);
	}

	public static byte[] readBytes(InputStream in) {
		ByteArrayOutputStream bos= new ByteArrayOutputStream();
		try {		
			while (true) {
				int c= in.read();
				if (c == -1)
					break;
				bos.write(c);
			}
					
		} catch (IOException ex) {
			return null;
		
		} finally {
			Utilities.close(in);
			try {
				bos.close();
			} catch (IOException x) {
				// silently ignored
			}
		}
		
		return bos.toByteArray();
	}

	public static IPath getIconPath(Display display) {
		return ICONS_PATH;
	}
	
	/*
	 * Initialize the given Action from a ResourceBundle.
	 */
	public static void initAction(IAction a, ResourceBundle bundle, String prefix) {
		
		String labelKey= "label"; //$NON-NLS-1$
		String tooltipKey= "tooltip"; //$NON-NLS-1$
		String imageKey= "image"; //$NON-NLS-1$
		String descriptionKey= "description"; //$NON-NLS-1$
		
		if (prefix != null && prefix.length() > 0) {
			labelKey= prefix + labelKey;
			tooltipKey= prefix + tooltipKey;
			imageKey= prefix + imageKey;
			descriptionKey= prefix + descriptionKey;
		}
		
		a.setText(getString(bundle, labelKey, labelKey));
		a.setToolTipText(getString(bundle, tooltipKey, null));
		a.setDescription(getString(bundle, descriptionKey, null));
		
		String relPath= getString(bundle, imageKey, null);
		if (relPath != null && relPath.trim().length() > 0) {
			
			String dPath;
			String ePath;
			
			if (relPath.indexOf("/") >= 0) { //$NON-NLS-1$
				String path= relPath.substring(1);
				dPath= 'd' + path;
				ePath= 'e' + path;
			} else {
				dPath= "dlcl16/" + relPath; //$NON-NLS-1$
				ePath= "elcl16/" + relPath; //$NON-NLS-1$
			}
			
			ImageDescriptor id= CompareUIPlugin.getImageDescriptor(dPath);	// we set the disabled image first (see PR 1GDDE87)
			if (id != null)
				a.setDisabledImageDescriptor(id);
			id= CompareUIPlugin.getImageDescriptor(ePath);
			if (id != null) {
				a.setImageDescriptor(id);
				a.setHoverImageDescriptor(id);
			}
		}
	}
	
	public static void initToggleAction(IAction a, ResourceBundle bundle, String prefix, boolean checked) {

		String tooltip= null;
		if (checked)
			tooltip= getString(bundle, prefix + "tooltip.checked", null);	//$NON-NLS-1$
		else
			tooltip= getString(bundle, prefix + "tooltip.unchecked", null);	//$NON-NLS-1$
		if (tooltip == null)
			tooltip= getString(bundle, prefix + "tooltip", null);	//$NON-NLS-1$
		
		if (tooltip != null)
			a.setToolTipText(tooltip);
			
		String description= null;
		if (checked)
			description= getString(bundle, prefix + "description.checked", null);	//$NON-NLS-1$
		else
			description= getString(bundle, prefix + "description.unchecked", null);	//$NON-NLS-1$
		if (description == null)
			description= getString(bundle, prefix + "description", null);	//$NON-NLS-1$
		
		if (description != null)
			a.setDescription(description);
			
	}

	public static String getString(ResourceBundle bundle, String key, String dfltValue) {
		
		if (bundle != null) {
			try {
				return bundle.getString(key);
			} catch (MissingResourceException x) {
				// NeedWork
			}
		}
		return dfltValue;
	}
	
	public static String getFormattedString(ResourceBundle bundle, String key, String arg) {
		
		if (bundle != null) {
			try {
				return MessageFormat.format(bundle.getString(key), new String[] { arg });
			} catch (MissingResourceException x) {
				// NeedWork
			}
		}
		return "!" + key + "!";//$NON-NLS-2$ //$NON-NLS-1$
	}
	
	public static String getString(String key) {
		try {
			return CompareUI.getResourceBundle().getString(key);
		} catch (MissingResourceException e) {
			return "!" + key + "!";//$NON-NLS-2$ //$NON-NLS-1$
		}
	}
	
	public static String getFormattedString(String key, String arg) {
		try {
			return MessageFormat.format(CompareUI.getResourceBundle().getString(key), new String[] { arg });
		} catch (MissingResourceException e) {
			return "!" + key + "!";//$NON-NLS-2$ //$NON-NLS-1$
		}	
	}

	public static String getFormattedString(String key, String arg0, String arg1) {
		try {
			return MessageFormat.format(CompareUI.getResourceBundle().getString(key), new String[] { arg0, arg1 });
		} catch (MissingResourceException e) {
			return "!" + key + "!";//$NON-NLS-2$ //$NON-NLS-1$
		}	
	}

	public static String getString(ResourceBundle bundle, String key) {
		return getString(bundle, key, key);
	}
	
	public static int getInteger(ResourceBundle bundle, String key, int dfltValue) {
		
		if (bundle != null) {
			try {
				String s= bundle.getString(key);
				if (s != null)
					return Integer.parseInt(s);
			} catch (NumberFormatException x) {
				// NeedWork
			} catch (MissingResourceException x) {
				// NeedWork
			}
		}
		return dfltValue;
	}

	/**
	 * Answers <code>true</code> if the given selection contains resources that don't
	 * have overlapping paths and <code>false</code> otherwise. 
	 */
	/*
	public static boolean isSelectionNonOverlapping() throws TeamException {
		IResource[] resources = getSelectedResources();
		// allow operation for non-overlapping resource selections
		if(resources.length>0) {
			List validPaths = new ArrayList(2);
			for (int i = 0; i < resources.length; i++) {
				IResource resource = resources[i];
				
				// only allow cvs resources to be selected
				if(RepositoryProvider.getProvider(resource.getProject(), CVSProviderPlugin.getTypeId()) == null) {
					return false;
				}
				
				// check if this resource overlaps other selections		
				IPath resourceFullPath = resource.getFullPath();
				if(!validPaths.isEmpty()) {
					for (Iterator it = validPaths.iterator(); it.hasNext();) {
						IPath path = (IPath) it.next();
						if(path.isPrefixOf(resourceFullPath) || 
					       resourceFullPath.isPrefixOf(path)) {
							return false;
						}
					}
				}
				validPaths.add(resourceFullPath);
				
				// ensure that resources are managed
				ICVSResource cvsResource = CVSWorkspaceRoot.getCVSResourceFor(resource);
				if(cvsResource.isFolder()) {
					if( ! ((ICVSFolder)cvsResource).isCVSFolder()) return false;
				} else {
					if( ! cvsResource.isManaged()) return false;
				}
			}
			return true;
		}
		return false;
	}
	*/
	
	/* validate edit utilities */
	
	/**
	 * Status constant indicating that an validateEdit call has changed the
	 * content of a file on disk.
	 */
	private static final int VALIDATE_EDIT_PROBLEM= 10004;	
	
	/*
	 * Makes the given resources committable. Committable means that all
	 * resources are writeable and that the content of the resources hasn't
	 * changed by calling <code>validateEdit</code> for a given file on
	 * <tt>IWorkspace</tt>.
	 * 
	 * @param resources the resources to be checked
	 * @param shell the Shell passed to <code>validateEdit</code> as a context
	 * @return returns <code>true</code> if all resources are committable, <code>false</code> otherwise
	 * 
	 * @see org.eclipse.core.resources.IWorkspace#validateEdit(org.eclipse.core.resources.IFile[], java.lang.Object)
	 */
	public static boolean validateResource(IResource resource, Shell shell, String title) {
		return validateResources(new IResource[] { resource }, shell, title);
	}
	
	/*
	 * Makes the given resources committable. Committable means that all
	 * resources are writeable and that the content of the resources hasn't
	 * changed by calling <code>validateEdit</code> for a given file on
	 * <tt>IWorkspace</tt>.
	 * 
	 * @param resources the resources to be checked
	 * @param shell the Shell passed to <code>validateEdit</code> as a context
	 * @return returns <code>true</code> if all resources are committable, <code>false</code> otherwise
	 * 
	 * @see org.eclipse.core.resources.IWorkspace#validateEdit(org.eclipse.core.resources.IFile[], java.lang.Object)
	 */
	public static boolean validateResources(List resources, Shell shell, String title) {
		IResource r[]= (IResource[]) resources.toArray(new IResource[resources.size()]);
		return validateResources(r, shell, title);
	}
	
	/*
	 * Makes the given resources committable. Committable means that all
	 * resources are writeable and that the content of the resources hasn't
	 * changed by calling <code>validateEdit</code> for a given file on
	 * <tt>IWorkspace</tt>.
	 * 
	 * @param resources the resources to be checked
	 * @param shell the Shell passed to <code>validateEdit</code> as a context
	 * @return returns <code>true</code> if all resources are committable, <code>false</code> otherwise
	 * 
	 * @see org.eclipse.core.resources.IWorkspace#validateEdit(org.eclipse.core.resources.IFile[], java.lang.Object)
	 */
	public static boolean validateResources(IResource[] resources, Shell shell, String title) {
		
		// get all readonly files
		List readOnlyFiles= getReadonlyFiles(resources);
		if (readOnlyFiles.size() == 0)
			return true;
		
		// get timestamps of readonly files before validateEdit
		Map oldTimeStamps= createModificationStampMap(readOnlyFiles);
		
		IFile[] files= (IFile[]) readOnlyFiles.toArray(new IFile[readOnlyFiles.size()]);
		IStatus status= ResourcesPlugin.getWorkspace().validateEdit(files, shell);
		if (! status.isOK()) {
			String message= getString("ValidateEdit.error.unable_to_perform"); //$NON-NLS-1$
			ErrorDialog.openError(shell, title, message, status);
			return false;
		}
			
		IStatus modified= null;
		Map newTimeStamps= createModificationStampMap(readOnlyFiles);
		for (Iterator iter= oldTimeStamps.keySet().iterator(); iter.hasNext();) {
			IFile file= (IFile) iter.next();
			if (file.isReadOnly()) {
				IStatus entry= new Status(IStatus.ERROR,
								CompareUIPlugin.getPluginId(),
								VALIDATE_EDIT_PROBLEM,
								getFormattedString("ValidateEdit.error.stillReadonly", file.getFullPath().toString()), //$NON-NLS-1$
								null);
				modified= addStatus(modified, entry);
			} else if (! oldTimeStamps.get(file).equals(newTimeStamps.get(file))) {
				IStatus entry= new Status(IStatus.ERROR,
								CompareUIPlugin.getPluginId(),
								VALIDATE_EDIT_PROBLEM,
								getFormattedString("ValidateEdit.error.fileModified", file.getFullPath().toString()), //$NON-NLS-1$
								null);
				modified= addStatus(modified, entry);
			}
		}
		if (modified != null) {
			String message= getString("ValidateEdit.error.unable_to_perform"); //$NON-NLS-1$
			ErrorDialog.openError(shell, title, message, modified);
			return false;
		}
		return true;
	}
	
	private static List getReadonlyFiles(IResource[] resources) {
		List readOnlyFiles= new ArrayList();
		for (int i= 0; i < resources.length; i++) {
			IResource resource= resources[i];
			ResourceAttributes resourceAttributes= resource.getResourceAttributes();
			if (resource.getType() == IResource.FILE && resourceAttributes != null && resourceAttributes.isReadOnly())	
				readOnlyFiles.add(resource);
		}
		return readOnlyFiles;
	}

	private static Map createModificationStampMap(List files) {
		Map map= new HashMap();
		for (Iterator iter= files.iterator(); iter.hasNext(); ) {
			IFile file= (IFile)iter.next();
			map.put(file, new Long(file.getModificationStamp()));
		}
		return map;
	}
	
	private static IStatus addStatus(IStatus status, IStatus entry) {
		
		if (status == null)
			return entry;
			
		if (status.isMultiStatus()) {
			((MultiStatus)status).add(entry);
			return status;
		}

		MultiStatus result= new MultiStatus(CompareUIPlugin.getPluginId(),
			VALIDATE_EDIT_PROBLEM,
			getString("ValidateEdit.error.unable_to_perform"), null); //$NON-NLS-1$ 
		result.add(status);
		result.add(entry);
		return result;
	}
	
	// encoding
	
	/*
	 * Returns null if an error occurred.
	 */
	public static String readString(InputStream is, String encoding) {
		if (is == null)
			return null;
		BufferedReader reader= null;
		try {
			StringBuffer buffer= new StringBuffer();
			char[] part= new char[2048];
			int read= 0;
			reader= new BufferedReader(new InputStreamReader(is, encoding));

			while ((read= reader.read(part)) != -1)
				buffer.append(part, 0, read);
			
			return buffer.toString();
			
		} catch (IOException ex) {
			// NeedWork
		} finally {
			if (reader != null) {
				try {
					reader.close();
				} catch (IOException ex) {
					// silently ignored
				}
			}
		}
		return null;
	}
	
	public static String getCharset(IResource resource) {
		if (resource instanceof IEncodedStorage) {
			try {
				return ((IEncodedStorage)resource).getCharset();
			} catch (CoreException ex) {
				// fall  through
			}
		}
		return ResourcesPlugin.getEncoding();
	}
	
	public static byte[] getBytes(String s, String encoding) {
		byte[] bytes= null;
		if (s != null) {
			try {
				bytes= s.getBytes(encoding);
			} catch (UnsupportedEncodingException e) {
				bytes= s.getBytes();
			}
		}
		return bytes;
	}

	public static String readString(IStreamContentAccessor sa) throws CoreException {
		InputStream is= sa.getContents();
		String encoding= null;
		if (sa instanceof IEncodedStreamContentAccessor)
			encoding= ((IEncodedStreamContentAccessor)sa).getCharset();
		if (encoding == null)
			encoding= ResourcesPlugin.getEncoding();
		return Utilities.readString(is, encoding);
	}

	public static void close(InputStream is) {
		if (is != null) {
			try {
				is.close();
			} catch (IOException ex) {
				// silently ignored
			}
		}
	}

	public static IResource getFirstResource(ISelection selection) {
		IResource[] resources = getResources(selection);
		if (resources.length > 0)
			return resources[0];
		return null;
	}
}
