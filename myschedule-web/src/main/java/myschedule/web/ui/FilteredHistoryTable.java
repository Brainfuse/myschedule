package myschedule.web.ui;

import java.rmi.RemoteException;
import java.sql.Clob;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.data.Container.Filter;
import com.vaadin.data.Item;
import com.vaadin.data.Property;
import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.data.util.IndexedContainer;
import com.vaadin.event.FieldEvents.TextChangeEvent;
import com.vaadin.event.FieldEvents.TextChangeListener;
import com.vaadin.ui.AbstractLayout;
import com.vaadin.ui.Accordion;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.HorizontalSplitPanel;
import com.vaadin.ui.Label;
import com.vaadin.ui.Notification;
import com.vaadin.ui.PopupDateField;
import com.vaadin.ui.Table;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;

import myschedule.quartz.extra.JdbcSchedulerRemoteInterface;
import myschedule.quartz.extra.SchedulerTemplate;
import myschedule.web.MySchedule;

public class FilteredHistoryTable extends CustomComponent {
	class CheckboxGroup extends CustomComponent {

		private static final long serialVersionUID = -6762371787075654521L;

		private final Set<String> boxesNames;
		private List<CheckBox> checkboxes;
		private final String columnID;

		public CheckboxGroup(final String groupName,
				final Set<String> boxesNames) {

			this.checkboxes = new ArrayList<CheckBox>();
			this.columnID = groupName;
			this.boxesNames = boxesNames;
		}

		public CheckboxGroup init() {

			VerticalLayout layout = new VerticalLayout();
			layout.setWidth(100, Unit.PERCENTAGE);

			setCompositionRoot(layout);

			if (boxesNames == null || boxesNames.isEmpty()) {
				return this;
			}

			for (String name : boxesNames) {
				if (name == null)
					continue;
				CheckBox cb = new CheckBox(name);
				cb.setValue(true);// default checked
				checkboxes.add(cb);
			}

			Button selectAllBtn = new Button("All");
			selectAllBtn.addClickListener(new ClickListener() {

				private static final long serialVersionUID = -6815756828312138720L;

				@Override
				public void buttonClick(ClickEvent event) {
					for (CheckBox cb : checkboxes) {
						cb.setValue(true);

					}
					updateFilters(columnID, "");
				}
			});

			Button selectNoneBtn = new Button("None");
			selectNoneBtn.addClickListener(new ClickListener() {

				private static final long serialVersionUID = -1884288624398676463L;

				@Override
				public void buttonClick(ClickEvent event) {
					for (CheckBox cb : checkboxes) {
						cb.setValue(false);

					}
					updateFilters(columnID, null);
				}
			});

			layout.addComponent(
					new HorizontalLayout(selectAllBtn, selectNoneBtn));

			for (CheckBox checkbox : checkboxes) {
				checkbox.setValue(true);
				layout.addComponent(checkbox);
				checkbox.addValueChangeListener(new ValueChangeListener() {

					private static final long serialVersionUID = 1279363297795264292L;

					@Override
					public void valueChange(ValueChangeEvent event) {

						List<String> checked = new ArrayList<String>();
						for (CheckBox b : checkboxes) {
							if (b.getValue()) {
								checked.add(b.getCaption());
							}
						}

						if (checked.isEmpty()) {
							updateFilters(columnID, null);
						} else {
							final String whiteListValues = String.join(",",
									checked);
							updateFilters(columnID, whiteListValues);
						}

					}
				});
			}
			return this;

		}

	}

	private class DateRangeField extends CustomComponent {

		private static final long serialVersionUID = -7512807717987551605L;
		private final String columnID;
		private final PopupDateField end;
		private final PopupDateField start;

		public DateRangeField(final String columnID) {
			this.columnID = columnID;
			this.start = new PopupDateField();
			this.end = new PopupDateField();
		}

		public DateRangeField init() {
			start.setValue(new Date());
			end.setValue(new Date());
			start.setImmediate(true);
			end.setImmediate(true);

			ValueChangeListener listener = new ValueChangeListener() {

				private static final long serialVersionUID = 4175578457933523187L;

				@Override
				public void valueChange(ValueChangeEvent event) {

					container.removeContainerFilters(columnID);

					Date inputStart = start.getValue() == null ? new Date()
							: start.getValue();
					Date inputEnd = end.getValue() == null ? new Date()
							: end.getValue();
					final Date filterStart = inputStart.before(inputEnd)
							? inputStart : inputEnd;
					final Date filterEnd = inputStart.before(inputEnd)
							? inputEnd : inputStart;
					logger.debug("filter dates {} ~ {}", filterStart,
							filterEnd);
					container.addContainerFilter(new Filter() {

						private static final long serialVersionUID = 1691912829845711349L;

						@Override
						public boolean appliesToProperty(Object propertyId) {
							return columnID.equals(propertyId);
						}

						@Override
						public boolean passesFilter(Object itemId, Item item)
								throws UnsupportedOperationException {

							@SuppressWarnings("rawtypes")
							Property itemProperty = item
									.getItemProperty(columnID);

							if (itemProperty == null)
								return false;
							if (itemProperty.getValue() == null)
								return false;
							String s = itemProperty.getValue().toString();

							try {
								Date dataDate = DATE_FORMAT.parse(s);
								if (dataDate.before(filterEnd)
										&& dataDate.after(filterStart)) {
									return true;
								} else {
									return false;
								}
							} catch (ParseException e) {
								logger.error("", e);
								return true;
							}

						}

					});

				}
			};
			start.addValueChangeListener(listener);

			end.addValueChangeListener(listener);

			HorizontalLayout layout = new HorizontalLayout();
			setCompositionRoot(layout);
			layout.addComponent(start);
			layout.addComponent(new Label("-"));
			layout.addComponent(end);

			return this;
		}
	}

	private static class HistoryRecordBean {

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

	private static class NoPluginException extends Exception {

		private static final long serialVersionUID = 7330834006797104283L;

		public NoPluginException(String msg) {
			super(msg);
		}
	}

	private class TextContainsFilter extends CustomComponent {

		private static final long serialVersionUID = -3275730697419451781L;

		private final String columnID;

		private final TextField textField;

		public TextContainsFilter(final String columnID) {
			this.columnID = columnID;
			this.textField = new TextField();
		}

		public TextContainsFilter init() {
			textField.setWidth(100, Unit.PERCENTAGE);
			setCompositionRoot(textField);

			textField.addTextChangeListener(new TextChangeListener() {

				private static final long serialVersionUID = 8562124832221536802L;

				@Override
				public void textChange(TextChangeEvent event) {
					updateFilters(columnID, event.getText());
				}
			});
			return this;
		}

	}

	private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat(
			"yyyy-MM-dd HH:mm:ss");

	static final String EVENT_NAME = "Event Name";

	static final String EVENT_TIME = "Event Time";

	static final String EVENT_TYPE = "Event Type";

	static final String HOST_IP_NAME = "Host IP/Name";

	static final String INFO_1 = "Info 1";

	static final String INFO_2 = "Info 2";

	static final String INFO_3 = "Info 3";

	static final String INFO_4 = "Info 4";

	static final String INFO_5 = "Info 5";

	private static final Logger logger = LoggerFactory
			.getLogger(FilteredHistoryTable.class);

	static final String SCHEDULER_NAME = "Scheduler Name";

	private static final long serialVersionUID = 6894801051461901373L;

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

	private HistoryRecordListHolder cachedTableData;

	private IndexedContainer container;

	private AbstractLayout filtersLayout;

	private final String schedulerSettingsName;

	private final Table table;

	public FilteredHistoryTable(String schedulerSettingsName) throws RemoteException {
		this.schedulerSettingsName = schedulerSettingsName;
		container = new IndexedContainer();
		table = new Table();

		try {
			
			HorizontalSplitPanel rootComponent = new HorizontalSplitPanel();
			setCompositionRoot(rootComponent);
			rootComponent.setSplitPosition(250, Unit.PIXELS);
			rootComponent.setHeight(600, Unit.PIXELS);
			
			logger.info("init filterable history table for {}",
					schedulerSettingsName);

			// defines the container's columns
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

			// setup the contents for filter panel
			filtersLayout = new VerticalLayout();
			HistoryRecordListHolder holder = getCachedTableData();
			Accordion accordion = new Accordion();
			accordion.addTab(
					new CheckboxGroup(HOST_IP_NAME, holder.getHostIPs()).init(),
					HOST_IP_NAME);
			accordion.addTab(new CheckboxGroup(SCHEDULER_NAME,
					holder.getSchedulerNames()).init(), SCHEDULER_NAME);
			accordion.addTab(
					new CheckboxGroup(EVENT_TYPE, holder.getEventTypes())
							.init(),
					EVENT_TYPE);
			accordion.addTab(
					new CheckboxGroup(EVENT_NAME, holder.getEventNames())
							.init(),
					EVENT_NAME);

			accordion.addTab(new DateRangeField(EVENT_TIME).init(), EVENT_TIME);
			
			//info 1 is the job key
			accordion.addTab(
					new TextContainsFilter(INFO_1).init(),
					INFO_1);
			//info 2 is the job name
			accordion.addTab(new CheckboxGroup(INFO_2, holder.getInfo2()).init(), INFO_2);
			accordion.addTab(new TextContainsFilter(INFO_3).init(), INFO_3);
			accordion.addTab(new TextContainsFilter(INFO_4).init(), INFO_4);
			accordion.addTab(new TextContainsFilter(INFO_5).init(), INFO_5);
			
			//default to open the event time tab
			accordion.setSelectedTab(4);
			filtersLayout.addComponent(accordion);

			// set the the tables to display actual data
			table.setSizeFull();
			table.setImmediate(true);
			table.setSelectable(true);
			table.setContainerDataSource(container);

			table.setColumnWidth(INFO_5, 600);

//			rootComponent.addComponent(filtersLayout);
			rootComponent.addComponent(table);

			logger.info("finished init history table");
		} catch (NoPluginException e) {
			Notification.show("WARNING", e.getMessage(),
					Notification.Type.WARNING_MESSAGE);
		}
	}
	
	HistoryRecordListHolder getCachedTableData()
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
				//We updated info5 to clob, use the cStr from Tools
				String info5 = toolsCStr(history.get(10),true);

				beans.add(new HistoryRecordBean(ip, schedulerName, eventType,
						eventName, eventTime, info1, info2, info3, info4,
						info5));
			}

			cachedTableData = new HistoryRecordListHolder(beans);
		}
		return cachedTableData;
	}


	private List<List<Object>> getDataFromJdbc() throws NoPluginException, RemoteException {
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

	@SuppressWarnings("unchecked")
	void loadDataIntoContainer() throws NoPluginException, RemoteException {
		HistoryRecordListHolder tableData = getCachedTableData();

		container.removeAllItems();
		container.removeAllContainerFilters();
		for (HistoryRecordBean bean : tableData.getActualBeans()) {
			Item addItem = container.getItem(container.addItem());
			addItem.getItemProperty(HOST_IP_NAME).setValue(bean.getHostIP());
			addItem.getItemProperty(SCHEDULER_NAME)
					.setValue(bean.getSchedulerName());
			addItem.getItemProperty(EVENT_TYPE).setValue(bean.getEventType());
			addItem.getItemProperty(EVENT_NAME).setValue(bean.getEventName());
			addItem.getItemProperty(EVENT_TIME).setValue(bean.getEventTime());
			addItem.getItemProperty(INFO_1).setValue(bean.getInfo1());
			addItem.getItemProperty(INFO_2).setValue(bean.getInfo2());
			addItem.getItemProperty(INFO_3).setValue(bean.getInfo3());
			addItem.getItemProperty(INFO_4).setValue(bean.getInfo4());
			addItem.getItemProperty(INFO_5).setValue(bean.getInfo5());
		}
	}

	public void refresh() throws RemoteException {
		try {
			this.cachedTableData = null;
			loadDataIntoContainer();
//			container.removeAllContainerFilters();
		} catch (NoPluginException e) {
			Notification.show("WARNING", e.getMessage(),
					Notification.Type.WARNING_MESSAGE);
		}

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

	private void updateFilters(final String columnID, String whileListValue) {

		container.removeContainerFilters(columnID);

		if (whileListValue == null) {
			// block everything
			container.addContainerFilter(new Filter() {

				private static final long serialVersionUID = 3249884161445620906L;

				@Override
				public boolean appliesToProperty(Object propertyId) {
					return true;
				}

				@Override
				public boolean passesFilter(Object itemId, Item item)
						throws UnsupportedOperationException {
					return false;
				}
			});
			return;
		}

		// clear everything
		if ("".equals(whileListValue))
			return;

		final List<String> whileLists = new ArrayList<String>();
		if (whileListValue.contains(",")) {
			for (String w : whileListValue.split(",")) {
				whileLists.add(w);
			}
		} else {
			whileLists.add(whileListValue);
		}

		container.addContainerFilter(new Filter() {

			private static final long serialVersionUID = 2884978605273521704L;

			@Override
			public boolean appliesToProperty(Object propertyId) {
				boolean equals = columnID.equals(propertyId);
				return equals;
			}

			@Override
			public boolean passesFilter(Object itemId, Item item)
					throws UnsupportedOperationException {

				// just do a contains ignore case filter
				@SuppressWarnings("unchecked")
				Property<String> itemProperty = item.getItemProperty(columnID);
				if (itemProperty == null)
					return false;

				Object valObj = itemProperty.getValue();
				String val = valObj == null ? ""
						: valObj.toString().toLowerCase();

				for (String string : whileLists) {
					if (val.contains(string.toLowerCase()))
						return true;
				}
				return false;
			}
		});

	}

	public AbstractLayout getFiltersLayout() {
		return filtersLayout;
	}
}