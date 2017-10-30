package com.brainfuse.jobs.plugin;

import java.rmi.RemoteException;
import java.util.Map;

import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.spi.ClassLoadHelper;
import org.quartz.spi.SchedulerPlugin;

import myschedule.quartz.extra.ScriptExecutor;
import myschedule.quartz.extra.util.ScriptingUtils;
import myschedule.quartz.extra.util.Utils;

public class ScriptExecutorImpl extends SharedUnicastPlugin
implements ScriptExecutor, SchedulerPlugin{
	Scheduler scheduler;
	public ScriptExecutorImpl() throws RemoteException {
		super();
	}
	

	/**
	 * 
	 */
	private static final long serialVersionUID = 8116232514825983235L;
	
	/* (non-Javadoc)
	 * @see com.brainfuse.quartz.bootstrap.ScriptExecutor#execute(java.lang.String, java.lang.String)
	 */
	@Override
	public void execute(String scriptEngineName, String javascript) throws RemoteException{
		 // Bind scheduler as implicit variable
        Map<String, Object> bindings = Utils.toMap("scheduler", scheduler);
        try {
            ScriptingUtils.runScriptText(scriptEngineName, javascript, bindings);
        } catch (RuntimeException e) {
        	throw e;
        }
	}

	@Override
	public void initializeInternal(String name, Scheduler scheduler,
			ClassLoadHelper loadHelper) throws SchedulerException {
		this.scheduler=scheduler;
		
	    scheduler.getContext().put(SCRIPT_EXECUTOR_KEY, this);
		
	}

	@Override
	public void start() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void shutdown() {
		// TODO Auto-generated method stub
		
	}
	

}
