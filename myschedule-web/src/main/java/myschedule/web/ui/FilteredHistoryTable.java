package myschedule.web.ui;

import java.rmi.RemoteException;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.data.Container;
import com.vaadin.data.Container.Filter;
import com.vaadin.data.Item;
import com.vaadin.data.Property;
import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.data.util.filter.Compare;
import com.vaadin.data.util.filter.Or;
import com.vaadin.event.FieldEvents.TextChangeEvent;
import com.vaadin.event.FieldEvents.TextChangeListener;
import com.vaadin.ui.AbstractLayout;
import com.vaadin.ui.Accordion;
import com.vaadin.ui.Button;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Notification;
import com.vaadin.ui.PopupDateField;
import com.vaadin.ui.Table;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;

import myschedule.web.ui.FilterTableImpl.HistoryRecordListHolder;

public class FilteredHistoryTable extends CustomComponent {
	class CheckboxGroup extends CustomComponent {

		private static final long serialVersionUID = -6762371787075654521L;

		private final Set<String> boxesNames;
		private List<CheckBox> checkboxes;
		private final String columnID;
		private boolean ignoreTriggers;

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
			selectAllBtn.addClickListener(event -> selectAllOrNone(true));

			Button selectNoneBtn = new Button("None");
			selectNoneBtn.addClickListener(event -> selectAllOrNone(false));

			layout.addComponent(
					new HorizontalLayout(selectAllBtn, selectNoneBtn));

			for (CheckBox checkbox : checkboxes) {
				checkbox.setValue(true);
				layout.addComponent(checkbox);
				checkbox.addValueChangeListener(event -> {
					if(ignoreTriggers) {
						// Used for the None and All buttons to avoid multiple triggers.
						return;
					}
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

				});
			}
			return this;

		}

		private void selectAllOrNone(boolean value) {
			try {
				ignoreTriggers = true;

				for (CheckBox cb : checkboxes) {
					cb.setValue(value);

				}
				updateFilters(columnID, "");
			} finally {
				ignoreTriggers = false;
			}
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

					removeContainerFilter(columnID);

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
					addContainerFilter(columnID, new Filter() {

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

	

	public static class NoPluginException extends Exception {

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

	static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat(
			"yyyy-MM-dd HH:mm:ss");

	static final String EVENT_NAME = "EVENT_NAME";

	static final String EVENT_TIME = "EVENT_TIME";

	static final String EVENT_TYPE = "EVENT_TYPE";

	static final String HOST_IP_NAME = "Host IP/Name";

	static final String INFO_1 = "INFO1";

	static final String INFO_2 = "INFO2";

	static final String INFO_3 = "INFO3";

	static final String INFO_4 = "INFO4";

	static final String INFO_5 = "INFO5";
	
	private static final Logger logger = LoggerFactory
			.getLogger(FilteredHistoryTable.class);

	static final String SCHEDULER_NAME = "Scheduler Name";

	private static final long serialVersionUID = 6894801051461901373L;

	

	

	private Container.Filterable container;

	private AbstractLayout filtersLayout;

	private final String schedulerSettingsName;

	private final Table table;

	

	private FilterTableImpl filterImpl;

	public FilteredHistoryTable(String schedulerSettingsName) throws RemoteException {
		this.schedulerSettingsName = schedulerSettingsName;
		table = new Table();

		try {
			
//			HorizontalLayout rootComponent = new HorizontalLayout();
			setCompositionRoot(table);
//			rootComponent.setSplitPosition(250, Unit.PIXELS);
//			rootComponent.setHeight(600, Unit.PIXELS);
			
			logger.info("init filterable history table for {}",
					schedulerSettingsName);

			this.filterImpl = FilterTableImpl.getSQLImpl(schedulerSettingsName);
			this.container = filterImpl.getContainer();
			// setup the contents for filter panel
			filtersLayout = new VerticalLayout();
			HistoryRecordListHolder holder = filterImpl.getCachedFiltersData();
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
			table.setHeight(100, UNITS_PERCENTAGE);

//			rootComponent.addComponent(filtersLayout);
//			rootComponent.addComponent(table);

			logger.info("finished init history table");
		} catch (NoPluginException e) {
			Notification.show("WARNING", e.getMessage(),
					Notification.Type.WARNING_MESSAGE);
		} catch (SQLException e) {
			Notification.show("WARNING", e.getMessage(),
					Notification.Type.WARNING_MESSAGE);
		}
	}

		
	
	
	


	public void refresh() throws RemoteException {
		try {
			filterImpl.refresh();
		} catch (RemoteException|NoPluginException|SQLException e) {
			Notification.show("WARNING", e.getMessage(),
					Notification.Type.WARNING_MESSAGE);
		}

	}


	private void updateFilters(final String columnID, String whileListValue) {

		removeContainerFilter(columnID);
		if (whileListValue == null) {
			// block everything
//			
			Filter filter = new Filter() {

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
			};
			addContainerFilter(columnID, filter);
			return;
		}

		// clear everything
		if ("".equals(whileListValue))
			return;

		final List<String> whileLists = Arrays
				.asList(whileListValue.split(","));
		Filter filter;
		if (whileLists.size() > 0) {
			filter = new Or(whileLists.stream()
					.map(value -> new Compare.Equal(columnID, value))
					.toArray(Filter[]::new));

		} else

			filter = new Compare.Equal(columnID, whileListValue);
		// new Filter() {
//
//			private static final long serialVersionUID = 2884978605273521704L;
//
//			@Override
//			public boolean appliesToProperty(Object propertyId) {
//				boolean equals = columnID.equals(propertyId);
//				return equals;
//			}
//
//			@Override
//			public boolean passesFilter(Object itemId, Item item)
//					throws UnsupportedOperationException {
//
//				// just do a contains ignore case filter
//				@SuppressWarnings("unchecked")
//				Property<String> itemProperty = item.getItemProperty(columnID);
//				if (itemProperty == null)
//					return false;
//				Object valObj = itemProperty.getValue();
//				String val = valObj == null ? ""
//						: valObj.toString().toLowerCase();
//
//				for (String string : whileLists) {
//					if (val.contains(string.toLowerCase()))
//						return true;
//				}
//				return false;
//			}
//		};
		addContainerFilter(columnID, filter);

	}

	private void removeContainerFilter(final String columnID) {
		filterImpl.removeFilter(columnID);
	}

	private void addContainerFilter(final String columnID, Filter filter) {
		filterImpl.addFilter(columnID, filter);
	}

	public AbstractLayout getFiltersLayout() {
		return filtersLayout;
	}
}