/*******************************************************************************
 * Copyright (c) 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.ant.internal.ui.editor;

import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.hyperlink.IHyperlink;

public class AntElementHyperlink implements IHyperlink {

    private IRegion fRegion= null;
    private Object fLinkTarget= null;
    private AntEditor fEditor= null;
    
    public AntElementHyperlink(AntEditor editor, IRegion region, Object linkTarget) {
        
       fRegion= region;
       fLinkTarget= linkTarget;
       fEditor= editor;
    }

    /* (non-Javadoc)
     * @see org.eclipse.jface.text.hyperlink.IHyperlink#getRegion()
     */
    public IRegion getRegion() {
        return fRegion;
    }

    /* (non-Javadoc)
     * @see org.eclipse.jface.text.hyperlink.IHyperlink#getTypeLabel()
     */
    public String getTypeLabel() {
        return null;
    }

    /* (non-Javadoc)
     * @see org.eclipse.jface.text.hyperlink.IHyperlink#getText()
     */
    public String getText() {
        return null;
    }

    /* (non-Javadoc)
     * @see org.eclipse.jface.text.hyperlink.IHyperlink#open()
     */
    public void open() {
        fEditor.openTarget(fLinkTarget);
    }
}