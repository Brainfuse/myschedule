package myschedule.quartz.extra.job;

import java.rmi.Remote;
import java.rmi.RemoteException;

import org.quartz.JobDetail;
import org.quartz.SchedulerException;

public interface JobSubmitter extends Remote {
	
	String JOB_SUBMITTER_KEY = "JobSubmitterPlugin.instance";

	JobDetail submitJob(JobInfo info) throws RemoteException, ClassNotFoundException, SchedulerException;

}
