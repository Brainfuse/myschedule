load("nashorn:mozilla_compat.js");
logger.info("Plugin shutdown");

importClass(Packages.myschedule.quartz.extra.ScriptingSchedulerPluginTest);
ScriptingSchedulerPluginTest.RESULT_FILE.appendLine('shutdown: ' + new Date());
