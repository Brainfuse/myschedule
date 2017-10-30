package myschedule.quartz.extra.job;

import static org.quartz.JobBuilder.newJob;

import java.rmi.RemoteException;
import java.util.Map;

import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.spi.ClassLoadHelper;
import org.quartz.spi.SchedulerPlugin;

import com.brainfuse.jobs.plugin.SharedUnicastPlugin;

public class JobSubmitterImpl extends SharedUnicastPlugin implements JobSubmitter, SchedulerPlugin {

	private Scheduler scheduler;

	public JobSubmitterImpl() throws RemoteException {
		super();
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = 5188111843135287389L;

	@Override
	public void initializeInternal(String name, Scheduler scheduler,
			ClassLoadHelper loadHelper) throws SchedulerException {
		this.scheduler = scheduler;
		this.scheduler.getContext().put(JobSubmitter.JOB_SUBMITTER_KEY, this);

	}

	@Override
	public void start() {
		// TODO Auto-generated method stub

	}

	@Override
	public void shutdown() {
		// TODO Auto-generated method stub

	}

	@Override
	public JobDetail submitJob(JobInfo info) throws RemoteException, ClassNotFoundException, SchedulerException {
		JobDetail jobDetail = buildJobDetail(info);
		this.scheduler.addJob(jobDetail, false);
		return jobDetail;
	}
	
	public static JobDetail buildJobDetail(JobInfo jobInfo) throws ClassNotFoundException {
        org.quartz.JobBuilder jobBuilder = newJob();

        JobDataMap jobDataMap = new JobDataMap();
        Map<String, Object> jobParams = jobInfo.getJobParams();
		if (jobParams != null) {
			for (String paramKey : jobParams.keySet()) {
				jobDataMap.put(paramKey, jobParams.get(paramKey));
			}
		}
        
        
        jobBuilder.ofType((Class<? extends Job>) Class.forName(jobInfo.getJobClass())).
                withIdentity(jobInfo.getName(), jobInfo.getGroup()).
                withDescription(jobInfo.getDescription()).
                storeDurably(jobInfo.isStoreDurably()).
                requestRecovery(jobInfo.isRequestRecovery()).
                usingJobData(jobDataMap);

        JobDetail build = jobBuilder.build();
		return build;
    }

}
