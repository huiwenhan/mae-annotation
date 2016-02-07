/*
 * MAE - Multi-purpose Annotation Environment
 *
 * Copyright Keigh Rim (krim@brandeis.edu)
 * Department of Computer Science, Brandeis University
 * Original program by Amber Stubbs (astubbs@cs.brandeis.edu)
 *
 * MAE is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, @see <a href="http://www.gnu.org/licenses">http://www.gnu.org/licenses</a>.
 *
 * For feedback, reporting bugs, use the project on Github
 * @see <a href="https://github.com/keighrim/mae-annotation">https://github.com/keighrim/mae-annotation</a>.
 */

package edu.brandeis.cs.nlp.mae.controller;

import edu.brandeis.cs.nlp.mae.MaeException;
import edu.brandeis.cs.nlp.mae.MaeStrings;
import edu.brandeis.cs.nlp.mae.database.LocalSqliteDriverImpl;
import edu.brandeis.cs.nlp.mae.database.MaeDBException;
import edu.brandeis.cs.nlp.mae.database.MaeDriverI;
import edu.brandeis.cs.nlp.mae.io.MaeIOException;
import edu.brandeis.cs.nlp.mae.model.*;
import edu.brandeis.cs.nlp.mae.util.ColorHandler;
import edu.brandeis.cs.nlp.mae.util.SpanHandler;
import edu.brandeis.cs.nlp.mae.view.MaeMainView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.plaf.FontUIResource;
import javax.swing.text.Highlighter;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.*;
import java.util.List;
import java.util.Timer;

/**
 * Created by krim on 12/30/2015.
 */
public class MaeMainController extends JPanel {

    public static final int DEFAULT_FONT_SIZE = 12;
    public static final String DEFAULT_FONT_FAMILY = "DejaVu Sans";
    public static final int MODE_NORMAL = 0;
    public static final int MODE_MULTI_SPAN = 1;
    public static final int MODE_ARG_SEL = 2;
    public static final int MODE_ADJUD = 9;
    private static final Logger logger = LoggerFactory.getLogger(MaeMainController.class.getName());
    private int mode;

    private JFrame mainFrame;


    private StatusBarController statusBar;
    private TextPanelController textPanel;
    private TablePanelController tablePanel;
    private MenuController menu;
    private DialogController dialogs;

    // booleans for user preferences
    private boolean normalModeOnCreation = true; // on by default
    private String mFilenameSuffix = ""; // for file operation

    // database connectors
    private List<MaeDriverI> drivers;
    private MaeDriverI currentDriver;

    private ColorHandler textHighlighColors;
    private List<TagType> tagsForColor;
    private Map<TagType, Boolean> coloredTagsInLastDocument;
    private ColorHandler documentTabColors;

    public MaeMainController() {

        drivers = new ArrayList<>();

        mode = MODE_NORMAL;
        tagsForColor = new ArrayList<>();
        documentTabColors = new ColorHandler(6); // by default, 6 colors allowed to distinguish documents

        try {
            menu = new MenuController(this);
            textPanel = new TextPanelController(this);
            tablePanel = new TablePanelController(this);
            statusBar = new StatusBarController(this);
            dialogs = new DialogController(this);
            coloredTagsInLastDocument = new HashMap<>();
        } catch (MaeException e) {
            showError(e);
        }
    }

    public static void main(String[] args) {
        MaeMainController main = new MaeMainController();
        JFrame mainFrame = main.initUI();
        main.setWindowFrame(mainFrame);
        mainFrame.pack();
        mainFrame.setSize(900, 700);
        mainFrame.setVisible(true);

        if (args.length > 0) {
            boolean argCmd = false;
            List<String> argsList = new ArrayList<>();
            String taskFilename = null;
            String docFilename = null;
            for (String arg : args) {
                argsList.add(arg);
            }
            if (argsList.contains("--task")) {
                taskFilename = argsList.get(argsList.indexOf("--task") + 1);
                argCmd = true;
                if (argsList.contains("--doc")) {
                    docFilename = argsList.get(argsList.indexOf("--doc") + 1);

                }
            }
            if (!argCmd) {
                System.out.println("TODO: show some help text");
            }

            if (taskFilename != null) {
                main.setupScheme(new File(taskFilename), true);
                if (docFilename != null) {
                    main.addDocument(new File(docFilename));
                }
            }
        }
    }

    private void setWindowFrame(JFrame mainFrame) {
        this.mainFrame = mainFrame;
    }

    private JFrame initUI() {
        logger.debug("initiating UI components.");

        FontUIResource resource = new FontUIResource(new Font(DEFAULT_FONT_FAMILY, Font.PLAIN, DEFAULT_FONT_SIZE));
        java.util.Enumeration keys = UIManager.getDefaults().keys();
        while (keys.hasMoreElements())
        {
            Object key = keys.nextElement();
            Object value = UIManager.get(key);
            if (value instanceof javax.swing.plaf.FontUIResource)
            {
                UIManager.put(key, resource);
            }
        }
        return new MaeMainView(menu.getView(), textPanel.getView(), statusBar.getView(), tablePanel.getView());
    }

    private MenuController getMenu() {
        return menu;
    }

    private DialogController getDialogs() {
        return dialogs;
    }

    public boolean showWarning(String message) {
        boolean response = getDialogs().showWarning(message) == JOptionPane.OK_OPTION;
        logger.warn(message + ": " + response);
        return response;

    }

    public boolean showUnsavedChangeWarning() {
        List<String> unsavedFiles = new LinkedList<>();
        for (MaeDriverI driver : getDrivers()) {
            if (driver.isAnnotationChanged()) {
                try {
                    unsavedFiles.add(driver.getAnnotationFileBaseName());
                } catch (MaeDBException ignored) { // this won't happen
                }
            }
        }
        if (unsavedFiles.size() > 0) {
            String warning = String.format("Warning! You have unsaved changes. \n%s\n Are you sure to continue?"
                    , unsavedFiles.toString());
            return showWarning(warning);
        } else {
            return true;
        }

    }

    public boolean showBatchDeletionWarning() {
        String warning = ("Deleting extent tag(s) will also delete \n" +
                "any links that use these extents.  Would you like to continue?");
        return showWarning(warning);
    }

    public void showError(Exception e) {
        getDialogs().showError(e);
        logException(e);
    }

    public void showError(String message, Exception e) {
        getDialogs().showError(message, e);
        logger.error(message);
        logException(e);
    }

    void logException(Exception e) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        logger.error(sw.toString());
    }


    public void showError(String message) {
        getDialogs().showError(message);
        logger.error(message);
    }

    public boolean isTaskLoaded() {
        return !drivers.isEmpty() && getDriver().isTaskLoaded();
    }

    public Set<TagType> getActiveLinkTags() {
        return getTablePanel().getActiveLinkTags();
    }

    public Set<TagType> getActiveExtentTags() {
        return getTablePanel().getActiveExtentTags();
    }

    public boolean isDocumentOpen() {
        return isTaskLoaded() && getDriver().isAnnotationLoaded();
    }

    public boolean isAnnotationChanged() {
        return getDriver().isAnnotationChanged();
    }

    public void updateSavedStatusInTextPanel() {
        try {
            getTextPanel().updateTabTitles();
        } catch (MaeDBException e) {
            showError(e);
        }
    }

    public boolean normalModeOnCreation() {
        return normalModeOnCreation;
    }

    public void setnormalModeOnCreation(boolean b) {
        this.normalModeOnCreation = b;
    }

    public int getMode() {
        return mode;
    }

    public void setMode(int mode) {
        this.mode = mode;
    }

    public JFrame getMainWindow() {
        return mainFrame;
    }

    public String getFilenameSuffix() {
        return mFilenameSuffix;
    }

    public void setFilenameSuffix(String mFilenameSuffix) {
        this.mFilenameSuffix = mFilenameSuffix;
    }

    private TablePanelController getTablePanel() {
        return tablePanel;
    }

    private TextPanelController getTextPanel() {
        return textPanel;
    }

    private StatusBarController getStatusBar() {
        return statusBar;
    }

    public MaeDriverI getDriver() {
        return currentDriver;
    }

    public List<MaeDriverI> getDrivers() {
        return drivers;
    }

    public MaeDriverI getDriverAt(int i) {
        return drivers.get(i);
    }

    public int getCurrentDocumentTabIndex() {
        return getTextPanel().getCurrentTab();
    }

    public void closeCurrentDocument() {
        closeDocumentAt(getCurrentDocumentTabIndex());
    }

    public void closeDocumentAt(int i) {
        int openDrivers = getDrivers().size();
        if (i > openDrivers || openDrivers != getTextPanel().getOpenTabCount()) {
            showError("drivers and documents do not match!");
        }
        try {
            if (getDrivers().size() > 1) {
                getDriverAt(i).destroy();
                drivers.remove(i);
                getTextPanel().closeDocumentTab(i);
            } else {
                File taskFile = new File(getDriver().getTaskFileName());
                setupScheme(taskFile, true);
            }
        } catch (MaeDBException e) {
            showError(e);
        }
    }

    public void sendNotification(String message) {
        getStatusBar().setText(message);
        mouseCursorToDefault();
        logger.debug(message);
    }

    public void updateNotificationAreaIn(long millisecond) {
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                updateNotificationArea();
            }
        }, millisecond);

    }

    void updateNotificationArea() {
        getStatusBar().refresh();
        mouseCursorToDefault();

    }

    public void sendTemporaryNotification(String message, long periodMillisecond) {
        sendNotification(message);
        updateNotificationAreaIn(periodMillisecond);
    }

    public void sendWaitMessage() {
        getStatusBar().setText(MaeStrings.WAIT_MESSAGE);
        mouseCursorToWait();
    }

    public void mouseCursorToDefault() {
        getMainWindow().setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
    }

    public void mouseCursorToWait() {
        getMainWindow().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
    }

    public void switchToArgSelMode() {

        if (mode != MODE_ARG_SEL) {
            mode = MODE_ARG_SEL;
            sendTemporaryNotification(MaeStrings.SB_ARGSEL_MODE_NOTI, 3000);
            getMenu().resetModeMenu();
        }
    }

    public void switchToMSpanMode() {

        if (mode != MODE_MULTI_SPAN) {
            mode = MODE_MULTI_SPAN;
            sendTemporaryNotification(MaeStrings.SB_MSPAN_MODE_NOTI, 3000);
            getMenu().resetModeMenu();
        }
    }

    public void switchToNormalMode() {

        if (mode != MODE_NORMAL) {
            mode = MODE_NORMAL;
            sendTemporaryNotification(MaeStrings.SB_NORM_MODE_NOTI, 3000);
            removeAllBGColors();
            addBGColorOver(getTextPanel().leavingLatestSelection(), ColorHandler.getDefaultHighlighter());
            getMenu().resetModeMenu();
        }
    }

    public void setupScheme(File taskFile, boolean fromNewTask) {
        // this always wipes out on-going annotation works,
        // even with multi-file support, an instance of MAE requires all works share the same DB schema
        try {
            sendWaitMessage();
            String dbFilename = String.format("mae-%d", System.currentTimeMillis());
            File dbFile;
            try {
                dbFile = File.createTempFile(dbFilename, ".sqlite");
            } catch (IOException e) {
                showError("Could not generate DB file!", e);
                return;
            }
            if (fromNewTask) {
                wipeDrivers();
            }
            currentDriver = new LocalSqliteDriverImpl(dbFile.getAbsolutePath());
            drivers.add(currentDriver);
            try {
                getDriver().readTask(taskFile);
            } catch (MaeIOException | IOException e) {
                showError("Could not open task definition!", e);
                return;
            }
            logger.info(String.format("task \"%s\" is loaded, has %d extent tag definitions and %d link tag definitions",
                    getDriver().getTaskName(), getDriver().getExtentTagTypes().size(), getDriver().getLinkTagTypes().size()));
            if (fromNewTask) {
                resetPaintableColors();
                getMenu().resetFileMenu();
                getTextPanel().noDocumentGuide();
                getMainWindow().setTitle(String.format("%s :: %s", MaeStrings.TITLE_PREFIX, getDriver().getTaskName()));
                getTablePanel().makeAllTables();
                storePaintedStates();
                sendTemporaryNotification(MaeStrings.SB_NEWTASK, 3000);
            }
        } catch (MaeDBException e) {
            showError("Found an error in DB!", e);
        } catch (MaeControlException e) {
            showError("Failed to sort out tag tables!", e);
        }
    }

    public void addDocument(File annotationFile) {
        for (MaeDriverI driver : getDrivers()) {
            try {
                if (annotationFile.getAbsolutePath().equals(driver.getAnnotationFileName())) {
                    showError(String.format("%s \nis already open!", annotationFile.getName()));
                    return;

                }
            } catch (MaeDBException ignored) {
            }
        }
        try {
            boolean multiFile = getDrivers().size() > 0 && getDriver().isAnnotationLoaded();
            sendWaitMessage();
            if (multiFile) {
                setupScheme(new File(getDriver().getTaskFileName()), false);
            }
            getDriver().readAnnotation(annotationFile);
            getTextPanel().addDocumentTab(getDriver().getAnnotationFileBaseName(), getDriver().getPrimaryText());
            logger.info(String.format("document \"%s\" is open.", getDriver().getAnnotationFileBaseName()));
            if (!multiFile) {
                getMenu().resetFileMenu();
                getMenu().resetTagsMenu();
                getMenu().resetModeMenu();
                getTablePanel().insertAllTags();
            }
            getTextPanel().assignAllFGColors();
            sendTemporaryNotification(MaeStrings.SB_FILEOPEN, 3000);
        } catch (Exception e) {
            showError(e);
        }

    }

    public void switchAnnotationTab(int tabId) {
        try {
            sendWaitMessage();
            if (getDriver().isAnnotationLoaded()) {
                getTablePanel().wipeAllTables();
                getTextPanel().clearSelection();
                getTextPanel().clearCaret();
                updateNotificationArea();
            }
            currentDriver = drivers.get(tabId);
            getTablePanel().insertAllTags();
            assignTextColorsOver(anchorsToRepaint());
            storePaintedStates();
            logger.info(String.format("switched to document \"%s\", using DB file at \"%s\"",
                    getDriver().getAnnotationFileBaseName(), getDriver().getDBSourceName()));
            updateNotificationArea();
        } catch (MaeException e) {
            showError(e);
        }

    }

    public String getTextIn(int...locations) {
        try {
            return getTextPanel().getTextIn(locations, false);
        } catch (MaeControlException e) {
            showError(e);
        }
        return null;
    }

    public String getSelectedText() {
        try {
            return getTextPanel().getSelectedText();
        } catch (MaeControlException e) {
            showError(e);
        }
        return null;
    }

    public int[] getSelectedTextSpans() {
        return getTextPanel().getSelected();
    }

    public List<ExtentTag> getSelectedArguments() {
        try {
            return getTextPanel().getSelectedArgumentsInOrder();
        } catch (MaeDBException e) {
            showError(e);
        }
        return null;
    }

    public void undoLastSelection() {
        String notification;
        int[] unselected = getTextPanel().undoSelection();
        if (unselected != null) {
            removeAllBGColors();
            addBGColorOver(getSelectedTextSpans(), ColorHandler.getDefaultHighlighter());
            addBGColorOver(unselected, ColorHandler.getFadingHighlighter());
            notification = String.format(
                    "Removed '%s' from selection! Click anywhere to continue."
                    , getTextIn(unselected));
        } else {
            notification = "Nothing to undo! Click anywhere to continue.";
        }

        sendTemporaryNotification(notification, 3000);

    }

    public void clearTextSelection() {
        getTextPanel().clearSelection();
        sendTemporaryNotification("Clear!, Click anywhere to continue", 3000);
    }

    public File selectSingleFile(String defautName, boolean saveFile) {
        return getDialogs().showFileChooseDialogAndSelect(defautName, saveFile);
    }

    public void assignTextColorsOver(List<Integer> anchors) {
        try {
            getTextPanel().assignFGColorOver(anchors);
        } catch (Exception e) {
            showError(e);
        }
    }

    void resetPaintableColors() throws MaeDBException {
        textHighlighColors = new ColorHandler(getDriver().getExtentTagTypes().size());
        tagsForColor.clear();
    }

    void storePaintedStates() {
        Set<TagType> activated = getTablePanel().getActiveTags();
        try {
            for (TagType type : getDriver().getAllTagTypes()) {
                if (activated.contains(type)) {
                    coloredTagsInLastDocument.put(type, Boolean.TRUE);
                } else {
                    coloredTagsInLastDocument.put(type, Boolean.FALSE);
                }
            }
        } catch (MaeDBException e) {
            showError(e);
        }
    }

    List<Integer> anchorsToRepaint() {
        Set<Integer> toRepaint = new HashSet<>();
        Set<TagType> currentlyActivated = getTablePanel().getActiveTags();
        for (TagType type : coloredTagsInLastDocument.keySet()) {
            if ((currentlyActivated.contains(type) && !coloredTagsInLastDocument.get(type))
                || (!currentlyActivated.contains(type) && coloredTagsInLastDocument.get(type))) {
                try {
                    toRepaint.addAll(getDriver().getAllAnchorsOfTagType(type, Collections.<TagType>emptyList()));
                } catch (MaeDBException e) {
                    showError(e);
                }
            }
        }
        return new ArrayList<>(toRepaint);
    }

    private void wipeDrivers() {
        for (MaeDriverI driver : drivers) {
            try {
                driver.destroy();
            } catch (Exception e) {
                showError(e);
            }
        }
        drivers.clear();
    }

    public List<ExtentTag> getExtentTagsInSelectedSpans() {
        try {
            return getDriver().getTagsIn(getSelectedTextSpans());
        } catch (Exception e) {
            showError(e);
            return new ArrayList<>();
        }
    }

    public Color getDocumentColor(int documentTabIndex) {
        return documentTabColors.getColor(documentTabIndex);
    }

    public Color getFGColor(TagType type) {
        if (!tagsForColor.contains(type)) {
            tagsForColor.add(type);
        }
        return getTextHighlightColors().getColor(tagsForColor.indexOf(type));

    }

    public int paintableTagTypes() {
        return tagsForColor.size();
    }

    public ColorHandler getTextHighlightColors() {
        return textHighlighColors;
    }

    public boolean isTextSelected() {
        return getTextPanel().isTextSelected();
    }

    public void addBGColorOver(List<Integer> spans, Highlighter.HighlightPainter painter) {
        addBGColorOver(SpanHandler.convertIntegerlistToIntegerarray(spans), painter);

    }

    public void addBGColorOver(int[] spans, Highlighter.HighlightPainter painter) {
        try {
            getTextPanel().addBGColorOver(spans, painter);
        } catch (MaeControlException e) {
            showError(e);
        }
    }

    public void removeAllBGColors() {
        getTextPanel().removeAllBGColors();
    }

    public void increaseTextFontSize() {
        getTextPanel().increaseFontSize();

    }

    public void decreaseTextFontSize() {
        getTextPanel().decreaseFontSize();

    }

    public void resetFontSize() {
        getTextPanel().resetFontSize();

    }

    public void setAsArgumentFromDialog(String argumentTid) {
        try {
            getDialogs().setAsArgument(argumentTid);
        } catch (MaeDBException e) {
            showError(e);
        }

    }

    public LinkTag createLinkFromDialog(TagType linkType, List<ExtentTag> candidates) {
        try {
            return getDialogs().createLink(linkType, candidates);
        } catch (MaeDBException e) {
            showError(e);
            return null;
        }
    }

    public void propagateSelectionFromTextPanel() {
        getTablePanel().clearTableSelections();
        try {
            List<ExtentTag> releventTags = getExtentTagsInSelectedSpans();
            for (ExtentTag tag : releventTags) {
                getTablePanel().selectTagFromTable(tag);
            }
            updateNotificationArea();
        } catch (Exception e) {
            showError(e);
        }
    }
    public void propagateSelectionFromTablePanel(String tid) {
        removeAllBGColors();
        try {
            addBGColorOver(getDriver().getAnchorsByTid(tid), ColorHandler.getVividHighliter());
        } catch (Exception e) {
            showError(e);
        }
    }

    public JPopupMenu createTableContextMenu(JTable table) {
        logger.debug("creating context menu from table panel");
        try {
            return getMenu().createTableContextMenu(table);
        } catch (MaeDBException e) {
            showError(e);
        }
        return null;
    }

    public JPopupMenu createTextContextMenu() {
        logger.debug("creating context menu from text panel");
        try {
            return getMenu().createTextContextMenu();
        } catch (MaeDBException e) {
            showError(e);
        }
        return null;
    }

    public void removeTag(Tag tag) {
        try {
            getTablePanel().removeTagFromTable(tag);
        } catch (MaeDBException e) {
            showError(e);
        }
    }

    public Tag createTagFromMenu(TagType tagType) {

        boolean nc = getSelectedTextSpans() == null || getSelectedTextSpans().length == 0;
        String message;
        if (tagType.isLink()) {
            message = String.format("creating DB row for am yet-empty Link tag: (%s)", tagType.getName());
        } else if (nc) {
            message = String.format("creating DB row for a NC extent tag: (%s)", tagType.getName());
        } else {
            message = String.format("creating DB row from text selection: (%s) \"%s\"", tagType.getName(), getSelectedText());
        }
        logger.info(message);
        try {
            Tag tag;
            String tid = getDriver().getNextId(tagType);
            if (tagType.isLink()) {
                tag = getDriver().createLinkTag(tid, tagType);
                // creating a link from text popup will always end up in an empty link,
                // no need to populate or repaint its arguments
            } else if (nc) {
                tag = getDriver().createExtentTag(tid, tagType, null, null);
            } else {
                tag = getDriver().createExtentTag(tid, tagType, getSelectedText(), getSelectedTextSpans());
            }
            populateDefaultAttributes(tag);
            getTablePanel().insertTagIntoTable(tag);
            selectTagAndTable(tag);
            updateSavedStatusInTextPanel();
            if (tagType.isExtent()) {
                assignTextColorsOver(((ExtentTag) tag).getSpansAsList());
            }
            if (normalModeOnCreation()) {
                switchToNormalMode();
            }
            return tag;
        } catch (MaeException e) {
            showError(e);
        }
        return  null;
    }

    public void selectTagAndTable(Tag tag) {
        try {
            getTablePanel().selectTagFromTable(tag);
            getTablePanel().selectTabOf(tag.getTagtype());
            propagateSelectionFromTablePanel(tag.getId());
        } catch (MaeDBException e) {
            showError(e);
        }
    }

    void populateDefaultAttributes(Tag tag) throws MaeDBException {
        for (AttributeType attType : tag.getTagtype().getAttributeTypes()) {
            String defaultValue = attType.getDefaultValue();
            if (defaultValue != null && defaultValue.length() > 0) {
                getDriver().addAttribute(tag, attType, defaultValue);
            }
        }
    }

    public void deleteTagFromTableDeletion(Tag tag) {
        logger.debug(String.format("removing DB row based on table deletion: \"%s\"", tag.getId()));
        try {
            getDriver().deleteTag(tag);
            if (tag.getTagtype().isExtent()) {
                assignTextColorsOver(((ExtentTag) tag).getSpansAsList());
            } else {
                for (ExtentTag arg : ((LinkTag) tag).getArgumentTags()) {
                    assignTextColorsOver(arg.getSpansAsList());
                }
            }
            updateSavedStatusInTextPanel();
        } catch (MaeDBException e) {
            showError(e);
        }
    }

    public boolean updateDBFromTableUpdate(String tid, String colName, String value) {
        logger.debug(String.format("modifying DB based on table update: updating \"%s\" of %s to \"%s\"", colName, tid, value));
        boolean succeed = false;
        try {
            Tag tag = getTagByTid(tid);
            if (tag.getTagtype().isExtent() && colName.equals(MaeStrings.SPANS_COL_NAME)) {
                succeed = getDriver().updateTagSpans((ExtentTag) tag, SpanHandler.convertStringToArray(value));
            } else if (tag.getTagtype().isExtent() && colName.equals(MaeStrings.TEXT_COL_NAME)) {
                succeed = getDriver().updateTagText((ExtentTag) tag, value);
            } else if (tag.getTagtype().isLink() && colName.endsWith(MaeStrings.ARG_IDCOL_SUF)) {
                String argTypeName = colName.substring(0, colName.length() - MaeStrings.ARG_IDCOL_SUF.length());
                ArgumentType argType = getDriver().getArgumentTypeOfTagTypeByName(tag.getTagtype(), argTypeName);
                LinkTag linker = (LinkTag) getTagByTid(tid);
                ExtentTag arg = (ExtentTag) getTagByTid(value);
                if (arg == null) {
                    showError("Argument not found: " + value);
                    return false;
                }
                succeed = (getDriver().addOrUpdateArgument(linker, argType, arg) != null);
            } else if (tag.getTagtype().isLink() && colName.endsWith(MaeStrings.ARG_TEXTCOL_SUF)) {
                // do nothing, will be automatically updated when argId is updated
                return true;
            } else {
                AttributeType attType = getDriver().getAttributeTypeOfTagTypeByName(tag.getTagtype(), colName);
                succeed = (getDriver().updateAttribute(tag, attType, value) != null);
            }
        } catch (MaeException e) {
            showError(e);
        }
        if (succeed) {
            updateSavedStatusInTextPanel();
        }
        return succeed;
    }

    public void surgicallyUpdateCell(Tag tag, String colName, String value) {
        getTablePanel().insertValueIntoCell(tag, colName, value);
    }

    public Tag getTagByTid(String tid) {
        try {
            return getDriver().getTagByTid(tid);
        } catch (MaeDBException e) {
            showError(e);
        }
        return null;
    }

    public Set<Tag> checkCompleteness() {
        try {
            Set<Tag> incomplete = new TreeSet<>();
            for (TagType type : getDriver().getAllTagTypes()) {
                if (type.isExtent()) {
                    for (ExtentTag tag : getDriver().getAllExtentTagsOfType(type)) {
                        if (!tag.isComplete()) {
                            incomplete.add(tag);
                        }
                    }
                } else {
                    for (LinkTag tag : getDriver().getAllLinkTagsOfType(type)) {
                        if (!tag.isComplete()) {
                            incomplete.add(tag);
                        }
                    }
                }
            }
            return incomplete;
        } catch (MaeDBException e) {
            showError(e);
        }
        return null;
    }

    public boolean showIncompleteTagsWarning() {
        return getDialogs().showIncompleteTagsWarning(checkCompleteness());
    }

}

