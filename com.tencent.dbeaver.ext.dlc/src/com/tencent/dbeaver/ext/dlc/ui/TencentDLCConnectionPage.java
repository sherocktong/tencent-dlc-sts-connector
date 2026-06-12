/*
 * Tencent DLC DBeaver Plugin
 * Copyright (C) 2026 Bing Tong and contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.tencent.dbeaver.ext.dlc.ui;

import com.tencent.dbeaver.ext.dlc.TencentDLCConstants;
import org.eclipse.jface.dialogs.IDialogPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.ui.IDialogPageProvider;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.connection.ConnectionPageWithAuth;
import org.jkiss.dbeaver.ui.dialogs.connection.DriverPropertiesDialogPage;
import org.jkiss.utils.CommonUtils;

/**
 * Tencent DLC connection page.
 */
public class TencentDLCConnectionPage extends ConnectionPageWithAuth implements IDialogPageProvider {

    private Text hostText;
    private Text regionText;
    private Text taskTypeText;
    private Text dataEngineNameText;
    private Text datasourceConnectionNameText;
    private Text resultTypeText;
    private Text readTypeText;

    private final DriverPropertiesDialogPage driverPropsPage;

    public TencentDLCConnectionPage() {
        driverPropsPage = new DriverPropertiesDialogPage(this);
    }

    @Override
    public void createControl(Composite composite) {
        Composite settingsGroup = new Composite(composite, SWT.NONE);
        settingsGroup.setLayout(new GridLayout(1, false));
        settingsGroup.setLayoutData(new GridData(GridData.FILL_BOTH));

        ModifyListener textListener = e -> site.updateButtons();

        {
            Composite addrGroup = UIUtils.createTitledComposite(
                settingsGroup,
                "Connection settings",
                2,
                GridData.FILL_HORIZONTAL
            );

            hostText = UIUtils.createLabelText(addrGroup, "Host", TencentDLCConstants.DEFAULT_HOST);
            hostText.addModifyListener(textListener);

            regionText = UIUtils.createLabelText(addrGroup, "Region", TencentDLCConstants.DEFAULT_REGION);
            regionText.addModifyListener(textListener);

            taskTypeText = UIUtils.createLabelText(addrGroup, "Task Type", TencentDLCConstants.DEFAULT_TASK_TYPE);
            taskTypeText.addModifyListener(textListener);

            dataEngineNameText = UIUtils.createLabelText(addrGroup, "Data Engine Name", "");
            dataEngineNameText.addModifyListener(textListener);

            datasourceConnectionNameText = UIUtils.createLabelText(
                addrGroup,
                "Datasource Connection Name",
                TencentDLCConstants.DEFAULT_DATASOURCE_CONNECTION_NAME
            );
            datasourceConnectionNameText.addModifyListener(textListener);

            resultTypeText = UIUtils.createLabelText(addrGroup, "Result Type", TencentDLCConstants.DEFAULT_RESULT_TYPE);
            resultTypeText.addModifyListener(textListener);

            readTypeText = UIUtils.createLabelText(addrGroup, "Read Type", TencentDLCConstants.DEFAULT_READ_TYPE);
            readTypeText.addModifyListener(textListener);
        }

        createAuthPanel(settingsGroup, 1);
        createDriverPanel(settingsGroup);
        setControl(settingsGroup);
    }

    @Override
    public boolean isComplete() {
        return hostText != null && !CommonUtils.isEmpty(hostText.getText()) && super.isComplete();
    }

    @Override
    public void loadSettings() {
        super.loadSettings();

        DBPConnectionConfiguration connectionInfo = site.getActiveDataSource().getConnectionConfiguration();

        if (hostText != null) {
            String host = connectionInfo.getHostName();
            if (CommonUtils.isEmpty(host)) {
                host = TencentDLCConstants.DEFAULT_HOST;
            }
            hostText.setText(host);
        }

        setTextValue(regionText, connectionInfo, TencentDLCConstants.PROP_REGION, TencentDLCConstants.DEFAULT_REGION);
        setTextValue(taskTypeText, connectionInfo, TencentDLCConstants.PROP_TASK_TYPE, TencentDLCConstants.DEFAULT_TASK_TYPE);
        setTextValue(dataEngineNameText, connectionInfo, TencentDLCConstants.PROP_DATA_ENGINE_NAME, "");
        setTextValue(datasourceConnectionNameText, connectionInfo, TencentDLCConstants.PROP_DATASOURCE_CONNECTION_NAME, TencentDLCConstants.DEFAULT_DATASOURCE_CONNECTION_NAME);
        setTextValue(resultTypeText, connectionInfo, TencentDLCConstants.PROP_RESULT_TYPE, TencentDLCConstants.DEFAULT_RESULT_TYPE);
        setTextValue(readTypeText, connectionInfo, TencentDLCConstants.PROP_READ_TYPE, TencentDLCConstants.DEFAULT_READ_TYPE);
    }

    private static void setTextValue(Text text, DBPConnectionConfiguration connectionInfo, String key, String defaultValue) {
        if (text != null) {
            String value = connectionInfo.getProviderProperty(key);
            if (CommonUtils.isEmpty(value)) {
                value = defaultValue;
            }
            text.setText(value);
        }
    }

    @Override
    public void saveSettings(@NotNull DBPDataSourceContainer dataSource) {
        DBPConnectionConfiguration connectionInfo = dataSource.getConnectionConfiguration();

        if (hostText != null) {
            connectionInfo.setHostName(hostText.getText().trim());
        }
        saveProviderProperty(connectionInfo, regionText, TencentDLCConstants.PROP_REGION);
        saveProviderProperty(connectionInfo, taskTypeText, TencentDLCConstants.PROP_TASK_TYPE);
        saveProviderProperty(connectionInfo, dataEngineNameText, TencentDLCConstants.PROP_DATA_ENGINE_NAME);
        saveProviderProperty(connectionInfo, datasourceConnectionNameText, TencentDLCConstants.PROP_DATASOURCE_CONNECTION_NAME);
        saveProviderProperty(connectionInfo, resultTypeText, TencentDLCConstants.PROP_RESULT_TYPE);
        saveProviderProperty(connectionInfo, readTypeText, TencentDLCConstants.PROP_READ_TYPE);

        super.saveSettings(dataSource);
    }

    private static void saveProviderProperty(DBPConnectionConfiguration connectionInfo, Text text, String key) {
        if (text != null) {
            connectionInfo.setProviderProperty(key, text.getText().trim());
        }
    }

    @Nullable
    @Override
    public IDialogPage[] getDialogPages(boolean extrasOnly, boolean forceCreate) {
        return new IDialogPage[]{ driverPropsPage };
    }
}
