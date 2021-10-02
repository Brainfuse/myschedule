package myschedule.web.ui;

import static java.util.stream.Collectors.toList;
import static org.quartz.JobKey.jobKey;
import static org.quartz.TriggerKey.triggerKey;
import static org.quartz.impl.jdbcjobstore.Constants.COL_ENTRY_ID;
import static org.quartz.impl.jdbcjobstore.Constants.COL_ENTRY_STATE;
import static org.quartz.impl.jdbcjobstore.Constants.COL_FIRED_TIME;
import static org.quartz.impl.jdbcjobstore.Constants.COL_INSTANCE_NAME;
import static org.quartz.impl.jdbcjobstore.Constants.COL_IS_NONCONCURRENT;
import static org.quartz.impl.jdbcjobstore.Constants.COL_JOB_GROUP;
import static org.quartz.impl.jdbcjobstore.Constants.COL_JOB_NAME;
import static org.quartz.impl.jdbcjobstore.Constants.COL_PRIORITY;
import static org.quartz.impl.jdbcjobstore.Constants.COL_REQUESTS_RECOVERY;
import static org.quartz.impl.jdbcjobstore.Constants.COL_SCHED_TIME;
import static org.quartz.impl.jdbcjobstore.Constants.COL_TRIGGER_GROUP;
import static org.quartz.impl.jdbcjobstore.Constants.COL_TRIGGER_NAME;
import static org.quartz.impl.jdbcjobstore.Constants.STATE_ACQUIRED;
import static org.quartz.impl.jdbcjobstore.StdJDBCConstants.SELECT_FIRED_TRIGGERS;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.quartz.JobExecutionContext;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.TriggerKey;
import org.quartz.core.QuartzScheduler;
import org.quartz.core.QuartzSchedulerResources;
import org.quartz.impl.jdbcjobstore.FiredTriggerRecord;
import org.quartz.impl.jdbcjobstore.JobStoreSupport;
import org.quartz.impl.jdbcjobstore.Util;
import org.quartz.utils.DBConnectionManager;

import myschedule.quartz.extra.QuartzRuntimeException;
import myschedule.quartz.extra.SchedulerTemplate;
import myschedule.web.MySchedule;

/**
 * Basic from https://stackoverflow.com/a/17242375/9519287
 * @author sgabriel
 * Date: Sep 29, 2021
 */
class QuartzClusterJobStatusService
{
	interface FiredJob{

		JobKey getJobKey();
    	
    }
    interface FiredTrigger{

		TriggerKey getTriggerKey();

		Date getPreviousFireTime();

		Date getNextFireTime();

		JobKey getJobKey();
    	
    }
    interface JobExecutionContextResult{
    	FiredJob getJob();
    	FiredTrigger getTrigger();
    	class Adapter implements JobExecutionContextResult{
    		final JobExecutionContext ctx;

			Adapter(JobExecutionContext ctx) {
				super();
				this.ctx = ctx;
			}

			@Override
			public FiredJob getJob() {
				return new FiredJob() {
					
					@Override
					public JobKey getJobKey() {
						return ctx.getJobDetail().getKey();
					}
				};
			}

			@Override
			public FiredTrigger getTrigger() {
				return new FiredTrigger() {
					
					@Override
					public TriggerKey getTriggerKey() {
						return ctx.getTrigger().getKey();
					}

					@Override
					public Date getPreviousFireTime() {
						return ctx.getTrigger().getPreviousFireTime();
					}

					@Override
					public Date getNextFireTime() {
						return ctx.getTrigger().getNextFireTime();
					}

					@Override
					public JobKey getJobKey() {
						return ctx.getTrigger().getJobKey();
					}
				};
			}
    		
    	}
    }
	static final class JobExecutionContextResultDbImpl
			implements JobExecutionContextResult {
		private final FiredTriggerRecord rec;

		JobExecutionContextResultDbImpl(FiredTriggerRecord rec) {
			this.rec = rec;
		}

		@Override
		public FiredTrigger getTrigger() {
			return new FiredTrigger() {
				@Override
				public TriggerKey getTriggerKey() {
					return rec.getTriggerKey();
				}

				@Override
				public Date getPreviousFireTime() {
					final long timeStamp = rec
							.getScheduleTimestamp();
					return getDateFromTS(timeStamp);
				}

				public Date getDateFromTS(
						final long timeStamp) {
					return new Date(timeStamp / 86400000000l
							- 693595);
				}

				@Override
				public Date getNextFireTime() {
					return getDateFromTS(
							rec.getFireTimestamp());
				}

				@Override
				public JobKey getJobKey() {
					return rec.getJobKey();
				}
			};
		}

		@Override
		public FiredJob getJob() {
			return new FiredJob() {
				@Override
				public JobKey getJobKey() {
					return rec.getJobKey();
				}
			};
		}
	}

	final Scheduler scheduler;

    QuartzClusterJobStatusService(Scheduler quartzScheduler) {
		super();
		this.scheduler = quartzScheduler;
	}

	List<JobExecutionContextResult> getCurrentlyRunningJobs() throws QuartzRuntimeException {
		if (!(this.scheduler instanceof QuartzScheduler))
			try {
				return adapt(this.scheduler.getCurrentlyExecutingJobs());
			} catch (SchedulerException e1) {
				throw new QuartzRuntimeException(e1);
			}

		JobStoreSupport js = getJobStoreSupport(this.scheduler);
		
		try {
			if (!js.isClustered()) {
				final List<JobExecutionContext> currentlyExecutingJobs = this.scheduler
						.getCurrentlyExecutingJobs();
				return adapt(currentlyExecutingJobs);
			}
		} catch (SchedulerException e1) {
			throw new QuartzRuntimeException(e1);
		}
		List<JobExecutionContextResult> resultList = new ArrayList<>();
		try (Connection conn = DBConnectionManager.getInstance()
				.getConnection(js.getDataSource());
				PreparedStatement stmt = conn.prepareStatement(
						Util.rtp(SELECT_FIRED_TRIGGERS, js.getTablePrefix(),
								scheduler.getSchedulerName()))) {

			ResultSet rs = stmt.executeQuery();
			while (rs.next()) {
				FiredTriggerRecord rec = new FiredTriggerRecord();

				rec.setFireInstanceId(rs.getString(COL_ENTRY_ID));
				rec.setFireInstanceState(rs.getString(COL_ENTRY_STATE));
				rec.setFireTimestamp(rs.getLong(COL_FIRED_TIME));
				rec.setScheduleTimestamp(rs.getLong(COL_SCHED_TIME));
				rec.setPriority(rs.getInt(COL_PRIORITY));
				rec.setSchedulerInstanceId(rs.getString(COL_INSTANCE_NAME));
				rec.setTriggerKey(triggerKey(rs.getString(COL_TRIGGER_NAME),
						rs.getString(COL_TRIGGER_GROUP)));
				if (!rec.getFireInstanceState().equals(STATE_ACQUIRED)) {
					rec.setJobDisallowsConcurrentExecution(
							rs.getBoolean(COL_IS_NONCONCURRENT));
					rec.setJobRequestsRecovery(
							rs.getBoolean(COL_REQUESTS_RECOVERY));
					rec.setJobKey(jobKey(rs.getString(COL_JOB_NAME),
							rs.getString(COL_JOB_GROUP)));
				}
				JobExecutionContextResult result = new JobExecutionContextResultDbImpl(
						rec);
				resultList.add(result);
			}
			return resultList;
    	} catch (SQLException | SchedulerException e) {
    		throw new QuartzRuntimeException(e);
    	}
    }

	public List<JobExecutionContextResult> adapt(
			final List<JobExecutionContext> currentlyExecutingJobs) {
		return currentlyExecutingJobs.stream()
				.map(JobExecutionContextResult.Adapter::new).collect(toList());
	}
    public static JobStoreSupport getJobStoreSuppot(String schedulerSettingsName) {
    	MySchedule mySchedule = MySchedule.getInstance();
		SchedulerTemplate scheduler = mySchedule
				.getScheduler(schedulerSettingsName);
		return getJobStoreSupport(scheduler.getScheduler());
    }
    public static JobStoreSupport getJobStoreSupport(Scheduler stdScheduler) {
    	QuartzScheduler sched = (QuartzScheduler) getFieldValue(stdScheduler, "sched");
		return (JobStoreSupport) ((QuartzSchedulerResources) getFieldValue(sched, "resources")).getJobStore();
    }

	private static Object getFieldValue(final Object obj, final String name) {
		final Field[] declaredFields = obj.getClass().getDeclaredFields();
    	for(Field f: declaredFields) {
			if(f.getName().equals(name)) {
    			f.setAccessible(true);
    			try {
					return f.get(obj);
				} catch (IllegalAccessException e) {
					throw new QuartzRuntimeException(e);
				}
    		}
    	}
    	throw new IllegalArgumentException("no resources field found in QuartzScheduler.class");
	}
}