/*
 * Zed Attack Proxy (ZAP) and its related class files.
 *
 * ZAP is an HTTP/HTTPS proxy for assessing web application security.
 *
 * Copyright 2021 The ZAP Development Team
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
package org.zaproxy.addon.reports;

import java.awt.Component;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.parosproxy.paros.Constant;
import org.parosproxy.paros.model.Model;
import org.parosproxy.paros.model.SiteMap;
import org.parosproxy.paros.model.SiteNode;
import org.parosproxy.paros.view.View;
import org.zaproxy.zap.extension.alert.AlertNode;
import org.zaproxy.zap.model.Context;
import org.zaproxy.zap.utils.DisplayUtils;
import org.zaproxy.zap.utils.ZapTextField;
import org.zaproxy.zap.view.StandardFieldsDialog;

public class ReportDialog extends StandardFieldsDialog {

    private static final Logger LOGGER = LogManager.getLogger(ReportDialog.class);

    private static final String FIELD_TITLE = "reports.dialog.field.title";
    private static final String FIELD_TEMPLATE = "reports.dialog.field.template";
    private static final String FIELD_REPORT_DIR = "reports.dialog.field.reportdir";
    private static final String FIELD_REPORT_NAME = "reports.dialog.field.reportname";
    private static final String FIELD_DESCRIPTION = "reports.dialog.field.description";
    private static final String FIELD_CONTEXTS = "reports.dialog.field.contexts";
    private static final String FIELD_SITES = "reports.dialog.field.sites";
    private static final String FIELD_GENERATE_ANYWAY = "reports.dialog.field.generateanyway";
    private static final String FIELD_DISPLAY_REPORT = "reports.dialog.field.display";
    private static final String FIELD_TEMPLATE_DIR = "reports.dialog.field.templatedir";
    private static final String FIELD_REPORT_NAME_PATTERN = "reports.dialog.field.namepattern";
    private static final String FIELD_CONFIDENCE_HEADER = "reports.dialog.field.confidence";
    private static final String FIELD_CONFIDENCE_0 = "reports.dialog.field.confidence.0";
    private static final String FIELD_CONFIDENCE_1 = "reports.dialog.field.confidence.1";
    private static final String FIELD_CONFIDENCE_2 = "reports.dialog.field.confidence.2";
    private static final String FIELD_CONFIDENCE_3 = "reports.dialog.field.confidence.3";
    private static final String FIELD_CONFIDENCE_4 = "reports.dialog.field.confidence.4";
    private static final String FIELD_RISK_HEADER = "reports.dialog.field.risk";
    private static final String FIELD_RISK_0 = "reports.dialog.field.risk.0";
    private static final String FIELD_RISK_1 = "reports.dialog.field.risk.1";
    private static final String FIELD_RISK_2 = "reports.dialog.field.risk.2";
    private static final String FIELD_RISK_3 = "reports.dialog.field.risk.3";

    private static final String[] TAB_LABELS = {
        "reports.dialog.tab.scope", "reports.dialog.tab.filter", "reports.dialog.tab.options",
    };

    private static final int TAB_SCOPE = 0;
    private static final int TAB_FILTER = 1;
    private static final int TAB_OPTIONS = 2;

    private static final long serialVersionUID = 1L;

    private ExtensionReports extension = null;
    private JButton[] extraButtons = null;
    private DefaultListModel<Context> contextsModel;
    private DefaultListModel<String> sitesModel;

    private JList<Context> contextsSelector;
    private JList<String> sitesSelector;

    public ReportDialog(ExtensionReports ext, Frame owner) {
        super(owner, "reports.dialog.title", DisplayUtils.getScaledDimension(600, 500), TAB_LABELS);

        this.extension = ext;
        // The first time init to the default options set, after that keep own copies
        reset(true);
    }

    public void init() {
        this.removeAllFields();
        // Ensure the contexts and sites get re-read as they may well have changed
        this.contextsModel = null;
        this.sitesModel = null;
        this.contextsSelector = null;
        this.sitesSelector = null;

        ReportParam reportParam = extension.getReportParam();

        // All these first as they are used by other fields
        this.addTextField(
                TAB_OPTIONS, FIELD_REPORT_NAME_PATTERN, reportParam.getReportNamePattern());
        this.addFileSelectField(
                TAB_OPTIONS,
                FIELD_TEMPLATE_DIR,
                new File(reportParam.getTemplateDirectory()),
                JFileChooser.DIRECTORIES_ONLY,
                null);
        this.addPadding(TAB_OPTIONS);

        this.addTextField(TAB_SCOPE, FIELD_TITLE, reportParam.getTitle());
        Template defaultTemplate = extension.getTemplateByConfigName(reportParam.getTemplate());
        this.addComboField(
                TAB_SCOPE,
                FIELD_TEMPLATE,
                extension.getTemplateNames(),
                defaultTemplate != null ? defaultTemplate.getDisplayName() : null);
        this.addTextField(TAB_SCOPE, FIELD_REPORT_NAME, "");
        this.addFileSelectField(
                TAB_SCOPE,
                FIELD_REPORT_DIR,
                new File(reportParam.getReportDirectory()),
                JFileChooser.DIRECTORIES_ONLY,
                null);
        this.addMultilineField(TAB_SCOPE, FIELD_DESCRIPTION, reportParam.getDescription());
        this.addCustomComponent(
                TAB_SCOPE, FIELD_CONTEXTS, getNewJScrollPane(getContextsSelector(), 400, 50));
        this.addCustomComponent(
                TAB_SCOPE, FIELD_SITES, getNewJScrollPane(getSitesSelector(), 400, 100));
        this.addCheckBoxField(TAB_SCOPE, FIELD_GENERATE_ANYWAY, false);
        this.addCheckBoxField(TAB_SCOPE, FIELD_DISPLAY_REPORT, reportParam.isDisplayReport());

        setReportName();
        ((JComboBox<?>) this.getField(FIELD_TEMPLATE))
                .addActionListener(
                        new ActionListener() {
                            @Override
                            public void actionPerformed(ActionEvent e) {
                                setReportName();
                            }
                        });
        getSitesSelector()
                .addListSelectionListener(
                        new ListSelectionListener() {
                            @Override
                            public void valueChanged(ListSelectionEvent e) {
                                setReportName();
                            }
                        });

        this.addTextFieldReadOnly(TAB_FILTER, FIELD_RISK_HEADER, "");
        this.addCheckBoxField(TAB_FILTER, FIELD_RISK_3, reportParam.isIncRisk3());
        this.addCheckBoxField(TAB_FILTER, FIELD_RISK_2, reportParam.isIncRisk2());
        this.addCheckBoxField(TAB_FILTER, FIELD_RISK_1, reportParam.isIncRisk1());
        this.addCheckBoxField(TAB_FILTER, FIELD_RISK_0, reportParam.isIncRisk0());

        this.addTextFieldReadOnly(TAB_FILTER, FIELD_CONFIDENCE_HEADER, "");
        this.addCheckBoxField(TAB_FILTER, FIELD_CONFIDENCE_4, reportParam.isIncConfidence4());
        this.addCheckBoxField(TAB_FILTER, FIELD_CONFIDENCE_3, reportParam.isIncConfidence3());
        this.addCheckBoxField(TAB_FILTER, FIELD_CONFIDENCE_2, reportParam.isIncConfidence2());
        this.addCheckBoxField(TAB_FILTER, FIELD_CONFIDENCE_1, reportParam.isIncConfidence1());
        this.addCheckBoxField(TAB_FILTER, FIELD_CONFIDENCE_0, reportParam.isIncConfidence0());
        this.addPadding(TAB_FILTER);

        this.pack();
    }

    private void setReportName() {
        String pattern = this.getStringValue(FIELD_REPORT_NAME_PATTERN);
        String name =
                ExtensionReports.getNameFromPattern(pattern, getSitesSelector().getSelectedValue());
        Template template = extension.getTemplateByDisplayName(getStringValue(FIELD_TEMPLATE));
        ((ZapTextField) this.getField(FIELD_REPORT_NAME))
                .setText(name + "." + template.getExtension());
    }

    private JScrollPane getNewJScrollPane(Component view, int width, int height) {
        JScrollPane pane = new JScrollPane(view);
        pane.setPreferredSize(DisplayUtils.getScaledDimension(width, height));
        pane.setMinimumSize((DisplayUtils.getScaledDimension(width, height)));
        return pane;
    }

    private DefaultListModel<Context> getContextsModel() {
        if (contextsModel == null) {
            contextsModel = new DefaultListModel<Context>();
            for (Context context : Model.getSingleton().getSession().getContexts()) {
                contextsModel.addElement(context);
            }
        }
        return contextsModel;
    }

    private JList<Context> getContextsSelector() {
        if (contextsSelector == null) {
            contextsSelector = new JList<>(getContextsModel());
            contextsSelector.setCellRenderer(
                    new DefaultListCellRenderer() {
                        private static final long serialVersionUID = 1L;

                        @Override
                        public Component getListCellRendererComponent(
                                JList<?> list,
                                Object value,
                                int index,
                                boolean isSelected,
                                boolean cellHasFocus) {
                            JLabel label =
                                    (JLabel)
                                            super.getListCellRendererComponent(
                                                    list, value, index, isSelected, cellHasFocus);
                            if (value instanceof Context) {
                                label.setText(((Context) value).getName());
                            }
                            return label;
                        }
                    });
        }
        return contextsSelector;
    }

    private DefaultListModel<String> getSitesModel() {
        if (sitesModel == null) {
            sitesModel = new DefaultListModel<String>();
            SiteMap siteMap = Model.getSingleton().getSession().getSiteTree();
            SiteNode root = siteMap.getRoot();
            if (root.getChildCount() > 0) {
                SiteNode child = (SiteNode) root.getFirstChild();
                while (child != null) {
                    sitesModel.addElement(child.getName());
                    child = (SiteNode) root.getChildAfter(child);
                }
            }
        }
        return sitesModel;
    }

    private JList<String> getSitesSelector() {
        if (sitesSelector == null) {
            sitesSelector = new JList<>(getSitesModel());
        }
        return sitesSelector;
    }

    @Override
    public String getHelpIndex() {
        return "reports";
    }

    private void reset(boolean refreshUi) {

        if (refreshUi) {
            init();
            repaint();
        }
    }

    @Override
    public String getSaveButtonText() {
        return Constant.messages.getString("reports.dialog.button.generate");
    }

    @Override
    public JButton[] getExtraButtons() {
        if (extraButtons == null) {
            JButton resetButton =
                    new JButton(Constant.messages.getString("reports.dialog.button.reset"));
            resetButton.addActionListener(
                    new java.awt.event.ActionListener() {
                        @Override
                        public void actionPerformed(java.awt.event.ActionEvent e) {
                            reset(true);
                        }
                    });

            extraButtons = new JButton[] {resetButton};
        }

        return extraButtons;
    }

    private ReportData getReportData() {
        ReportData reportData = new ReportData();
        reportData.setTitle(this.getStringValue(FIELD_TITLE));
        reportData.setDescription(this.getStringValue(FIELD_DESCRIPTION));
        reportData.setContexts(this.getContextsSelector().getSelectedValuesList());
        reportData.setSites(this.getSitesSelector().getSelectedValuesList());
        if (reportData.getSites().isEmpty()) {
            // None selected so add all
            reportData.setSites(
                    IntStream.range(0, getSitesModel().size())
                            .mapToObj(getSitesModel()::get)
                            .collect(Collectors.toList()));
        }
        reportData.setIncludeConfidence(0, this.getBoolValue(FIELD_CONFIDENCE_0));
        reportData.setIncludeConfidence(1, this.getBoolValue(FIELD_CONFIDENCE_1));
        reportData.setIncludeConfidence(2, this.getBoolValue(FIELD_CONFIDENCE_2));
        reportData.setIncludeConfidence(3, this.getBoolValue(FIELD_CONFIDENCE_3));
        reportData.setIncludeConfidence(4, this.getBoolValue(FIELD_CONFIDENCE_4));
        reportData.setIncludeRisk(0, this.getBoolValue(FIELD_RISK_0));
        reportData.setIncludeRisk(1, this.getBoolValue(FIELD_RISK_1));
        reportData.setIncludeRisk(2, this.getBoolValue(FIELD_RISK_2));
        reportData.setIncludeRisk(3, this.getBoolValue(FIELD_RISK_3));
        // Always do this last as it depends on the other fields
        reportData.setAlertTreeRootNode(extension.getFilteredAlertTree(reportData));
        return reportData;
    }

    @Override
    public void save() {
        ReportData reportData = getReportData();
        boolean displayReport = this.getBoolValue(FIELD_DISPLAY_REPORT);
        Template template = extension.getTemplateByDisplayName(getStringValue(FIELD_TEMPLATE));

        // Always save all of the options
        ReportParam reportParam = extension.getReportParam();
        reportParam.setDisplayReport(displayReport);
        reportParam.setTitle(reportData.getTitle());
        reportParam.setDescription(reportData.getDescription());
        reportParam.setTemplate(template.getConfigName());
        reportParam.setReportDirectory(this.getStringValue(FIELD_REPORT_DIR));
        reportParam.setTemplateDirectory(this.getStringValue(FIELD_TEMPLATE_DIR));
        reportParam.setReportNamePattern(this.getStringValue(FIELD_REPORT_NAME_PATTERN));
        reportParam.setIncConfidence0(this.getBoolValue(FIELD_CONFIDENCE_0));
        reportParam.setIncConfidence1(this.getBoolValue(FIELD_CONFIDENCE_1));
        reportParam.setIncConfidence2(this.getBoolValue(FIELD_CONFIDENCE_2));
        reportParam.setIncConfidence3(this.getBoolValue(FIELD_CONFIDENCE_3));
        reportParam.setIncConfidence4(this.getBoolValue(FIELD_CONFIDENCE_4));
        reportParam.setIncRisk0(this.getBoolValue(FIELD_RISK_0));
        reportParam.setIncRisk1(this.getBoolValue(FIELD_RISK_1));
        reportParam.setIncRisk2(this.getBoolValue(FIELD_RISK_2));
        reportParam.setIncRisk3(this.getBoolValue(FIELD_RISK_3));
        try {
            reportParam.getConfig().save();
        } catch (ConfigurationException e) {
            LOGGER.error("Failed to save Reports configuration", e);
        }

        try {
            this.extension.generateReport(
                    reportData, template, getReportFile().getAbsolutePath(), displayReport);

        } catch (Exception e) {
            View.getSingleton()
                    .showWarningDialog(
                            thisDialog,
                            Constant.messages.getString(
                                    "reports.dialog.error.generate", e.getMessage()));
            LOGGER.error(
                    "Failed to generate a report using template "
                            + extension.getTemplateByDisplayName(getStringValue(FIELD_TEMPLATE)),
                    e);
        }
    }

    private File getReportFile() {
        return new File(
                this.getStringValue(FIELD_REPORT_DIR), this.getStringValue(FIELD_REPORT_NAME));
    }

    @Override
    public void setVisible(boolean show) {
        super.setVisible(show);
    }

    @Override
    public String validateFields() {
        File f = getReportFile();
        if (!f.exists()) {
            if (!f.getParentFile().canWrite()) {
                return Constant.messages.getString(
                        "reports.dialog.error.dirperms", f.getParentFile().getAbsolutePath());
            }
        } else if (!f.canWrite()) {
            return Constant.messages.getString(
                    "reports.dialog.error.fileperms", f.getParentFile().getAbsolutePath());
        }
        AlertNode root = extension.getFilteredAlertTree(getReportData());
        if (root.getChildCount() == 0 && !this.getBoolValue(FIELD_GENERATE_ANYWAY)) {
            return Constant.messages.getString("reports.dialog.error.noalerts");
        }
        return null;
    }

    void reset() {
        reset(true);
    }
}
