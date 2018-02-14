package com.brainfuse.jobs.plugin;

import java.rmi.NoSuchObjectException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.spi.ClassLoadHelper;
import org.quartz.spi.SchedulerPlugin;

public abstract class SharedUnicastPlugin extends UnicastRemoteObject
		implements SchedulerPlugin, Remote {
	private static final long serialVersionUID = -4307516663305890014L;

	private boolean exportRMI = true;

	protected SharedUnicastPlugin() throws RemoteException {
		super();
	}

	@Override
	public void initialize(String name, Scheduler scheduler,
			ClassLoadHelper loadHelper) throws SchedulerException {

		initializeInternal(name, scheduler, loadHelper);
		this.unexportIfSet();

	}

	protected abstract void initializeInternal(String name, Scheduler scheduler,
			ClassLoadHelper loadHelper) throws SchedulerException;

	public boolean isExportRMI() {
		return exportRMI;
	}

	public void setExportRMI(boolean exportRMI) {
		this.exportRMI = exportRMI;
	}

	public void unexportIfSet() {
		if (isExportRMI())
			return;
		try {
			unexportObject(this, true);
		} catch (NoSuchObjectException e) {
			//ok to ignore
		}
	}
}
