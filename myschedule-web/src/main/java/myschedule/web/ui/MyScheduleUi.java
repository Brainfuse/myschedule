package myschedule.web.ui;

import static java.util.stream.Collectors.joining;

import com.vaadin.annotations.Widgetset;
import com.vaadin.server.VaadinRequest;
import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Component;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;

import myschedule.web.MySchedule;

/**
 * The MySchedule UI application entry point. This UI holds the main application and reference to top level UI
 * components. It also expose methods to change and load different UI screens based on user's actions. The top level
 * components of this application would only consist of a bread crumb like navigation bar and a user screen that
 * can be changed/loaded depending on user's action. The default user screen is the DashboardScreen view.
 */
@Widgetset("myschedule.web.ui.MyScheduleUiWidgetSet")
public class MyScheduleUi extends UI {
    private static final long serialVersionUID = 1L;
    private VerticalLayout content;
    private BreadcrumbBar breadcrumbBar;
    private Component currentScreen;
    private HorizontalLayout currentScreenWrapper;

    @Override
    protected void init(VaadinRequest vaadinRequest) {
        // Setup main page
        getPage().setTitle("MySchedule - Quartz Scheduler Manager");

        // Create components
        content = new VerticalLayout();
        content.setSizeFull();
        breadcrumbBar = new BreadcrumbBar(this);
        currentScreen = new DashboardScreen(this);

//        HorizontalLayout headerContent = createHeader();
        HorizontalLayout footerContent = creatFooter();

        // We needed a wrapper to remain in place so when refreshing currentScreen, the footer won't get messed up.
        currentScreenWrapper = new HorizontalLayout();
        currentScreenWrapper.setSizeFull();
        currentScreenWrapper.addComponent(currentScreen);

        // Setup content
        content.setImmediate(true);
        content.setMargin(true);
//        content.addComponent(headerContent);
//        content.setComponentAlignment(headerContent, Alignment.TOP_CENTER);
        content.addComponent(breadcrumbBar);
        content.setExpandRatio(breadcrumbBar, 0);
        content.addComponent(currentScreenWrapper);
        content.setComponentAlignment(currentScreenWrapper, Alignment.MIDDLE_CENTER);
        content.setExpandRatio(currentScreenWrapper, 1);
        content.addComponent(footerContent);
        content.setComponentAlignment(footerContent, Alignment.BOTTOM_RIGHT);
        content.setExpandRatio(footerContent, 0);
        setContent(content);
    }

    private HorizontalLayout createHeader() {
        Label headerLabel = new Label("<h1>MySchedule - Quartz Scheduler Manager</h1>", ContentMode.HTML);

        HorizontalLayout result = new HorizontalLayout();
        result.addComponent(headerLabel);
        return result;
    }

	private HorizontalLayout creatFooter() {
		MySchedule mySchedule = MySchedule.getInstance();
		String versions = mySchedule.getVersions().entrySet().stream()
				.map(e -> e.getKey() + "-" + e.getValue())
				.collect(joining(", "));
        

        String poweredByText = "Powered by " + versions;
        Label poweredByLabel = new Label(poweredByText, ContentMode.PREFORMATTED);

        HorizontalLayout result = new HorizontalLayout();
        result.addComponent(poweredByLabel);
        return result;
    }

    void loadSchedulerScreen(String schedulerSettingsName) {
        currentScreenWrapper.removeComponent(currentScreen);
        currentScreen = new SchedulerScreen(this, schedulerSettingsName);
        currentScreenWrapper.addComponent(currentScreen);

        MySchedule mySchedule = MySchedule.getInstance();
        String schedulerFullName = mySchedule.getSchedulerSettings(schedulerSettingsName).getSchedulerFullName();
        breadcrumbBar.addSchedulerCrumb(schedulerFullName, schedulerSettingsName);
    }

    void loadDashboardScreen() {
        currentScreenWrapper.removeComponent(currentScreen);
        currentScreen = new DashboardScreen(this);
        currentScreenWrapper.addComponent(currentScreen);

        breadcrumbBar.removeSchedulerCrumb();
    }
}
