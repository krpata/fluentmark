package net.certiv.fluentmark;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.editors.text.EditorsUI;
import org.eclipse.ui.forms.FormColors;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;
import org.osgi.service.prefs.BackingStoreException;

import net.certiv.fluentmark.editor.FluentMkTextTools;
import net.certiv.fluentmark.editor.color.ColorManager;
import net.certiv.fluentmark.editor.color.IColorManager;
import net.certiv.fluentmark.preferences.Prefs;

/**
 * The activator class controls the plug-in life cycle
 */
public class FluentMkUI extends AbstractUIPlugin {

	// The plug-in ID
	public static final String PLUGIN_ID = "net.certiv.fluentmark"; //$NON-NLS-1$

	// The shared instance
	private static FluentMkUI plugin;

	private IPreferenceStore combinedStore;

	private FluentMkImages fluentMkImages;

	private FormToolkit dialogsFormToolkit;

	private ColorManager colorManager;

	private FluentMkTextTools fluentMkTextTools;

	public FluentMkUI() {
		super();
	}

	public void start(BundleContext context) throws Exception {
		super.start(context);
		fluentMkImages = new FluentMkImages(context.getBundle(), this);
		plugin = this;
	}

	public void stop(BundleContext context) throws Exception {
		plugin = null;
		super.stop(context);
	}

	/**
	 * Returns the shared instance
	 */
	public static FluentMkUI getDefault() {
		return plugin;
	}

	public IColorManager getColorMgr() {
		if (colorManager == null) {
			colorManager = new ColorManager(true);
		}
		return colorManager;
	}

	/**
	 * Returns a chained preference store representing the combined values of the FluentMkUI,
	 * EditorsUI, and PlatformUI stores.
	 */
	public IPreferenceStore getCombinedPreferenceStore() {
		if (combinedStore == null) {
			List<IPreferenceStore> stores = new ArrayList<>();
			stores.add(getPreferenceStore()); // FluentMkUI store
			stores.add(EditorsUI.getPreferenceStore());
			stores.add(PlatformUI.getPreferenceStore());
			combinedStore = new WritableChainedPreferenceStore(stores.toArray(new IPreferenceStore[stores.size()]));
		}
		return combinedStore;
	}

	public FormToolkit getDialogsFormToolkit() {
		if (dialogsFormToolkit == null) {
			FormColors colors = new FormColors(Display.getCurrent());
			colors.setBackground(null);
			colors.setForeground(null);
			dialogsFormToolkit = new FormToolkit(colors);
		}
		return dialogsFormToolkit;
	}

	public FluentMkTextTools getTextTools() {
		if (fluentMkTextTools == null) {
			fluentMkTextTools = new FluentMkTextTools(true);
		}
		return fluentMkTextTools;
	}

	/** Returns the image provider */
	public FluentMkImages getImageProvider() {
		return fluentMkImages;
	}

	public static Image getImage(String key) {
		return plugin.fluentMkImages.get(key);
	}

	public static ImageDescriptor getDescriptor(String key) {
		return plugin.fluentMkImages.getDescriptor(key);
	}

	/**
	 * Returns the content assist additional info focus affordance string.
	 *
	 * @return the affordance string which is <code>null</code> if the preference is disabled
	 * @see EditorsUI#getTooltipAffordanceString()
	 * @since 3.4
	 */
	public static String getAdditionalInfoAffordanceString() {
		if (!EditorsUI.getPreferenceStore().getBoolean(Prefs.EDITOR_SHOW_TEXT_HOVER_AFFORDANCE)) {
			return null;
		}
		return "Press 'Tab' from proposal table or click for focus"; //$NON-NLS-1$
	}

	/**
	 * Returns the workspace root default charset encoding.
	 *
	 * @return the name of the default charset encoding for workspace root.
	 * @see IContainer#getDefaultCharset()
	 * @see ResourcesPlugin#getEncoding()
	 */
	public static String getEncoding() {
		try {
			return ResourcesPlugin.getWorkspace().getRoot().getDefaultCharset();
		} catch (IllegalStateException e) {
			return System.getProperty("file.encoding"); //$NON-NLS-1$
		} catch (CoreException e) {
			return ResourcesPlugin.getEncoding();
		}
	}

	/**
	 * Flushes the instance scope of this plug-in.
	 */
	public static void flushInstanceScope() {
		try {
			InstanceScope.INSTANCE.getNode(PLUGIN_ID).flush();
		} catch (BackingStoreException e) {
			Log.error(e);
		}
	}
}