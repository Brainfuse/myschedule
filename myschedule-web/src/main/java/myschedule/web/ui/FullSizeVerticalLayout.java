package myschedule.web.ui;

import com.vaadin.ui.Component;
import com.vaadin.ui.VerticalLayout;

public class FullSizeVerticalLayout extends VerticalLayout {

	/**
	 * 
	 */
	private static final long serialVersionUID = -3755785671346461428L;

	public FullSizeVerticalLayout() {
		super();
		setSizeFull();
	}

	public FullSizeVerticalLayout(Component... children) {
		super(children);
		setSizeFull();
	}
	
	protected void addContent(Component content) {
		addComponent(content);
		setExpandRatio(content, 1f);
	}

}
