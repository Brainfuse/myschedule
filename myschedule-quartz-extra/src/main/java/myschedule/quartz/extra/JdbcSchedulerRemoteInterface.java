package myschedule.quartz.extra;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

public interface JdbcSchedulerRemoteInterface extends Remote{

	List<List<Object>> getJobHistoryData() throws RemoteException;

}