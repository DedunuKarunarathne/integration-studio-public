/*
 * Copyright 2009-2016 WSO2, Inc. (http://wso2.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.wso2.integrationstudio.esb.form.editors.article.providers;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.eclipse.emf.transaction.RecordingCommand;
import org.eclipse.emf.transaction.TransactionalEditingDomain;
import org.eclipse.emf.transaction.util.TransactionUtil;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.wso2.integrationstudio.capp.core.artifacts.manager.CAppEnvironment;
import org.wso2.integrationstudio.capp.core.model.RegistryConnection;
import org.wso2.integrationstudio.esb.core.interfaces.IEsbEndpoint;
import org.wso2.integrationstudio.esb.core.interfaces.IEsbLocalEntry;
import org.wso2.integrationstudio.esb.core.interfaces.IEsbSequence;
import org.wso2.integrationstudio.gmf.esb.RegistryKeyProperty;
import org.wso2.integrationstudio.gmf.esb.impl.RegistryKeyPropertyImpl;
import org.wso2.integrationstudio.registry.core.interfaces.IRegistryConnection;
import org.wso2.integrationstudio.registry.core.interfaces.IRegistryData;
import org.wso2.integrationstudio.registry.core.interfaces.IRegistryHandler;
import org.wso2.integrationstudio.registry.core.interfaces.IRegistryFile;

/**
 * A SWT based editor dialog to be used for editing registry key properties.
 */
public class RegistryKeyPropertyEditorDialog extends Dialog {
	
	
	/**
	 * Default registry url.
	 */
	private static final String DEFAULT_REGISTRY_URL = "https://localhost:9443/registry";
	
	
	/**
	 * Prefix for governance registry paths.
	 */
	private static final String G_REG_PATH_PREFIX = "/_system/governance/";
	
	/**
	 * Prefix for configuration registry paths.
	 */
	private static final String C_REG_PATH_PREFIX = "/_system/config/";
	
	/**
	 * Registry key text field.
	 */
	private Text rkTextField;
	
	/**
	 * Registry key property being edited.
	 */
	private RegistryKeyProperty rkProperty;
	
	private String value;
	
	String title;
	

	/**
	 * Local named entities.
	 */
    private java.util.List<NamedEntityDescriptor> localNamedEntities;
	private FormData rkTextFieldLayoutData;
	
//	/**
//	 * Status indicating whether this dialog was saved or cancelled.
//	 */
//	private boolean saved;		

	/**
	 * Creates a new {@link RegistryKeyPropertyEditorDialog} instance.
	 * 
	 * @param parent
	 *            parent shell.
	 * @param style
	 *            style.
	 * @param property
	 *            {@link RegistryKeyProperty} to be edited.
	 * @param localNamedEntities
	 *            named entities currently defined on the local configuration.
	 */
	public RegistryKeyPropertyEditorDialog(Shell parent, int style, RegistryKeyProperty property,
			java.util.List<NamedEntityDescriptor> localNamedEntities) {
		super(parent);
		this.rkProperty = property;
		this.localNamedEntities = localNamedEntities;
	}

	public RegistryKeyPropertyEditorDialog(Shell parent, int style, RegistryKeyProperty property,
			java.util.List<NamedEntityDescriptor> localNamedEntities, String value) {
		super(parent);
		this.rkProperty = property;
		this.localNamedEntities = localNamedEntities;
		this.value = value;
	}

	public void setTitle(String title) {
		this.title = title;
	}
	
	@Override
	protected void configureShell(Shell newShell) {
		// TODO Auto-generated method stub
		super.configureShell(newShell);
		if (title != null) {
			newShell.setText(title);
		}
	}
	
	private void hide(){
		changeVisibility(false);
	}
	
	private void show(){
		changeVisibility(true);
	}

	private void changeVisibility(boolean visible) {
		try {
			this.getShell().setVisible(visible);
		} catch (Exception e) {
			//ignore this
		}
	}
	
	protected Control createDialogArea(Composite parent) {
		Composite container = (Composite) super.createDialogArea(parent);
		FillLayout fl_container = new FillLayout(SWT.HORIZONTAL);
		fl_container.marginHeight = 5;
		fl_container.marginWidth = 5;
		fl_container.spacing = 10;
		container.setLayout(fl_container);
		
		Group grpPropertyKey = new Group(container,SWT.None);
		grpPropertyKey.setText("Resource Key");
		
		FormLayout fl_grpPropertyKey = new FormLayout();
		fl_grpPropertyKey.marginHeight = 10;
		fl_grpPropertyKey.marginWidth = 10;
		grpPropertyKey.setLayout(fl_grpPropertyKey);
		
		// Registry key text field.
		rkTextField = new Text(grpPropertyKey, SWT.BORDER);
		if(StringUtils.isNotEmpty(value)){
			rkTextField.setText(value);
		}
		rkTextField.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				saveConfiguration();
			}
		});
		{
			rkTextFieldLayoutData = new FormData();
			rkTextFieldLayoutData.right = new FormAttachment(100, -8);
			rkTextField.setLayoutData(rkTextFieldLayoutData);			
		}
		
		Link link = new Link(grpPropertyKey, SWT.NONE);
		link.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				String selectedCommand = e.text;
				if (selectedCommand.equals("registry")){
					openRegistryBrowser();
				}else if (selectedCommand.equals("workspace")){
					openRegistryResourceProviderDialog();
				}else if (selectedCommand.equals("local entries")){
					openEmbeddedEntryBrowser();
				}
			}
		});
		rkTextFieldLayoutData.top = new FormAttachment(link, 7);
		rkTextFieldLayoutData.left = new FormAttachment(link, 0, SWT.LEFT);
		FormData fd_link = new FormData();
		fd_link.right = new FormAttachment(100, -4);
		fd_link.left = new FormAttachment(0);
		fd_link.top = new FormAttachment(0, -2);
		fd_link.bottom = new FormAttachment(0, 25);
		link.setLayoutData(fd_link);

		// Fixing TOOLS-2553
		// link.setText("Type the key or specify from <a>registry</a>, <a>workspace</a> or <a>local entries</a>");
		link.setText("Type the key or specify from <a>registry</a> or <a>workspace</a>");
		
		Link link_1 = new Link(grpPropertyKey, SWT.NONE);
		link_1.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				openNewResourceTemplateDialog();
			}
		});
		FormData fd_link_1 = new FormData();
		fd_link_1.top = new FormAttachment(rkTextField, 150);
		fd_link_1.right = new FormAttachment(rkTextField, 0, SWT.RIGHT);
		link_1.setLayoutData(fd_link_1);
		link_1.setText("<a>Create && point to a new resource...</a>");
		loadConfiguration();
		return container;
	}

	private void loadConfiguration() {
		if (!StringUtils.isBlank(rkProperty.getKeyValue())) {
			rkTextField.setText(rkProperty.getKeyValue());
		}
	}
	
	@Override
	protected void okPressed() {
		saveConfiguration();
		super.okPressed();
	}
	
	protected void saveConfiguration() {
		rkProperty.setKeyValue(rkTextField.getText());
	}
	
   protected void saveConfigurationWithTransaction() {
       TransactionalEditingDomain domain = TransactionUtil.getEditingDomain(rkProperty);
       if (domain != null) {
           domain.getCommandStack().execute(new RecordingCommand(domain) {
        
               @Override
               protected void doExecute() {
                   rkProperty.setKeyValue(rkTextField.getText());
               }
           });
       } else {
           rkProperty.setKeyValue(rkTextField.getText());
       }
   }

	
	protected void openEmbeddedEntryBrowser() {
		hide();
		try{
			EmbeddedEntriesDialog embeddedEntriesDialog = new EmbeddedEntriesDialog(getParentShell(), localNamedEntities);
			embeddedEntriesDialog.create();
			embeddedEntriesDialog.getShell().setText("Embedded Resources");
			embeddedEntriesDialog.open();
			if (embeddedEntriesDialog.getReturnCode()==Window.OK){
				setSelectedPath(embeddedEntriesDialog.getSelectedItem().trim());
			}		
		}finally{
			show();
		}

	}

	protected void openNewResourceTemplateDialog() {
		hide();
		try{
			NewResourceTemplateDialog newResourceTemplateDialog = new NewResourceTemplateDialog(getParentShell(),(Map<String, List<String>>) rkProperty.getFilters());
			newResourceTemplateDialog.create();
			newResourceTemplateDialog.getShell().setText("New Resource");
			newResourceTemplateDialog.getShell().setSize(480, 500);
			newResourceTemplateDialog.open();
			if (newResourceTemplateDialog.getReturnCode()==Window.OK){
				setSelectedPath(newResourceTemplateDialog.getSelectedPath());
				String localPathOfRegistryKey = newResourceTemplateDialog.getIPathOfSelection();
				((RegistryKeyPropertyImpl)rkProperty).setLocalPathOfRegistryKey(localPathOfRegistryKey);
			}
		}finally{
			show();
		}

	}
	
	private void openRegistryBrowser() {
		// Open registry browser.
		hide();
		try{
			IRegistryConnection[] registryConnections = CAppEnvironment.getRegistryHandler().getRegistryConnections();
			if (registryConnections.length==0){
				RegistryConnection registryConnection = new RegistryConnection();
				try {
					registryConnection.setURL(new URL(DEFAULT_REGISTRY_URL));
				} catch (MalformedURLException e) {
					//not gonna happen :D
				}
				registryConnection.setPath("/_system/config");
			}
			IRegistryData selectedPathData = CAppEnvironment.getRegistryHandler().selectRegistryPath(registryConnections, "Registry Browser", "Select Registry Resource:",
					IRegistryHandler.SELECTED_REGISTRY_RESOURCE);
			
			if (selectedPathData!=null){
				// Convert to prefixed format.
				setSelectedPath(selectedPathData.getPath());
			}
		}finally{
			show();
		}
		
	}

	private void setSelectedPath(String selectedPath) {
		if (selectedPath.startsWith(G_REG_PATH_PREFIX)) {
			selectedPath = String.format("gov:%s", selectedPath.substring(G_REG_PATH_PREFIX.length()));
		} else if (selectedPath.startsWith(C_REG_PATH_PREFIX)) {
			selectedPath = String.format("conf:%s", selectedPath.substring(C_REG_PATH_PREFIX.length()));
		}
		
		rkTextField.setText(selectedPath);
		if (selectedPath!=null && !selectedPath.trim().equals("")){
			okPressed();
		}
	}
	
	@SuppressWarnings("unchecked")
	private void openRegistryResourceProviderDialog(){
		hide();
		try{
		IntegrationStudioElementProviderDialog registryResourceProviderSelector = new IntegrationStudioElementProviderDialog(getParentShell(),new Class[]{IRegistryFile.class, IEsbEndpoint.class, IEsbSequence.class, IEsbLocalEntry.class},(Map<String,List<String>>)rkProperty.getFilters());
			
			registryResourceProviderSelector.create();
			registryResourceProviderSelector.getShell().setText("Workspace Element Providers");
			registryResourceProviderSelector.open();
			if (registryResourceProviderSelector.getReturnCode()==Window.OK){
				setSelectedPath(registryResourceProviderSelector.getSelectedPath());
				String localPathOfRegistryKey = registryResourceProviderSelector.getIPathOfSelection();
				((RegistryKeyPropertyImpl)rkProperty).setLocalPathOfRegistryKey(localPathOfRegistryKey);
			}
		}finally{
			show();
		}
	}
}
