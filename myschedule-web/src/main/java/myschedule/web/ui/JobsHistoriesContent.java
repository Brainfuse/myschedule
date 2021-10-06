package myschedule.web.ui;

import java.rmi.RemoteException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.ui.Button;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.PopupView;
import com.vaadin.ui.UI;

import myschedule.web.MySchedule;

/**s
 * JobsHistoriesContent provide a table view for Job histories recorded by JdbcSchedulerHistoryPlugin.
 * User: Zemian Deng
 * Date: 6/1/13
 */
public class JobsHistoriesContent extends FullSizeVerticalLayout {
    private static final Logger LOGGER = LoggerFactory.getLogger(JobsHistoriesContent.class);
    MySchedule mySchedule = MySchedule.getInstance();
    MyScheduleUi myScheduleUi;
    String schedulerSettingsName;
    HorizontalLayout toolbar;
    
    private FilteredHistoryTable filteredTable;
	private PopupView filtersPopup;

    public JobsHistoriesContent(MyScheduleUi myScheduleUi, String schedulerSettingsName) throws RemoteException {
        this.myScheduleUi = myScheduleUi;
        this.schedulerSettingsName = schedulerSettingsName;
        setId("jobHistoryContent");
        addStyleName("jobHistoryContent");
        
        filteredTable = new FilteredHistoryTable(schedulerSettingsName);
        initToolbar();
        addContent(filteredTable);
    }

    private void initToolbar() {
        toolbar = new HorizontalLayout();
        addComponent(toolbar);
        filtersPopup = new PopupView("", filteredTable.getFiltersLayout());
        filtersPopup.setHideOnMouseOut(false);
        filtersPopup.addStyleName("jobHistoryContentFilter");
        
        UI.getCurrent().getPage().getStyles().add(".v-popupview-popup-jobHistoryContentFilter.v-popupview-popup {max-height: 100vh; max-width: 30vw; overflow:hidden !important;}"
        		+ ".v-popupview-popup-jobHistoryContentFilter.v-popupview-popup > .popupContent > .v-verticallayout{max-height:100vh;overflow: auto}");
		toolbar.addComponent(createFiltersButton());
        toolbar.addComponent(filtersPopup);
        toolbar.addComponent(createRefreshButton());
    }
   
    private Button createFiltersButton() {
        Button button = new Button("Filters");
        button.addClickListener(new Button.ClickListener() {
            @Override
			public void buttonClick(Button.ClickEvent event) {
				filtersPopup.setPopupVisible(true);
			}
        });
        return button;
    }

    private Button createRefreshButton() {
        Button button = new Button("Refresh");
        button.addClickListener(new Button.ClickListener() {
            @Override
            public void buttonClick(Button.ClickEvent event) {
                try {
					filteredTable.refresh();
				} catch (RemoteException e) {
					throw new RuntimeException(e);
				}
            }
        });
        return button;
    }


    private String toStr(Object item) {
        if (item == null)
            return "";
        else
            return "" + item;
    }

    private String toDateStr(Date date, SimpleDateFormat df) {
        if (date == null)
            return "";
        else
            return df.format(date);
    }
}