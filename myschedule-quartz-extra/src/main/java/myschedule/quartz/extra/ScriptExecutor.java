package myschedule.quartz.extra;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface ScriptExecutor extends Remote{

	String SCRIPT_EXECUTOR_KEY = "ScriptExecutorPlugin.instance";

	void execute(String scriptEngineName, String javascript)
			throws RemoteException;

}