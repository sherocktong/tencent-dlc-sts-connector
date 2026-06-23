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

import com.tencent.dlc.core.TencentDLCConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.access.DBAAuthModel;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.connection.DatabaseNativeAuthModelConfigurator;
import org.jkiss.utils.CommonUtils;

/**
 * Auth configurator for Tencent DLC.
 *
 * Adds a Token field below the standard username/password rows and keeps
 * the password field always editable regardless of the "Save password" checkbox.
 */
public class TencentDLCAuthModelConfigurator extends DatabaseNativeAuthModelConfigurator {

    private Text tokenText;

    @Override
    protected boolean isForceSaveCredentials() {
        return true;
    }

    @Override
    public void createControl(Composite parent, DBAAuthModel<?> authModel, Runnable changeListener) {
        super.createControl(parent, authModel, changeListener);

        // Keep password always editable: the parent's checkbox listener disables it when
        // unchecked, so we add a second listener that re-enables it right after.
        if (savePasswordCheck != null) {
            savePasswordCheck.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    if (passwordText != null && !passwordText.isDisposed()) {
                        passwordText.setEnabled(true);
                    }
                }
            });
        }

        UIUtils.createLabel(parent, "Token");
        tokenText = new Text(parent, SWT.BORDER | SWT.PASSWORD);
        GridData gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.horizontalSpan = 2;
        tokenText.setLayoutData(gd);
        tokenText.addModifyListener(e -> changeListener.run());
    }

    @Override
    public void loadSettings(DBPDataSourceContainer dataSource) {
        super.loadSettings(dataSource);

        // Parent sets passwordText.setEnabled(isSavePassword()), which is false for new
        // connections. Force it enabled — DLC credentials must always be typeable.
        if (passwordText != null && !passwordText.isDisposed()) {
            passwordText.setEnabled(true);
        }

        if (tokenText != null && !tokenText.isDisposed()) {
            String token = dataSource.getConnectionConfiguration().getAuthProperty(TencentDLCConstants.PROP_AUTH_TOKEN);
            tokenText.setText(CommonUtils.notEmpty(token));
        }
    }

    @Override
    public void saveSettings(DBPDataSourceContainer dataSource) {
        super.saveSettings(dataSource);
        if (tokenText != null && !tokenText.isDisposed()) {
            dataSource.getConnectionConfiguration().setAuthProperty(
                TencentDLCConstants.PROP_AUTH_TOKEN,
                tokenText.getText().trim()
            );
        }
    }
}
