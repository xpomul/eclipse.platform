package org.eclipse.update.internal.ui.forms;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import java.net.URL;
import java.util.*;

import org.eclipse.core.runtime.*;
import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.jface.dialogs.*;
import org.eclipse.jface.resource.*;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.*;
import org.eclipse.ui.help.WorkbenchHelp;
import org.eclipse.update.configuration.*;
import org.eclipse.update.core.*;
import org.eclipse.update.internal.ui.*;
import org.eclipse.update.internal.ui.model.*;
import org.eclipse.update.internal.ui.pages.UpdateFormPage;
import org.eclipse.update.internal.ui.parts.SWTUtil;
import org.eclipse.update.internal.ui.preferences.UpdateColors;
import org.eclipse.update.internal.ui.search.*;
import org.eclipse.update.internal.ui.views.DetailsView;
import org.eclipse.update.internal.ui.wizards.*;
import org.eclipse.update.ui.forms.internal.*;

public class DetailsForm extends PropertyWebForm {
	// NL keys

	private static final String KEY_PROVIDER = "FeaturePage.provider";
	private static final String KEY_VERSION = "FeaturePage.version";
	private static final String KEY_IVERSION = "FeaturePage.installedVersion";
	private static final String KEY_PENDING_VERSION =
		"FeaturePage.pendingVersion";
	private static final String KEY_SIZE = "FeaturePage.size";
	private static final String KEY_ESTIMATE = "FeaturePage.estimate";
	private static final String KEY_OS = "FeaturePage.os";
	private static final String KEY_WS = "FeaturePage.ws";
	private static final String KEY_NL = "FeaturePage.nl";
	private static final String KEY_ARCH = "FeaturePage.arch";
	private static final String KEY_PLATFORMS = "FeaturePage.platforms";
	private static final String KEY_DESC = "FeaturePage.description";
	private static final String KEY_INFO_LINK = "FeaturePage.infoLink";
	private static final String KEY_LICENSE_LINK = "FeaturePage.licenseLink";
	private static final String KEY_COPYRIGHT_LINK =
		"FeaturePage.copyrightLink";
	private static final String KEY_NOT_INSTALLED = "FeaturePage.notInstalled";
	private static final String KEY_SIZE_VALUE = "FeaturePage.sizeValue";
	private static final String KEY_ESTIMATE_VALUE =
		"FeaturePage.estimateValue";
	private static final String KEY_MINUTE_ESTIMATE_VALUE =
		"FeaturePage.minuteEstimateValue";
	private static final String KEY_UNKNOWN_SIZE_VALUE =
		"FeaturePage.unknownSizeValue";
	private static final String KEY_UNKNOWN_ESTIMATE_VALUE =
		"FeaturePage.unknownEstimateValue";
	private static final String KEY_DO_UNCONFIGURE =
		"FeaturePage.doButton.unconfigure";
	private static final String KEY_DO_CONFIGURE =
		"FeaturePage.doButton.configure";
	private static final String KEY_DO_REPAIR = "FeaturePage.doButton.repair";
	private static final String KEY_DO_CHANGE = "FeaturePage.doButton.change";
	private static final String KEY_DO_UPDATE = "FeaturePage.doButton.update";
	private static final String KEY_DO_INSTALL = "FeaturePage.doButton.install";
	private static final String KEY_DO_UNINSTALL =
		"FeaturePage.doButton.uninstall";

	private static final String KEY_BATCH_UNCONFIGURE =
		"FeaturePage.batchButton.unconfigure";
	private static final String KEY_BATCH_CONFIGURE =
		"FeaturePage.batchButton.configure";
	private static final String KEY_BATCH_REPAIR =
		"FeaturePage.batchButton.repair";
	private static final String KEY_BATCH_CHANGE =
		"FeaturePage.batchButton.change";
	private static final String KEY_BATCH_UPDATE =
		"FeaturePage.batchButton.update";
	private static final String KEY_BATCH_INSTALL =
		"FeaturePage.batchButton.install";
	private static final String KEY_BATCH_UNINSTALL =
		"FeaturePage.batchButton.uninstall";
	private static final String KEY_BATCH = "FeaturePage.batch";
	private static final String KEY_SELECTED_UPDATES =
		"FeaturePage.selectedUpdates";

	private static final String KEY_DIALOG_UTITLE = "FeaturePage.dialog.utitle";
	private static final String KEY_DIALOG_TITLE = "FeaturePage.dialog.title";
	private static final String KEY_DIALOG_CTITLE = "FeaturePage.dialog.ctitle";
	private static final String KEY_DIALOG_UCTITLE =
		"FeaturePage.dialog.uctitle";
	private static final String KEY_DIALOG_UMESSAGE =
		"FeaturePage.dialog.umessage";
	private static final String KEY_DIALOG_MESSAGE =
		"FeaturePage.dialog.message";
	private static final String KEY_DIALOG_CMESSAGE =
		"FeaturePage.dialog.cmessage";
	private static final String KEY_DIALOG_UCMESSAGE =
		"FeaturePage.dialog.ucmessage";
	private static final String KEY_MISSING_TITLE = "FeaturePage.missing.title";
	private static final String KEY_MISSING_MESSAGE =
		"FeaturePage.missing.message";
	private static final String KEY_MISSING_SEARCH =
		"FeaturePage.missing.search";
	private static final String KEY_MISSING_ABORT = "FeaturePage.missing.abort";
	private static final String KEY_SEARCH_OBJECT_NAME =
		"FeaturePage.missing.searchObjectName";
	private static final String KEY_OPTIONAL_INSTALL_MESSAGE =
		"FeaturePage.optionalInstall.message";
	private static final String KEY_OPTIONAL_INSTALL_TITLE =
		"FeaturePage.optionalInstall.title";
	//	
	private static final int REPAIR = 1;
	private static final int CHANGE = 2;
	private int reinstallCode = 0;

	private Label imageLabel;
	private Label providerLabel;
	private Label versionLabel;
	private Label installedVersionLabel;
	private Label sizeLabel;
	private Label estimatedTime;
	private Label osLabel;
	private Label wsLabel;
	private Label nlLabel;
	private Label archLabel;
	private Label descriptionText;
	private Label groupUpdatesLabel;
	private URL infoLinkURL;
	private SelectableFormLabel infoLinkLabel;
	private SelectableFormLabel itemsLink;
	private InfoLink licenseLink;
	private InfoLink copyrightLink;
	private ReflowGroup supportedPlatformsGroup;
	private Image providerImage;
	//private Button uninstallButton;
	private Button addButton;
	private Button doButton;
	private IFeature currentFeature;
	private IFeatureAdapter currentAdapter;
	private ModelListener modelListener;
	private Hashtable imageCache = new Hashtable();
	private HyperlinkHandler sectionHandler;
	private boolean alreadyInstalled;
	private IFeature[] installedFeatures;
	private boolean newerVersion;
	private boolean addBlock;
	private boolean inputBlock;

	class ModelListener implements IUpdateModelChangedListener {
		/**
		 * @see IUpdateModelChangedListener#objectAdded(Object, Object)
		 */
		public void objectsAdded(Object parent, Object[] children) {
			if (isCurrentFeature(children)) {
				SWTUtil.getStandardDisplay().asyncExec(new Runnable() {
					public void run() {
						refresh();
					}
				});
			}
		}

		boolean isCurrentFeature(Object[] children) {
			for (int i = 0; i < children.length; i++) {
				Object obj = children[i];
				if (obj instanceof PendingChange) {
					PendingChange job = (PendingChange) obj;
					if (job.getFeature().equals(currentFeature)) {
						return true;
					}
				}
			}
			return false;
		}

		/**
		 * @see IUpdateModelChangedListener#objectRemoved(Object, Object)
		 */
		public void objectsRemoved(Object parent, Object[] children) {
			if (isCurrentFeature(children)) {
				SWTUtil.getStandardDisplay().asyncExec(new Runnable() {
					public void run() {
						//doButton.setEnabled(true);
						refresh();
					}
				});
			}
		}

		/**
		 * @see IUpdateModelChangedListener#objectChanged(Object, String)
		 */
		public void objectChanged(Object object, String property) {
		}
	}

	abstract class LinkListener implements IHyperlinkListener {
		public abstract URL getURL();
		public void linkActivated(Control linkLabel) {
			URL url = getURL();
			if (url != null)
				openURL(url.toString());
		}
		public void linkEntered(Control linkLabel) {
			URL url = getURL();
			if (url != null)
				showStatus(url.toString());
		}
		public void linkExited(Control linkLabel) {
			showStatus(null);
		}

		private void showStatus(String text) {
			IViewSite site = getPage().getView().getViewSite();
			IStatusLineManager sm = site.getActionBars().getStatusLineManager();
			sm.setMessage(text);
		}
	}

	abstract class ReflowGroup extends ExpandableGroup {
		public void expanded() {
			reflow();
			updateSize();
		}
		public void collapsed() {
			reflow();
			updateSize();
		}
		protected SelectableFormLabel createTextLabel(
			Composite parent,
			FormWidgetFactory factory) {
			SelectableFormLabel label = super.createTextLabel(parent, factory);
			label.setFont(JFaceResources.getBannerFont());
			return label;
		}
		protected HyperlinkHandler getHyperlinkHandler(FormWidgetFactory factory) {
			return sectionHandler;
		}
	}

	public DetailsForm(UpdateFormPage page) {
		super(page);
		providerImage = UpdateUIImages.DESC_PROVIDER.createImage();
		modelListener = new ModelListener();
		UpdateModel model = UpdateUI.getDefault().getUpdateModel();
		model.addUpdateModelChangedListener(modelListener);
		sectionHandler = new HyperlinkHandler();
	}

	public void dispose() {
		UpdateModel model = UpdateUI.getDefault().getUpdateModel();
		model.removeUpdateModelChangedListener(modelListener);
		providerImage.dispose();
		for (Enumeration enum = imageCache.elements();
			enum.hasMoreElements();
			) {
			Image image = (Image) enum.nextElement();
			image.dispose();
		}
		imageCache.clear();
		sectionHandler.dispose();
		super.dispose();
	}

	public void initialize(Object modelObject) {
		setHeadingText("");
		super.initialize(modelObject);
	}

	private void configureSectionHandler(
		FormWidgetFactory factory,
		Display display) {
		sectionHandler.setHyperlinkUnderlineMode(
			HyperlinkHandler.UNDERLINE_NEVER);
		sectionHandler.setBackground(factory.getBackgroundColor());
		sectionHandler.setForeground(UpdateColors.getTopicColor(display));
	}

	protected void updateHeadings() {
		sectionHandler.setForeground(
			UpdateColors.getTopicColor(getControl().getDisplay()));
		super.updateHeadings();
	}

	public void createContents(Composite container) {
		HTMLTableLayout layout = new HTMLTableLayout();
		layout.numColumns = 2;
		container.setLayout(layout);
		//layout.rightMargin = 0;
		GridData gd;

		configureSectionHandler(factory, container.getDisplay());

		GridLayout glayout = new GridLayout();
		Composite properties = factory.createComposite(container);
		properties.setLayout(glayout);
		glayout.marginWidth = glayout.marginHeight = 0;
		glayout.verticalSpacing = 0;

		providerLabel =
			createProperty(properties, UpdateUI.getString(KEY_PROVIDER));
		versionLabel =
			createProperty(properties, UpdateUI.getString(KEY_VERSION));
		installedVersionLabel =
			createProperty(properties, UpdateUI.getString(KEY_IVERSION));
		sizeLabel = createProperty(properties, UpdateUI.getString(KEY_SIZE));
		estimatedTime =
			createProperty(properties, UpdateUI.getString(KEY_ESTIMATE));
		supportedPlatformsGroup = new ReflowGroup() {
			public void fillExpansion(
				Composite expansion,
				FormWidgetFactory factory) {
				GridLayout layout = new GridLayout();
				expansion.setLayout(layout);
				layout.marginWidth = 0;
				osLabel = createProperty(expansion, UpdateUI.getString(KEY_OS));
				wsLabel = createProperty(expansion, UpdateUI.getString(KEY_WS));
				nlLabel = createProperty(expansion, UpdateUI.getString(KEY_NL));
				archLabel =
					createProperty(expansion, UpdateUI.getString(KEY_ARCH));
			}
		};
		supportedPlatformsGroup.setText(UpdateUI.getString(KEY_PLATFORMS));
		new Label(properties, SWT.NULL);
		supportedPlatformsGroup.createControl(properties, factory);
		setFocusControl(supportedPlatformsGroup.getControl());

		imageLabel = factory.createLabel(container, null);
		TableData td = new TableData();
		td.align = TableData.CENTER;
		//td.valign = TableData.MIDDLE;
		imageLabel.setLayoutData(td);

		Label label = createHeading(container, UpdateUI.getString(KEY_DESC));
		td = new TableData();
		td.colspan = 2;
		label.setLayoutData(td);
		descriptionText = factory.createLabel(container, null, SWT.WRAP);
		td = new TableData();
		td.colspan = 2;
		td.grabHorizontal = true;
		descriptionText.setLayoutData(td);

		glayout = new GridLayout();
		glayout.numColumns = 4;
		glayout.horizontalSpacing = 20;
		glayout.marginWidth = 10;

		addSeparator(container);

		Composite footer = factory.createComposite(container);
		td = new TableData();
		td.colspan = 2;
		td.align = TableData.FILL;
		footer.setLayoutData(td);
		footer.setLayout(glayout);

		LinkListener listener = new LinkListener() {
			public URL getURL() {
				return infoLinkURL;
			}
		};

		infoLinkLabel = new SelectableFormLabel(footer, SWT.NULL);
		infoLinkLabel.setText(UpdateUI.getString(KEY_INFO_LINK));
		factory.turnIntoHyperlink(infoLinkLabel, listener);
		gd = new GridData(GridData.VERTICAL_ALIGN_BEGINNING);
		infoLinkLabel.setLayoutData(gd);
		licenseLink = new InfoLink((DetailsView) getPage().getView());
		licenseLink.setText(UpdateUI.getString(KEY_LICENSE_LINK));
		licenseLink.createControl(footer, factory);
		gd = new GridData(GridData.VERTICAL_ALIGN_BEGINNING);
		licenseLink.getControl().setLayoutData(gd);
		copyrightLink = new InfoLink((DetailsView) getPage().getView());
		copyrightLink.setText(UpdateUI.getString(KEY_COPYRIGHT_LINK));
		copyrightLink.createControl(footer, factory);
		gd = new GridData(GridData.VERTICAL_ALIGN_BEGINNING);
		copyrightLink.getControl().setLayoutData(gd);

		doButton = factory.createButton(footer, "", SWT.PUSH);
		doButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				doButtonSelected();
			}
		});
		gd =
			new GridData(
				GridData.HORIZONTAL_ALIGN_CENTER
					| GridData.VERTICAL_ALIGN_BEGINNING);
		gd.grabExcessHorizontalSpace = true;
		doButton.setLayoutData(gd);

		Composite batch = factory.createComposite(container);
		glayout = new GridLayout();
		glayout.numColumns = 2;
		glayout.marginWidth = 0;
		glayout.marginHeight = 0;
		batch.setLayout(glayout);
		td = new TableData();
		td.colspan = 2;
		td.align = TableData.FILL;
		//td.grabHorizontal = true;
		batch.setLayoutData(td);
		groupUpdatesLabel = createHeading(batch, UpdateUI.getString(KEY_BATCH));
		gd = new GridData();
		gd.horizontalSpan = 2;
		groupUpdatesLabel.setLayoutData(gd);
		addButton =
			factory.createButton(
				batch,
				UpdateUI.getString(KEY_BATCH_INSTALL),
				SWT.CHECK);
		addButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				if (!addBlock)
					doAdd(addButton.getSelection());
			}
		});
		addButton.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_CENTER));
		itemsLink =
			factory.createSelectableLabel(
				batch,
				UpdateUI.getString(KEY_SELECTED_UPDATES));
		factory.turnIntoHyperlink(itemsLink, new HyperlinkAdapter() {
			public void linkActivated(Control link) {
				openUpdateItems();
			}
		});
		itemsLink.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_CENTER));

		//SWTUtil.setButtonDimensionHint(doButton);
		WorkbenchHelp.setHelp(container, "org.eclipse.update.ui.DetailsForm");

	}

	private void addSeparator(Composite container) {
		Composite l = factory.createCompositeSeparator(container);
		l.setBackground(factory.getBorderColor());
		TableData td = new TableData();
		td.colspan = 2;
		td.heightHint = 1;
		td.align = TableData.FILL;
		l.setLayoutData(td);
	}

	private void openUpdateItems() {
		BusyIndicator.showWhile(SWTUtil.getStandardDisplay(), new Runnable() {
			public void run() {
				try {
					IWorkbenchPage page = UpdateUI.getActivePage();
					IViewPart view = page.findView(UpdatePerspective.ID_ITEMS);
					if (view != null)
						page.bringToTop(view);
					else {
						inputBlock = true;
						page.showView(UpdatePerspective.ID_ITEMS);
						inputBlock = false;
					}
				} catch (PartInitException e) {
					UpdateUI.logException(e);
				}
			}
		});
	}

	public void expandTo(final Object obj) {
		if (inputBlock)
			return;
		BusyIndicator.showWhile(getControl().getDisplay(), new Runnable() {
			public void run() {
				if (obj instanceof IFeature) {
					currentAdapter = null;
					currentFeature = (IFeature) obj;
					refresh();
				} else if (obj instanceof IFeatureAdapter) {
					IFeatureAdapter adapter = (IFeatureAdapter) obj;
					try {
						currentAdapter = adapter;
						currentFeature = adapter.getFeature(null);
					} catch (CoreException e) {
						//UpdateUI.logException(e);
						currentFeature =
							new MissingFeature(
								adapter.getSite(),
								adapter.getURL());
					} finally {
						refresh();
					}
				} else {
					currentFeature = null;
					currentAdapter = null;
					refresh();
				}
			}
		});
	}

	private boolean isConfigured(IFeature feature) {
		ISite site = feature.getSite();
		IConfiguredSite csite = site.getCurrentConfiguredSite();
		if (csite == null)
			return false;
		return csite.isConfigured(feature);
	}

	private String getInstalledVersionText(IFeature feature) {
		alreadyInstalled = false;
		VersionedIdentifier vid = feature.getVersionedIdentifier();
		PluginVersionIdentifier version = vid.getVersion();
		newerVersion = installedFeatures.length > 0;

		StringBuffer buf = new StringBuffer();
		for (int i = 0; i < installedFeatures.length; i++) {
			IFeature installedFeature = installedFeatures[i];
			boolean enabled = isConfigured(installedFeature);
			VersionedIdentifier ivid =
				installedFeature.getVersionedIdentifier();
			if (buf.length() > 0)
				buf.append(", ");
			PluginVersionIdentifier iversion = ivid.getVersion();
			buf.append(iversion.toString());
			if (!enabled) {
				buf.append(" ");
				buf.append(UpdateUI.getString("FeaturePage.disabledVersion"));
			}
			if (ivid.equals(vid)) {
				alreadyInstalled = true;
			} else {
				if (iversion.isGreaterOrEqualTo(version))
					newerVersion = false;
			}
		}
		if (buf.length() > 0) {
			String versionText = buf.toString();
			UpdateModel model = UpdateUI.getDefault().getUpdateModel();
			PendingChange change = model.findRelatedPendingChange(feature);
			if (change != null) {
				return UpdateUI.getFormattedMessage(
					KEY_PENDING_VERSION,
					versionText);
			} else
				return versionText;
		} else
			return null;
	}

	private void refresh() {
		IFeature feature = currentFeature;

		if (feature == null)
			return;

		installedFeatures = UpdateUI.getInstalledFeatures(feature);
		if (installedFeatures.length == 0)
			installedFeatures = UpdateUI.getInstalledFeatures(feature, false);

		setHeadingText(feature.getLabel());
		providerLabel.setText(feature.getProvider());
		versionLabel.setText(
			feature.getVersionedIdentifier().getVersion().toString());
		String installedVersion = getInstalledVersionText(feature);
		if (installedVersion == null)
			installedVersion = UpdateUI.getString(KEY_NOT_INSTALLED);
		installedVersionLabel.setText(installedVersion);
		long size = feature.getDownloadSize();
		String sizeText;
		if (size != -1) {
			String stext = Long.toString(size);
			sizeText = UpdateUI.getFormattedMessage(KEY_SIZE_VALUE, stext);
		} else {
			sizeText = UpdateUI.getString(KEY_UNKNOWN_SIZE_VALUE);
		}
		sizeLabel.setText(sizeText);
		estimatedTime.setText(getEstimatedTimeText(feature, size));

		if (feature.getDescription() != null
			&& feature.getDescription().getAnnotation() != null)
			descriptionText.setText(feature.getDescription().getAnnotation());
		else
			descriptionText.setText("");

		Image logoImage = loadProviderImage(feature);
		if (logoImage == null)
			logoImage = providerImage;
		imageLabel.setImage(logoImage);
		infoLinkURL = null;
		if (feature.getDescription() != null)
			infoLinkURL = feature.getDescription().getURL();
		infoLinkLabel.setVisible(infoLinkURL != null);

		setOS(feature.getOS());
		setWS(feature.getWS());
		setNL(feature.getNL());
		setArch(feature.getOSArch());

		licenseLink.setInfo(feature.getLicense());
		copyrightLink.setInfo(feature.getCopyright());
		this.reinstallCode = 0;

		UpdateModel model = UpdateUI.getDefault().getUpdateModel();
		PendingChange relatedJob =
			model.findRelatedPendingChange(currentFeature);
		doButton.setVisible(getDoButtonVisibility(false, relatedJob));
		addButton.setVisible(getDoButtonVisibility(true, relatedJob));
		itemsLink.setVisible(addButton.isVisible());
		groupUpdatesLabel.setVisible(addButton.isVisible());
		if (addButton.isVisible()) {
			if (relatedJob != null) {
				IFeature relatedFeature = relatedJob.getFeature();
				addButton.setEnabled(
					currentFeature.getVersionedIdentifier().equals(
						relatedFeature.getVersionedIdentifier()));
			} else
				addButton.setEnabled(true);
		}
		addBlock = true;
		addButton.setSelection(
			relatedJob != null && relatedJob.isProcessed() == false);
		addBlock = false;
		updateButtonText(newerVersion);
		reflow();
		updateSize();
		((Composite) getControl()).redraw();
	}

	private String getEstimatedTimeText(IFeature feature, long size) {
		long estimate = SiteManager.getEstimatedTransferRate(feature.getURL());
		String estimateFormat = null;
		if (estimate >= 0 && size != -1) {
			long nhours = estimate / 3600000;
			long nminutes = estimate % 3600000;

			if (nhours == 0 && nminutes == 0) {
				estimateFormat = UpdateUI.getString(KEY_MINUTE_ESTIMATE_VALUE);
			} else {
				String hours = Long.toString(nhours);
				String minutes = Long.toString(nminutes);

				estimateFormat =
					UpdateUI.getFormattedMessage(
						KEY_ESTIMATE_VALUE,
						new String[] { hours, minutes });
			}
		} else {
			estimateFormat = UpdateUI.getString(KEY_UNKNOWN_ESTIMATE_VALUE);
		}
		return estimateFormat;
	}

	private boolean getDoButtonVisibility(
		boolean add,
		PendingChange relatedJob) {
		if (currentFeature instanceof MissingFeature) {
			MissingFeature mf = (MissingFeature) currentFeature;
			if (mf.isOptional() && mf.getOriginatingSiteURL() != null)
				return add ? false : true;
			else
				return false;
		}

		if (currentAdapter == null)
			return false;

		boolean localContext = currentAdapter instanceof IConfiguredSiteContext;

		if (currentAdapter.isIncluded()) {
			if (!localContext)
				return false;
			if (!currentAdapter.isOptional())
				return false;
		}

		if (localContext) {
			IConfiguredSiteContext context =
				(IConfiguredSiteContext) currentAdapter;
			if (!context.getInstallConfiguration().isCurrent())
				return false;
			if (context.getConfiguredSite().isEnabled() == false)
				return false;
		}

		if (relatedJob != null) {
			/*if (add)
				return false;
			*/
			if (relatedJob.isProcessed())
				return false;
		}

		if (localContext) {
			IConfiguredSiteContext context =
				(IConfiguredSiteContext) currentAdapter;
			if (!context.getInstallConfiguration().isCurrent())
				return false;
			else
				return true;
		} else {
			// found on a remote site
			// Cannot install feature without a license
			if (!UpdateModel.hasLicense(currentFeature))
				return false;
		}
		// Random site feature
		if (alreadyInstalled) {
			if (isBrokenFeatureUpdate()) {
				reinstallCode = REPAIR;
				return true;
			}
			if (isOptionalFeatureInstall()) {
				reinstallCode = CHANGE;
				return true;
			}
			return false;
		}
		// Not installed - check if there are other 
		// features with this ID that are installed
		// and that are newer than this one
		if (installedFeatures.length > 0 && !newerVersion)
			return false;
		return true;
	}

	private boolean isBrokenFeatureUpdate() {
		if (installedFeatures.length != 1)
			return false;
		IFeature installedFeature = installedFeatures[0];
		if (installedFeature
			.getVersionedIdentifier()
			.equals(currentFeature.getVersionedIdentifier())) {
			return isBroken(installedFeature);
		}
		return false;
	}

	private boolean isBroken(IFeature feature) {
		try {
			IStatus status =
				SiteManager.getLocalSite().getFeatureStatus(feature);
			if (status != null && status.getSeverity() == IStatus.ERROR)
				return true;
		} catch (CoreException e) {
		}
		return false;
	}

	private boolean isOptionalFeatureInstall() {
		return hasMissingOptionalFeatures(installedFeatures[0]);
	}

	private boolean hasMissingOptionalFeatures(IFeature feature) {
		try {
			IIncludedFeatureReference refs[] =
				feature.getIncludedFeatureReferences();
			for (int i = 0; i < refs.length; i++) {
				IIncludedFeatureReference ref = refs[i];

				try {
					IFeature child = ref.getFeature(null);

					// not missing - try children
					if (hasMissingOptionalFeatures(child))
						return true;
				} catch (CoreException e) {
					// missing - if optional, return true
					if (ref.isOptional())
						return true;
				}
			}
		} catch (CoreException e) {
			// problem with the feature itself
		}
		return false;
	}

	private boolean getUninstallButtonVisibility() {
		/*
		 * We will not allow uninstalls for now.
		if (currentFeature instanceof MissingFeature)
			return false;
		if (currentAdapter == null || currentAdapter.isIncluded())
			return false;
		UpdateModel model = UpdateUI.getDefault().getUpdateModel();
		if (model.isPending(currentFeature))
			return false;
		if (currentAdapter instanceof IConfiguredSiteContext) {
			boolean configured = isConfigured();
			return !configured;
		}
		*/
		return false;
	}

	private boolean isConfigured() {
		if (currentAdapter instanceof IConfiguredSiteContext) {
			IConfiguredSiteContext context =
				(IConfiguredSiteContext) currentAdapter;
			IConfiguredSite csite = context.getConfiguredSite();
			IFeatureReference fref =
				csite.getSite().getFeatureReference(currentFeature);
			IFeatureReference[] cfeatures = csite.getConfiguredFeatures();
			for (int i = 0; i < cfeatures.length; i++) {
				if (cfeatures[i].equals(fref))
					return true;
			}
		}
		return false;
	}

	private void updateButtonText(boolean update) {
		if (reinstallCode == REPAIR) {
			doButton.setText(UpdateUI.getString(KEY_DO_REPAIR));
			addButton.setText(UpdateUI.getString(KEY_BATCH_REPAIR));
			return;
		}
		if (reinstallCode == CHANGE) {
			doButton.setText(UpdateUI.getString(KEY_DO_CHANGE));
			addButton.setText(UpdateUI.getString(KEY_BATCH_CHANGE));
			return;
		}
		if (currentFeature instanceof MissingFeature) {
			MissingFeature mf = (MissingFeature) currentFeature;
			if (mf.isOptional() && mf.getOriginatingSiteURL() != null) {
				doButton.setText(UpdateUI.getString(KEY_DO_INSTALL));
				return;
			}
		}
		if (currentAdapter instanceof IConfiguredSiteContext) {
			boolean configured = isConfigured();
			if (configured) {
				doButton.setText(UpdateUI.getString(KEY_DO_UNCONFIGURE));
				addButton.setText(UpdateUI.getString(KEY_BATCH_UNCONFIGURE));
			} else {
				doButton.setText(UpdateUI.getString(KEY_DO_CONFIGURE));
				addButton.setText(UpdateUI.getString(KEY_BATCH_CONFIGURE));
			}
		} else if (update && !alreadyInstalled) {
			doButton.setText(UpdateUI.getString(KEY_DO_UPDATE));
			addButton.setText(UpdateUI.getString(KEY_BATCH_UPDATE));
		} else {
			doButton.setText(UpdateUI.getString(KEY_DO_INSTALL));
			addButton.setText(UpdateUI.getString(KEY_BATCH_INSTALL));
		}
	}

	private Image loadProviderImage(IFeature feature) {
		Image image = null;
		URL imageURL = feature.getImage();
		if (imageURL == null)
			return null;
		// check table
		image = (Image) imageCache.get(imageURL);
		if (image == null) {
			ImageDescriptor id = ImageDescriptor.createFromURL(imageURL);
			try {
				image = id.createImage();
			} catch (SWTException e) {
				image = null;
			}
			if (image != null)
				imageCache.put(imageURL, image);
		}
		return image;
	}

	private void reflow() {
		versionLabel.getParent().layout(true);
		doButton.getParent().layout(true);
		imageLabel.getParent().layout(true);
		((Composite) getControl()).layout(true);
	}

	private void setOS(String os) {
		if (os == null)
			osLabel.setText("");
		else {
			String[] array = getTokens(os);
			StringBuffer buf = new StringBuffer();
			for (int i = 0; i < array.length; i++) {
				if (i > 0)
					buf.append("\n");
				buf.append(mapOS(array[i]));
			}
			osLabel.setText(buf.toString());
		}
	}

	private String mapOS(String key) {
		return key;
	}

	private String mapWS(String key) {
		return key;
	}

	private String mapArch(String key) {
		return key;
	}

	private String mapNL(String nl) {
		String language, country;

		int loc = nl.indexOf('_');
		if (loc != -1) {
			language = nl.substring(0, loc);
			country = nl.substring(loc + 1);
		} else {
			language = nl;
			country = "";
		}
		Locale locale = new Locale(language, country);
		return locale.getDisplayName();
	}

	private void setWS(String ws) {
		if (ws == null)
			wsLabel.setText("");
		else {
			String[] array = getTokens(ws);
			StringBuffer buf = new StringBuffer();
			for (int i = 0; i < array.length; i++) {
				if (i > 0)
					buf.append("\n");
				buf.append(mapWS(array[i]));
			}
			wsLabel.setText(buf.toString());
		}
	}

	private void setArch(String arch) {
		if (arch == null)
			archLabel.setText("");
		else {
			String[] array = getTokens(arch);
			StringBuffer buf = new StringBuffer();
			for (int i = 0; i < array.length; i++) {
				if (i > 0)
					buf.append("\n");
				buf.append(mapArch(array[i]));
			}
			archLabel.setText(buf.toString());
		}
	}

	private void setNL(String nl) {
		if (nl == null)
			nlLabel.setText("");
		else {
			String[] array = getTokens(nl);
			StringBuffer buf = new StringBuffer();
			for (int i = 0; i < array.length; i++) {
				if (i > 0)
					buf.append("\n");
				buf.append(mapNL(array[i]));
			}
			nlLabel.setText(buf.toString());
		}
	}

	private String[] getTokens(String source) {
		Vector result = new Vector();
		StringTokenizer stok = new StringTokenizer(source, ",");
		while (stok.hasMoreTokens()) {
			String tok = stok.nextToken();
			result.add(tok);
		}
		return (String[]) result.toArray(new String[result.size()]);
	}

	private void openURL(final String url) {
		BusyIndicator.showWhile(getControl().getDisplay(), new Runnable() {
			public void run() {
				DetailsView.showURL(url);
			}
		});
	}

	private void doAdd(boolean selection) {
		UpdateModel model = UpdateUI.getDefault().getUpdateModel();
		if (selection) {
			int mode = getCurrentJobType();
			PendingChange job = createPendingChange(mode);
			model.addPendingChange(job);
			/*
			IWorkbenchPage page = UpdateUI.getActivePage();
			IViewPart view = page.findView(UpdatePerspective.ID_ITEMS);
			if (view != null) {
				page.bringToTop(view);
			}
			*/
		} else {
			PendingChange job = model.findRelatedPendingChange(currentFeature);
			if (job != null)
				model.removePendingChange(job);
		}
	}

	private void doUninstall() {
		executeJob(PendingChange.UNINSTALL);
	}

	private void executeJob(int mode) {
		if (currentFeature != null) {
			if (mode == PendingChange.INSTALL) {
				if (testDependencies(currentFeature) == false)
					return;
			}
			PendingChange job = createPendingChange(mode);
			executeJob(getControl().getShell(), job, true);
		}
	}

	private void executeOptionalInstall(MissingFeature mf) {
		VersionedIdentifier vid = mf.getVersionedIdentifier();
		URL siteURL = mf.getOriginatingSiteURL();
		IConfiguredSite targetSite = null;
		if (mf.getParent() != null) {
			ISite psite = mf.getParent().getSite();
			targetSite = psite.getCurrentConfiguredSite();
		}
		IFeature feature =
			fetchFeatureFromServer(getControl().getShell(), siteURL, vid);
		if (feature != null) {
			PendingChange job = new PendingChange(feature, targetSite);
			executeJob(getControl().getShell(), job, true);
		}
	}

	public static IFeature fetchFeatureFromServer(
		Shell shell,
		final URL siteURL,
		final VersionedIdentifier vid) {
		// Locate remote site, find the optional feature
		// and install it

		final IFeature[] result = new IFeature[1];
		final CoreException[] exception = new CoreException[1];

		BusyIndicator.showWhile(shell.getDisplay(), new Runnable() {
			public void run() {
				try {
					ISite site = SiteManager.getSite(siteURL, null);
					IFeatureReference[] refs = site.getFeatureReferences();
					result[0] = findFeature(vid, refs);
				} catch (CoreException e) {
					exception[0] = e;
				}
			}
		});
		IStatus status = null;
		if (exception[0] != null) {
			// Show error dialog
			status = exception[0].getStatus();
		} else if (result[0] != null) {
			return result[0];
		} else {
			String message =
				UpdateUI.getFormattedMessage(
					KEY_OPTIONAL_INSTALL_MESSAGE,
					siteURL.toString());
			status =
				new Status(
					IStatus.ERROR,
					UpdateUI.PLUGIN_ID,
					IStatus.OK,
					message,
					null);
		}
		if (status != null) {
			// Show error dialog
			ErrorDialog.openError(
				shell,
				UpdateUI.getString(KEY_OPTIONAL_INSTALL_TITLE),
				null,
				status);
		}
		return null;
	}

	private static IFeature findFeature(
		VersionedIdentifier vid,
		IFeatureReference[] refs) {
		for (int i = 0; i < refs.length; i++) {
			IFeatureReference ref = refs[i];
			try {
				VersionedIdentifier refVid = ref.getVersionedIdentifier();
				if (refVid.equals(vid)) {
					return ref.getFeature(null);
				}
				// Try children
				IFeature feature = ref.getFeature(null);
				IFeatureReference[] irefs =
					feature.getIncludedFeatureReferences();
				IFeature result = findFeature(vid, irefs);
				if (result != null)
					return result;
			} catch (CoreException e) {
			}
		}
		return null;
	}

	public static void executeJob(
		Shell shell,
		final PendingChange job,
		final boolean needLicensePage) {
		IStatus validationStatus =
			ActivityConstraints.validatePendingChange(job);
		if (validationStatus != null) {
			ErrorDialog.openError(
				UpdateUI.getActiveWorkbenchShell(),
				null,
				null,
				validationStatus);
			return;
		}
		if (job.getJobType() == PendingChange.UNCONFIGURE
			&& job.getFeature().isPatch()) {
			unconfigurePatch(shell, job.getFeature());
			return;
		}
		BusyIndicator.showWhile(shell.getDisplay(), new Runnable() {
			public void run() {
				InstallWizard wizard = new InstallWizard(job, needLicensePage);
				WizardDialog dialog =
					new InstallWizardDialog(
						UpdateUI.getActiveWorkbenchShell(),
						wizard);
				dialog.create();
				dialog.getShell().setSize(600, 500);
				dialog.open();
				if (wizard.isSuccessfulInstall())
					UpdateUI.informRestartNeeded();
			}
		});
	}

	private static void unconfigurePatch(Shell shell, IFeature feature) {
		IInstallConfiguration config =
			UpdateUI.getBackupConfigurationFor(feature);
		if (config == null) {
			String message =
				"This feature is a patch and cannot be directly disabled. Locate a configuration before it was installed and revert to it instead.";
			MessageDialog.openError(shell, "Disable Feature", message);
			return;
		}
		try {
			ILocalSite localSite = SiteManager.getLocalSite();
			boolean success = RevertSection.performRevert(config, false, false);
			if (success) {
				localSite.removeFromPreservedConfigurations(config);
				localSite.save();
				UpdateUI.informRestartNeeded();
			}
		} catch (CoreException e) {
			UpdateUI.logException(e);
		}
	}

	private void doButtonSelected() {
		if (currentFeature != null) {
			if (currentFeature instanceof MissingFeature) {
				MissingFeature mf = (MissingFeature) currentFeature;
				if (mf.isOptional() && mf.getOriginatingSiteURL() != null) {
					executeOptionalInstall(mf);
					return;
				}
			}
			int mode;
			if (currentAdapter instanceof IConfiguredSiteContext) {
				boolean configured = isConfigured();
				if (configured)
					mode = PendingChange.UNCONFIGURE;
				else
					mode = PendingChange.CONFIGURE;
			} else {
				mode = PendingChange.INSTALL;
			}
			executeJob(mode);
		}
	}

	private int getCurrentJobType() {
		int mode;
		if (currentAdapter instanceof IConfiguredSiteContext) {
			boolean configured = isConfigured();
			if (configured)
				mode = PendingChange.UNCONFIGURE;
			else
				mode = PendingChange.CONFIGURE;
		} else {
			mode = PendingChange.INSTALL;
		}
		return mode;
	}

	private boolean testDependencies(IFeature feature) {
		// NOTE: testing and searching for dependencies is disabled
		//       at this point. The code needs to correctly handle
		//       matching rules that can be specified on the dependencies.
		if (true)
			return true;

		IImport[] imports = feature.getImports();
		if (imports.length == 0)
			return true;
		ArrayList missing = new ArrayList();
		try {
			ILocalSite localSite = SiteManager.getLocalSite();
			IInstallConfiguration config = localSite.getCurrentConfiguration();
			IConfiguredSite[] configSites = config.getConfiguredSites();

			for (int i = 0; i < imports.length; i++) {
				if (!isOnTheList(imports[i], configSites)) {
					missing.add(imports[i]);
				}
			}
		} catch (CoreException e) {
			UpdateUI.logException(e);
			return false;
		}
		if (missing.size() > 0) {
			// show missing plug-in dialog and ask to search
			MessageDialog dialog =
				new MessageDialog(
					getControl().getShell(),
					UpdateUI.getString(KEY_MISSING_TITLE),
					(Image) null,
					UpdateUI.getString(KEY_MISSING_MESSAGE),
					MessageDialog.WARNING,
					new String[] {
						UpdateUI.getString(KEY_MISSING_SEARCH),
						UpdateUI.getString(KEY_MISSING_ABORT)},
					0);
			int result = dialog.open();
			if (result == 0)
				initiatePluginSearch(missing);
			return false;
		} else
			return true;
	}
	private boolean isOnTheList(
		IImport iimport,
		IConfiguredSite[] configSites) {
		for (int i = 0; i < configSites.length; i++) {
			IConfiguredSite csite = configSites[i];
			ISite site = csite.getSite();
			IPluginEntry[] entries = site.getPluginEntries();
			if (isOnTheList(iimport, entries))
				return true;
		}
		return false;
	}
	private boolean isOnTheList(IImport iimport, IPluginEntry[] entries) {
		VersionedIdentifier importId = iimport.getVersionedIdentifier();
		PluginVersionIdentifier version = importId.getVersion();
		boolean noVersion =
			version.getMajorComponent() == 0
				&& version.getMinorComponent() == 0
				&& version.getServiceComponent() == 0;
		for (int i = 0; i < entries.length; i++) {
			IPluginEntry entry = entries[i];
			VersionedIdentifier entryId = entry.getVersionedIdentifier();
			if (noVersion) {
				if (importId.getIdentifier().equals(entryId.getIdentifier()))
					return true;
			} else if (entryId.equals(importId))
				return true;
		}
		return false;
	}

	private void initiatePluginSearch(ArrayList missing) {
		SearchCategoryDescriptor desc =
			SearchCategoryRegistryReader.getDefault().getDescriptor(
				"org.eclipse.update.ui.plugins");
		if (desc == null)
			return;
		String name =
			UpdateUI.getFormattedMessage(
				KEY_SEARCH_OBJECT_NAME,
				currentFeature.getLabel());
		SearchObject search = new SearchObject(name, desc, true);
		search.setPersistent(false);
		search.setInstantSearch(true);
		search.setSearchBookmarks(true);
		search.setSearchDiscovery(true);
		String value = PluginsSearchCategory.encodeImports(missing);
		search.getSettings().put("imports", value);
		UpdateModel model = UpdateUI.getDefault().getUpdateModel();
		try {
			UpdateUI.getActivePage().showView(UpdatePerspective.ID_UPDATES);
		} catch (PartInitException e) {
		}
		model.addBookmark(search);
		try {
			UpdateUI.getActivePage().showView(UpdatePerspective.ID_DETAILS);
		} catch (PartInitException e) {
		}
	}

	private PendingChange createPendingChange(int type) {
		if (type == PendingChange.INSTALL && installedFeatures.length > 0) {
			return new PendingChange(
				installedFeatures[0],
				currentFeature,
				alreadyInstalled);
		} else {
			return new PendingChange(currentFeature, type);
		}
	}
}