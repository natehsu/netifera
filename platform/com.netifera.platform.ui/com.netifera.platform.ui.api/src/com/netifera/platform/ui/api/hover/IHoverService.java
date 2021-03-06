package com.netifera.platform.ui.api.hover;

import java.util.List;

import org.eclipse.jface.action.IAction;

public interface IHoverService {
	String getInformation(Object o);
	List<IAction> getActions(Object o);
	List<IAction> getQuickActions(Object o);
}
