load("nashorn:mozilla_compat.js");
logger.info("Plugin start");

importClass(Packages.myschedule.quartz.extra.ScriptingSchedulerPluginTest);
ScriptingSchedulerPluginTest.RESULT_FILE.appendLine('start: ' + new Date());
