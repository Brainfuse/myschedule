load("nashorn:mozilla_compat.js");
importClass(Packages.myschedule.quartz.extra.job.LoggerJob);
importClass(Packages.org.quartz.JobBuilder);
importClass(Packages.org.quartz.TriggerBuilder);
importClass(Packages.org.quartz.SimpleScheduleBuilder);
var job = JobBuilder
  .newJob(LoggerJob.class)
  .withIdentity("simpleJob")
  .build();
var trigger = TriggerBuilder
  .newTrigger()
  .withIdentity("simpleJob")
  .withSchedule(
    SimpleScheduleBuilder.repeatHourlyForever())
  .build();
scheduler.scheduleJob(job, trigger);
