package myschedule.web.ui;

import static java.util.stream.Collectors.toList;
import static myschedule.web.ui.FilteredHistoryTable.DATE_FORMAT;
import static myschedule.web.ui.FilteredHistoryTable.EVENT_NAME;
import static myschedule.web.ui.FilteredHistoryTable.EVENT_TIME;
import static myschedule.web.ui.FilteredHistoryTable.EVENT_TYPE;
import static myschedule.web.ui.FilteredHistoryTable.HOST_IP_NAME;
import static myschedule.web.ui.FilteredHistoryTable.INFO_1;
import static myschedule.web.ui.FilteredHistoryTable.INFO_2;
import static myschedule.web.ui.FilteredHistoryTable.INFO_3;
import static myschedule.web.ui.FilteredHistoryTable.INFO_4;
import static myschedule.web.ui.FilteredHistoryTable.INFO_5;
import static myschedule.web.ui.FilteredHistoryTable.SCHEDULER_NAME;

import java.rmi.RemoteException;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.quartz.impl.jdbcjobstore.JobStoreSupport;
import org.quartz.utils.DBConnectionManager;

import com.vaadin.data.Container;
import com.vaadin.data.Container.Filter;
import com.vaadin.data.Container.Filterable;
import com.vaadin.data.Item;
import com.vaadin.data.util.IndexedContainer;
import com.vaadin.data.util.sqlcontainer.SQLContainer;
import com.vaadin.data.util.sqlcontainer.connection.JDBCConnectionPool;
import com.vaadin.data.util.sqlcontainer.query.OrderBy;
import com.vaadin.data.util.sqlcontainer.query.TableQuery;
import com.vaadin.data.util.sqlcontainer.query.generator.MSSQLGenerator;

import myschedule.quartz.extra.JdbcSchedulerRemoteInterface;
import myschedule.quartz.extra.SchedulerTemplate;
import myschedule.web.MySchedule;
import myschedule.web.ui.FilteredHistoryTable.NoPluginException;

interface FilterTableImpl {

	Container.Filterable getContainer();

	void refresh() throws RemoteException, NoPluginException, SQLException;

	void removeFilter(String columnID);

	void addFilter(String columnID, Filter filter);

	static class HistoryRecordBean {

		private String eventName;

		private String eventTime;
		private String eventType;
		private String hostIP;
		private String info1;
		private String info2;
		private String info3;
		private String info4;
		private String info5;
		private String schedulerName;

		public HistoryRecordBean(String hostIP, String schedulerName,
				String eventType, String eventName, String eventTime,
				String info1, String info2, String info3, String info4,
				String info5) {
			this.hostIP = hostIP;
			this.schedulerName = schedulerName;
			this.eventType = eventType;
			this.eventName = eventName;
			this.eventTime = eventTime;
			this.info1 = info1;
			this.info2 = info2;
			this.info3 = info3;
			this.info4 = info4;
			this.info5 = info5;
		}

		public String getEventName() {
			return eventName;
		}

		public String getEventTime() {
			return eventTime;
		}

		public String getEventType() {
			return eventType;
		}

		public String getHostIP() {
			return hostIP;
		}

		public String getInfo1() {
			return info1;
		}

		public String getInfo2() {
			return info2;
		}

		public String getInfo3() {
			return info3;
		}

		public String getInfo4() {
			return info4;
		}

		public String getInfo5() {
			return info5;
		}

		public String getSchedulerName() {
			return schedulerName;
		}
	}

	// this class builds the filtering checkbox selections
	static class HistoryRecordListHolder {

		private List<HistoryRecordBean> actualBeans;

		private Set<String> eventNames;

		private Set<String> eventTypes;
		private Set<String> hostIPs;
		private Set<String> info2;
		private Set<String> schedulerNames;

		public HistoryRecordListHolder(List<HistoryRecordBean> actualBeans) {
			this.actualBeans = actualBeans;

			hostIPs = new HashSet<String>();
			schedulerNames = new HashSet<String>();
			eventTypes = new HashSet<String>();
			eventNames = new HashSet<String>();
			info2 = new HashSet<String>();
			for (HistoryRecordBean b : actualBeans) {
				eventNames.add(b.getEventName());
				eventTypes.add(b.getEventType());
				schedulerNames.add(b.getSchedulerName());
				hostIPs.add(b.getHostIP());
				info2.add(b.getInfo2());
			}
		}

		public List<HistoryRecordBean> getActualBeans() {
			return Collections.unmodifiableList(actualBeans);
		}

		public Set<String> getEventNames() {
			return Collections.unmodifiableSet(eventNames);
		}

		public Set<String> getEventTypes() {
			return Collections.unmodifiableSet(eventTypes);
		}

		public Set<String> getHostIPs() {
			return Collections.unmodifiableSet(hostIPs);
		}

		public Set<String> getInfo2() {
			return info2;
		}

		public Set<String> getSchedulerNames() {
			return Collections.unmodifiableSet(schedulerNames);
		}

	}

	HistoryRecordListHolder getCachedFiltersData()
			throws NoPluginException, RemoteException;

	class IndexContainerImpl implements FilterTableImpl {
		private HistoryRecordListHolder cachedTableData;
		Container.Filterable container;
		final String schedulerSettingsName;
		private Map<String, Filter> filtersCache;

		private void initIndexedContainer()
				throws NoPluginException, RemoteException {
			// defines the container's columns
			container = new IndexedContainer();
			String defaultValue = "";
			container.addContainerProperty(HOST_IP_NAME, String.class,
					defaultValue);
			container.addContainerProperty(SCHEDULER_NAME, String.class,
					defaultValue);
			container.addContainerProperty(EVENT_TYPE, String.class,
					defaultValue);
			container.addContainerProperty(EVENT_NAME, String.class,
					defaultValue);
			container.addContainerProperty(EVENT_TIME, String.class,
					defaultValue);
			container.addContainerProperty(INFO_1, String.class, defaultValue);
			container.addContainerProperty(INFO_2, String.class, defaultValue);
			container.addContainerProperty(INFO_3, String.class, defaultValue);
			container.addContainerProperty(INFO_4, String.class, defaultValue);
			container.addContainerProperty(INFO_5, String.class, defaultValue);

			loadDataIntoContainer();
		}

		IndexContainerImpl(String schedulerSettingsName)
				throws RemoteException, NoPluginException {
			super();
			this.schedulerSettingsName = schedulerSettingsName;
			filtersCache = new HashMap<>();
			initIndexedContainer();

		}

		@SuppressWarnings("unchecked")
		void loadDataIntoContainer() throws NoPluginException, RemoteException {
			HistoryRecordListHolder tableData = getCachedFiltersData();

			container.removeAllItems();

			container.removeAllContainerFilters();

			for (HistoryRecordBean bean : tableData.getActualBeans()) {
				Item addItem = container.getItem(container.addItem());
				if (addItem == null) {
					// Item is filtered by an existing filter.
					continue;
				}
				addItem.getItemProperty(HOST_IP_NAME)
						.setValue(bean.getHostIP());
				addItem.getItemProperty(SCHEDULER_NAME)
						.setValue(bean.getSchedulerName());
				addItem.getItemProperty(EVENT_TYPE)
						.setValue(bean.getEventType());
				addItem.getItemProperty(EVENT_NAME)
						.setValue(bean.getEventName());
				addItem.getItemProperty(EVENT_TIME)
						.setValue(bean.getEventTime());
				addItem.getItemProperty(INFO_1).setValue(bean.getInfo1());
				addItem.getItemProperty(INFO_2).setValue(bean.getInfo2());
				addItem.getItemProperty(INFO_3).setValue(bean.getInfo3());
				addItem.getItemProperty(INFO_4).setValue(bean.getInfo4());
				addItem.getItemProperty(INFO_5).setValue(bean.getInfo5());
			}
			filtersCache.entrySet()
					.forEach(e -> container.addContainerFilter(e.getValue()));
		}

		public HistoryRecordListHolder getCachedFiltersData()
				throws NoPluginException, RemoteException {

			if (cachedTableData == null) {

				List<List<Object>> histories = getDataFromJdbc();
				List<HistoryRecordBean> beans = new ArrayList<HistoryRecordBean>();
				for (List<Object> history : histories) {
					String ip = history.get(0) + "/" + history.get(1);
					String schedulerName = toStr(history.get(2));
					String eventType = toStr(history.get(3));
					String eventName = toStr(history.get(4));
					String eventTime = (history.get(5) instanceof Date)
							? toDateStr((Date) history.get(5), DATE_FORMAT)
							: toStr(history.get(5));
					String info1 = toStr(history.get(6));
					String info2 = toStr(history.get(7));
					String info3 = toStr(history.get(8));
					String info4 = toStr(history.get(9));
					// We updated info5 to clob, use the cStr from Tools
					String info5 = toolsCStr(history.get(10), true);

					beans.add(new HistoryRecordBean(ip, schedulerName,
							eventType, eventName, eventTime, info1, info2,
							info3, info4, info5));
				}

				cachedTableData = new HistoryRecordListHolder(beans);
			}
			return cachedTableData;
		}

		/**
		 * Copied from Tools.cStr we don't have core library imports here except
		 * return empty string instead of null
		 * 
		 * @param value
		 * @param trim
		 * @return
		 */
		private static String toolsCStr(Object value, boolean trim) {
			if (value != null) {

				String retValue = null;
				if (value instanceof java.sql.Clob) {
					Clob clob = (Clob) value;
					try {
						retValue = clob.getSubString(1, (int) clob.length());
					} catch (SQLException e) {
						throw new RuntimeException(e);
					}
				} else {
					retValue = value.toString();
				}

				if (trim)
					retValue = retValue.trim();

				return retValue;
			}
			return "";
		}

		private String toDateStr(Date date, SimpleDateFormat df) {
			if (date == null)
				return "";
			else
				return df.format(date);
		}

		private String toStr(Object item) {
			if (item == null)
				return "";
			else
				return "" + item;
		}

		@Override
		public Filterable getContainer() {
			return container;
		}

		@Override
		public void refresh() throws RemoteException, NoPluginException {
			this.cachedTableData = null;
			loadDataIntoContainer();
			// container.removeAllContainerFilters();
		}

		private List<List<Object>> getDataFromJdbc()
				throws NoPluginException, RemoteException {
			MySchedule mySchedule = MySchedule.getInstance();
			SchedulerTemplate scheduler = mySchedule
					.getScheduler(schedulerSettingsName);
			String key = mySchedule.getMyScheduleSettings()
					.getJdbcSchedulerHistoryPluginContextKey();
			JdbcSchedulerRemoteInterface plugin = (JdbcSchedulerRemoteInterface) scheduler
					.getContext().get(key);
			if (plugin == null) {
				String msg = "No JdbcSchedulerHistoryPlugin detected! Please configure this plugin to record scheduler "
						+ "events and job histories in your scheduler config settings.";
				throw new NoPluginException(msg);
			}

			List<List<Object>> histories = plugin.getJobHistoryData();
			return histories;
		}

		@Override
		public void removeFilter(String columnID) {
			container.getContainerFilters().stream()
					.filter(f -> f.appliesToProperty(columnID))
					.forEach(f -> container.removeContainerFilter(f));
			filtersCache.remove(columnID);
		}

		@Override
		public void addFilter(String columnID, Filter filter) {
			filtersCache.put(columnID, filter);
			container.addContainerFilter(filter);
		}

	}

	static class SQLFilterTableImpl implements FilterTableImpl {

		private static final String SCHEDULER_HISTORY = "SCHEDULER_HISTORY";
		private String tableName;
		private String schedulerSettingsName;
		private SQLContainer container;
		private JDBCConnectionPool connectionPool;
		private HistoryRecordListHolder cachedFiltersData;

		public SQLFilterTableImpl(String schedulerSettingsName)
				throws RemoteException, NoPluginException, SQLException {
			this.schedulerSettingsName = schedulerSettingsName;
			initSQLContainer();
		}

		private void initSQLContainer()
				throws NoPluginException, RemoteException, SQLException {
			// defines the container's columns
			JobStoreSupport js = QuartzClusterJobStatusService
					.getJobStoreSuppot(schedulerSettingsName);
			tableName = js.getTablePrefix() + SCHEDULER_HISTORY;
			this.connectionPool = new JDBCConnectionPool() {

				/**
				 * 
				 */
				private static final long serialVersionUID = 5706716298526547364L;

				@Override
				public Connection reserveConnection() throws SQLException {
					return DBConnectionManager.getInstance()
							.getConnection(js.getDataSource());
				}

				@Override
				public void releaseConnection(Connection conn) {
					if (conn != null) {
						try {
							conn.close();
						} catch (SQLException e) {
						}
					}
				}

				@Override
				public void destroy() {

				}
			};
			TableQuery table = new TableQuery(tableName, connectionPool,
					new MSSQLGenerator());
			final SQLContainer sqlContainer = new SQLContainer(table);
			sqlContainer.setPageLength(500);
			sqlContainer.addOrderBy(new OrderBy("EVENT_TIME", false));
			initFilters();
			container = sqlContainer;
		}

		void initFilters() throws SQLException {
			String filterValues = "Select DISTINCT HOST_IP, SCHEDULER_NAME, EVENT_TYPE, EVENT_NAME, INFO2 "
					+ "FROM " + tableName;
			try (Connection con = connectionPool.reserveConnection();
					Statement stmt = con.createStatement();) {
				final ResultSet rs = stmt.executeQuery(filterValues);
				List<HistoryRecordBean> beans = new ArrayList<HistoryRecordBean>();
				while (rs.next()) {
					String ip = rs.getString(1);
					String schedulerName = rs.getString(2);
					String eventType = rs.getString("EVENT_TYPE");
					String eventName = rs.getString("EVENT_NAME");
					String info2 = rs.getString("INFO2");

					beans.add(new HistoryRecordBean(ip, schedulerName,
							eventType, eventName, null, null, info2, null, null,
							null));
				}

				cachedFiltersData = new HistoryRecordListHolder(beans);
			}
		}

		@Override
		public Filterable getContainer() {
			return container;
		}

		@Override
		public void refresh() throws SQLException {
			container.refresh();
			initFilters();
		}

		@Override
		public HistoryRecordListHolder getCachedFiltersData() {
			return this.cachedFiltersData;
		}

		@Override
		public void removeFilter(String columnID) {
			container.getContainerFilters().stream()
					.filter(f -> f.appliesToProperty(columnID))
					.collect(toList())
					.forEach(f -> container.removeContainerFilter(f));
		}

		@Override
		public void addFilter(String columnID, Filter filter) {
			container.addContainerFilter(filter);
		}

	}

	public static FilterTableImpl getIndexTable(String schedulerSettingsName)
			throws RemoteException, NoPluginException {
		return new IndexContainerImpl(schedulerSettingsName);
	}

	public static FilterTableImpl getSQLImpl(String schedulerSettingsName)
			throws RemoteException, NoPluginException, SQLException {
		return new SQLFilterTableImpl(schedulerSettingsName);
	}
}
