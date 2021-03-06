package com.netifera.platform.ui.internal.interop;

import java.io.File;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.util.tracker.ServiceTracker;

import com.netifera.platform.api.probe.IProbeManagerService;
import com.netifera.platform.net.model.INetworkEntityFactory;
import com.netifera.platform.probebuild.api.IProbeBuilderService;

public class Activator implements BundleActivator {
	private ServiceTracker probeBuilderTracker;
	private ServiceTracker probeManagerTracker;
	private ServiceTracker networkEntityFactoryTracker;

	private static Activator instance;
	
	public static Activator getInstance() {
		return instance;
	}

	/*
	 * (non-Javadoc)
	 * @see org.osgi.framework.BundleActivator#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext context) throws Exception {
		instance = this;
		
		System.setProperty("com.netifera.platform.probebuild.basedir", System.getProperty("user.home", System.getenv("HOME")) + File.separator + ".netifera" + File.separator + "probebuild" + File.separator);
		
		probeBuilderTracker = new ServiceTracker(context, IProbeBuilderService.class.getName(), null);
		probeBuilderTracker.open();

		probeManagerTracker = new ServiceTracker(context, IProbeManagerService.class.getName(), null);
		probeManagerTracker.open();
		
		networkEntityFactoryTracker = new ServiceTracker(context, INetworkEntityFactory.class.getName(), null);
		networkEntityFactoryTracker.open();
	}

	/*
	 * (non-Javadoc)
	 * @see org.osgi.framework.BundleActivator#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext context) throws Exception {
	}

	public IProbeBuilderService getProbeBuilder() {
		return (IProbeBuilderService) probeBuilderTracker.getService();
	}

	public IProbeManagerService getProbeManager() {
		return (IProbeManagerService) probeManagerTracker.getService();
	}

	public INetworkEntityFactory getNetworkEntityFactory() {
		return (INetworkEntityFactory) networkEntityFactoryTracker.getService();
	}
}
