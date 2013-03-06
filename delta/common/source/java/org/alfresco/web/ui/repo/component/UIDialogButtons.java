/*
 * Copyright (C) 2005-2007 Alfresco Software Limited.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.

 * As a special exception to the terms and conditions of version 2.0 of 
 * the GPL, you may redistribute this Program in connection with Free/Libre 
 * and Open Source Software ("FLOSS") applications as described in Alfresco's 
 * FLOSS exception.  You should have recieved a copy of the text describing 
 * the FLOSS exception, and it is also available here: 
 * http://www.alfresco.com/legal/licensing"
 */
package org.alfresco.web.ui.repo.component;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import javax.faces.component.UICommand;
import javax.faces.component.UIComponent;
import javax.faces.component.UIOutput;
import javax.faces.component.html.HtmlCommandButton;
import javax.faces.context.FacesContext;
import javax.faces.el.MethodBinding;
import javax.faces.el.ValueBinding;

import org.alfresco.web.app.Application;
import org.alfresco.web.app.servlet.FacesHelper;
import org.alfresco.web.config.DialogsConfigElement.DialogButtonConfig;
import org.alfresco.web.ui.common.ComponentConstants;
import org.alfresco.web.ui.common.Utils;
import org.alfresco.web.ui.common.component.SelfRenderingComponent;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ee.webmedia.alfresco.utils.ComponentUtil;

/**
 * Component that displays the buttons for a dialog.
 * <p>
 * The standard <code>OK</code> and <code>Cancel</code> buttons are always
 * generated. Any additional buttons, either configured or generated dynamically
 * by the dialog, are generated in between the standard buttons.
 * 
 * @author gavinc
 */
public class UIDialogButtons extends SelfRenderingComponent {
	protected static final String BINDING_EXPRESSION_START = "#{";

	private static final Log logger = LogFactory.getLog(UIDialogButtons.class);

	@Override
	public String getFamily() {
		return "org.alfresco.faces.DialogButtons";
	}

	@Override
	public void encodeBegin(FacesContext context) throws IOException {
		if (!isRendered())
			return;

		// We need to refresh buttons on every reload (dynamic adding)
		this.getChildren().clear();
		UIComponent existingButtons = ComponentUtil.findComponentById(context,
				Utils.getParentForm(context, this), "finish-button");
		if (existingButtons == null) {
			existingButtons = ComponentUtil.findComponentById(context,
					Utils.getParentForm(context, this), "cancel-button");
		}
		generateButtons(context, (existingButtons != null));
	}

	@Override
	public void encodeChildren(FacesContext context) throws IOException {
		if (!isRendered())
			return;

		// render the buttons
		for (Iterator i = getChildren().iterator(); i.hasNext(); /**/) {
			UIComponent child = (UIComponent) i.next();
			Utils.encodeRecursive(context, child);
		}
	}

	@Override
	public boolean getRendersChildren() {
		return true;
	}

	/**
	 * Generates the buttons for the dialog currently being shown.
	 * 
	 * @param context
	 *            Faces context
	 * @param secondRendering
	 *            true, if buttons are rendered second time
	 */
	@SuppressWarnings("unchecked")
	protected void generateButtons(FacesContext context, boolean secondRendering) {
		// generate the OK button, if necessary
		if (Application
				.getDialogManager()
				.getBean()
				.isFinishButtonVisible(
						Application.getDialogManager().isOKButtonVisible())) {
			UICommand okButton = (UICommand) context.getApplication()
					.createComponent(HtmlCommandButton.COMPONENT_TYPE);
			okButton.setRendererType(ComponentConstants.JAVAX_FACES_BUTTON);

			String okButtonId = secondRendering ? "finish-button-2"
					: "finish-button";
			FacesHelper.setupComponentId(context, okButton, okButtonId);

			// create the binding for the finish button label
			ValueBinding valueBinding = context.getApplication()
					.createValueBinding("#{DialogManager.finishButtonLabel}");
			okButton.setValueBinding("value", valueBinding);

			// create the action binding
			MethodBinding methodBinding = context.getApplication()
					.createMethodBinding("#{DialogManager.finish}", null);
			okButton.setAction(methodBinding);

			// setup CSS class for button
			StringBuilder sb = new StringBuilder(StringUtils.defaultIfEmpty(
					(String) this.getAttributes().get("styleClass"), ""));
			sb.append(" dialog-button primary");
			if (!secondRendering) {
				sb.append(" defaultAction");
			}
			okButton.getAttributes().put("styleClass", sb.toString());

			// add the OK button
			this.getChildren().add(okButton);

			addSpacingRow(context);
		}

		// generate the additional buttons
		generateAdditionalButtons(context, secondRendering);

		// generate the OK button
		UICommand cancelButton = (UICommand) context.getApplication()
				.createComponent(HtmlCommandButton.COMPONENT_TYPE);
		cancelButton.setRendererType(ComponentConstants.JAVAX_FACES_BUTTON);

		String cancelButtonId = secondRendering ? "cancel-button-2"
				: "cancel-button";
		FacesHelper.setupComponentId(context, cancelButton, cancelButtonId);

		// create the binding for the cancel button label
		ValueBinding valueBinding = context.getApplication()
				.createValueBinding("#{DialogManager.cancelButtonLabel}");
		cancelButton.setValueBinding("value", valueBinding);

		// create the action binding
		MethodBinding methodBinding = context.getApplication()
				.createMethodBinding("#{DialogManager.cancel}", null);
		cancelButton.setAction(methodBinding);

		// setup CSS class for button
		String styleClass = (String) this.getAttributes().get("styleClass");
		styleClass = (styleClass == null) ? "dialog-button cancel" : styleClass
				+ " dialog-button cancel";
		cancelButton.getAttributes().put("styleClass", styleClass);

		// set the immediate flag to true
		cancelButton.getAttributes().put("immediate", Boolean.TRUE);

		// add the Cancel button
		this.getChildren().add(cancelButton);
	}

	/**
	 * If there are any additional buttons to add as defined by the dialog
	 * configuration and the dialog at runtime they are generated in this
	 * method.
	 * 
	 * @param context
	 *            Faces context
	 */
	@SuppressWarnings("unchecked")
	protected void generateAdditionalButtons(FacesContext context,
			boolean secondRendering) {
		// get potential list of additional buttons
		List<DialogButtonConfig> buttons = Application.getDialogManager()
				.getAdditionalButtons();

		if (buttons != null && buttons.size() > 0) {
			if (logger.isDebugEnabled())
				logger.debug("Adding " + buttons.size()
						+ " additional buttons: " + buttons);

			for (DialogButtonConfig buttonCfg : buttons) {
				addSpacingRow(context);

				UICommand button = (UICommand) context.getApplication()
						.createComponent(HtmlCommandButton.COMPONENT_TYPE);
				button.setRendererType(ComponentConstants.JAVAX_FACES_BUTTON);

				String buttonId = secondRendering ? buttonCfg.getId() + "-2"
						: buttonCfg.getId();
				FacesHelper.setupComponentId(context, button, buttonId);

				// setup the value of the button (the label)
				String label = buttonCfg.getLabel();
				if (label != null) {
					// see if the label represents a value binding
					if (label.startsWith(BINDING_EXPRESSION_START)) {
						ValueBinding binding = context.getApplication()
								.createValueBinding(label);
						button.setValueBinding("value", binding);
					} else {
						button.setValue(label);
					}
				} else {
					// NOTE: the config checks that a label or a label id
					// is present so we can assume there is an id
					// if there isn't a label
					String labelId = buttonCfg.getLabelId();
					label = Application.getMessage(context, labelId);
					button.setValue(label);
				}

				// setup the action binding, the config checks that an action
				// is present so no need to check for NullPointer. It also
				// checks
				// it represents a method binding expression.
				String action = buttonCfg.getAction();
				MethodBinding methodBinding = context.getApplication()
						.createMethodBinding(action, null);
				button.setAction(methodBinding);

				// setup the disabled attribute, check for null and
				// binding expressions
				String disabled = buttonCfg.getDisabled();
				if (disabled != null && disabled.length() > 0) {
					if (disabled.startsWith(BINDING_EXPRESSION_START)) {
						ValueBinding binding = context.getApplication()
								.createValueBinding(disabled);
						button.setValueBinding("disabled", binding);
					} else {
						button.getAttributes().put("disabled",
								Boolean.parseBoolean(disabled));
					}
				}

				// setup CSS class for the button
				String styleClass = (String) this.getAttributes().get(
						"styleClass");
				styleClass = (styleClass == null) ? "dialog-button additional"
						: styleClass + " dialog-button additional";
				if (styleClass != null) {
					button.getAttributes().put("styleClass", styleClass);
				}

				// setup the onclick handler for the button
				String onclick = buttonCfg.getOnclick();
				if (onclick != null && onclick.length() > 0) {
					button.getAttributes().put("onclick", onclick);
				}

				// add the button
				// Kommenteerime lisanuppude lisamise välja
				//this.getChildren().add(button);

				if (logger.isDebugEnabled())
					logger.debug("Added button with id of: " + button.getId());
			}

			// add a spacing row to separate the additional buttons from the
			// Cancel button
			addSpacingRow(context);
		}
	}

	/**
	 * Creates an output text component to represent a spacing row.
	 * 
	 * @param context
	 *            Faces context
	 */
	@SuppressWarnings("unchecked")
	protected void addSpacingRow(FacesContext context) {
		UIOutput spacingRow = (UIOutput) context.getApplication()
				.createComponent(ComponentConstants.JAVAX_FACES_OUTPUT);
		spacingRow.setRendererType(ComponentConstants.JAVAX_FACES_TEXT);
		FacesHelper.setupComponentId(context, spacingRow, null);
		spacingRow.setValue(""); // NOTE: used to be <br />
		spacingRow.getAttributes().put("escape", Boolean.FALSE);
		this.getChildren().add(spacingRow);
	}

}
