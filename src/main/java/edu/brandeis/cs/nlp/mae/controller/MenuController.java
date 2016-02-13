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
import edu.brandeis.cs.nlp.mae.controller.action.*;
import edu.brandeis.cs.nlp.mae.database.MaeDBException;
import edu.brandeis.cs.nlp.mae.database.MaeDriverI;
import edu.brandeis.cs.nlp.mae.model.ExtentTag;
import edu.brandeis.cs.nlp.mae.model.Tag;
import edu.brandeis.cs.nlp.mae.model.TagType;
import edu.brandeis.cs.nlp.mae.view.TablePanelView;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import static edu.brandeis.cs.nlp.mae.MaeHotKeys.*;
import static edu.brandeis.cs.nlp.mae.MaeStrings.*;
import static edu.brandeis.cs.nlp.mae.controller.MaeMainController.*;

/**
 * Created by krim on 1/2/2016.
 */
class MenuController extends MaeControllerI {

    private static final int CAT_ETAG = 0;
    private static final int CAT_NCTAG = 1;
    private static final int CAT_EMPTY_LTAG = 2;
    private static final int CAT_LTAG = 3;
    private static final int CAT_LTAG_FROM_TABLE = 4;
    private static final int DELETE_MENU = 0;
    private static final int SETARG_MENU = 1;
    private static final int COPY_MENU = 2;

    private static final int MENUBAR_FILE = 0;
    private static final int MENUBAR_TAGS = 1;
    private static final int MENUBAR_MODE = 2;
    private static final int MENUBAR_DISPLAY = 3;
    private static final int MENUBAR_HELP = 4;

    // this controller is responsible for all these menus
    JMenu fileMenu;
    JMenu tagsMenu;
    JMenu displayMenu;
    JMenu helpMenu;
    JMenu modeMenu;
    JMenu preferenceMenu;

    JMenu[] menubarOrder = new JMenu[10];

    // and this view for top main menu
    private JMenuBar menubar;

    MenuController(MaeMainController mainController) throws MaeDBException {
        super(mainController);
        view = new JPanel(new BorderLayout());
        menubar = new JMenuBar();
        initMenubar();
        view.add(menubar, BorderLayout.CENTER);

    }

    void initMenubar() throws MaeDBException {

        menubar.removeAll();

        fileMenu = prepareFileMenu();
        tagsMenu = prepareTagsMenu();
        modeMenu = prepareModeMenu();
        displayMenu = prepareDisplayMenu();
        helpMenu = prepareHelpMenu();

        menubarOrder[MENUBAR_FILE] = fileMenu;
        menubarOrder[MENUBAR_TAGS] = tagsMenu;
        menubarOrder[MENUBAR_MODE] = modeMenu;
        menubarOrder[MENUBAR_DISPLAY] = displayMenu;
        menubarOrder[MENUBAR_HELP] = helpMenu;

        boolean isDocumentOpen = getMainController().isDocumentOpen();
        for (JMenu menu : menubarOrder) {
            if (menu != null) {
                menubar.add(menu);
            } else {
                break;
            }
        }

        tagsMenu.setEnabled(isDocumentOpen);
        modeMenu.setEnabled(isDocumentOpen);
        view.updateUI();

    }

    @Override
    void addListeners() throws MaeException {
        // no listeners involved

    }

    void resetAMenu(int menuIndex) {
        JMenu menu = menubarOrder[menuIndex];
        menubar.remove(menu);
        menu = prepareAMenu(menuIndex);
        menubarOrder[menuIndex] = menu;
        menubar.add(menu, menuIndex);
        view.updateUI();

    }
    void resetFileMenu() {
        resetAMenu(MENUBAR_FILE);
    }

    void resetTagsMenu() {
        resetAMenu(MENUBAR_TAGS);
    }

    void resetModeMenu() {
        resetAMenu(MENUBAR_MODE);
    }

    private JMenu prepareAMenu(int menuIndex) {
        switch (menuIndex) {
            case MENUBAR_FILE:
                return prepareFileMenu();
            case MENUBAR_MODE:
                return prepareModeMenu();
            case MENUBAR_DISPLAY:
                return prepareDisplayMenu();
            case MENUBAR_HELP:
                return prepareHelpMenu();
            case MENUBAR_TAGS:
                return prepareTagsMenu();
            default:
                logger.warn("not defined menu number: " + menuIndex);
                return null;
        }
    }

    private JMenu prepareFileMenu() {
        MaeActionI loadTaskAction = new LoadTask(MENUITEM_LOADTASK, null, ksLOADTASK, null, getMainController());
        MaeActionI openFileAction = new OpenFile(MENUITEM_OPENFILE, null, ksOPENFILE, null, getMainController());
        MaeActionI saveXMLAction = new SaveXML(MENUITEM_SAVEXML, null, ksSAVEXML, null, getMainController());
        MaeActionI closeFileAction = new CloseFile(MENUITEM_CLOSEFILE, null, ksCLOSEFILE, null, getMainController());
        String adjudItemLabel = getMainController().isAdjudicating()?
                MENUITEM_END_ADJUD
                : MENUITEM_START_ADJUD;
        String adjudItemCmd = getMainController().isAdjudicating()?
                Integer.toString(END_ADJUD)
                : Integer.toString(START_ADJUD);
        MaeActionI adjudModeAction = new ModeSwitch(adjudItemLabel, null, ksADJUDMODE, null, getMainController());

        JMenu menu = new JMenu(MENU_FILE);
        menu.setMnemonic(MENU_FILE.charAt(0));

        JMenuItem loadTask = new JMenuItem(loadTaskAction);
        JMenuItem openFile = new JMenuItem(openFileAction);
        JMenuItem saveXML = new JMenuItem(saveXMLAction);
        JMenuItem closeFile = new JMenuItem(closeFileAction);
        JMenuItem adjudMode = new JMenuItem(adjudModeAction);
        adjudMode.setActionCommand(adjudItemCmd);
        boolean taskLoaded = getMainController().isTaskLoaded();
        boolean fileLoaded = getMainController().isDocumentOpen();
        openFile.setEnabled(taskLoaded);
        saveXML.setEnabled(fileLoaded);
        closeFile.setEnabled(fileLoaded);

        menu.add(loadTask);
        menu.add(openFile);
        menu.addSeparator();
        menu.add(saveXML);
        menu.addSeparator();
        menu.add(closeFile);
        menu.addSeparator();
        menu.add(adjudMode);
        logger.debug("file menu is created: " + menu.getItemCount());
        return menu;
    }

    private JMenu prepareTagsMenu() {
        JMenu menu = new JMenu(MENU_TAGS);
        menu.setMnemonic(MENU_TAGS.charAt(0));

        if (getMainController().isDocumentOpen()) {
            menu.add(createMakeTagMenu(CAT_NCTAG));
            menu.add(createMakeTagMenu(CAT_EMPTY_LTAG));
        } else {
            JMenuItem documentNotOpen = new JCheckBoxMenuItem("Open a document to manage tags");
            documentNotOpen.setEnabled(false);
            menu.add(documentNotOpen);
        }
        logger.debug("tags menu is created: " + menu.getItemCount());
        return menu;

    }

    private JMenu prepareModeMenu() {
        JMenu menu = new JMenu(MENU_MODE);
        menu.setMnemonic(MENU_MODE.charAt(0));

        if (getMainController().isDocumentOpen()) {
            MaeActionI multiSpanModeAction = new ModeSwitch(MENUITEM_MSPAN_MODE, null, ksMSPANMODE, null, getMainController());
            MaeActionI argSelModeAction = new ModeSwitch(MENUITEM_ARGSEL_MODE, null, ksARGSMODE, null, getMainController());
            MaeActionI normalModeAction = new ModeSwitch(MENUITEM_NORMAL_MODE, null, ksNORMALMODE, null, getMainController());

            JMenuItem multiSpanMode = new JMenuItem(multiSpanModeAction);
            multiSpanMode.setActionCommand(Integer.toString(MODE_MULTI_SPAN));
            JMenuItem argSelMode = new JMenuItem(argSelModeAction);
            argSelMode.setActionCommand(Integer.toString(MODE_ARG_SEL));
            JMenuItem normalMode = new JMenuItem(normalModeAction);
            normalMode.setActionCommand(Integer.toString(MODE_NORMAL));
            switch (getMainController().getMode()) {
                case MODE_NORMAL:
                    normalMode.setEnabled(false);
                    break;
                case MODE_MULTI_SPAN:
                    multiSpanMode.setEnabled(false);
                    break;
                case MODE_ARG_SEL:
                    argSelMode.setEnabled(false);
                    break;
            }

            menu.add(multiSpanMode);
            menu.add(argSelMode);
            menu.addSeparator();
            menu.add(normalMode);
        } else {
            JMenuItem documentNotOpen = new JCheckBoxMenuItem("Modes ");
            documentNotOpen.setEnabled(false);
            menu.add(documentNotOpen);
        }
        logger.debug("mode menu is created: " + menu.getItemCount());
        return menu;
    }

    private JMenu prepareHelpMenu() {
        MaeActionI aboutAction = new About(MENUITEM_ABOUT, null, ksABOUT, null, getMainController());
        MaeActionI visitWebsiteAction = new VisitWebsite(MENUITEM_WEB, null, ksWEB, null, getMainController());

        JMenu menu = new JMenu(MENU_HELP);
        menu.setMnemonic(MENU_HELP.charAt(0));

        JMenuItem about = new JMenuItem(aboutAction);
        JMenuItem visitWebsite = new JMenuItem(visitWebsiteAction);

        menu.add(about);
        menu.add(visitWebsite);
        logger.debug("help menu is created: " + menu.getItemCount());
        return menu;
    }

    private JMenu prepareDisplayMenu() {
        MaeActionI increaseFontSizeAction = new ChangeFontsize(MENUITEM_ZOOMIN, null, ksZOOMIN, null, getMainController());
        MaeActionI decreaseFontSizeAction = new ChangeFontsize(MENUITEM_ZOOMOUT, null, ksZOOMOUT, null, getMainController());
        MaeActionI resetFontSizeAction = new ChangeFontsize(MENUITEM_RESET_ZOOM, null, ksRESETZOOM, null, getMainController());

        JMenu menu = new JMenu(MENU_DISPLAY);
        menu.setMnemonic(MENU_DISPLAY.charAt(0));

        JMenuItem increaseFontSize = new JMenuItem(increaseFontSizeAction);
        increaseFontSize.setActionCommand("+");
        JMenuItem decreaseFontSize = new JMenuItem(decreaseFontSizeAction);
        decreaseFontSize.setActionCommand("-");
        JMenuItem resetFontSize = new JMenuItem(resetFontSizeAction);
        resetFontSize.setActionCommand("0");

        menu.add(increaseFontSize);
        menu.add(decreaseFontSize);
        menu.add(resetFontSize);
        logger.debug("display menu is created: " + menu.getItemCount());
        return menu;
    }


    JPopupMenu createTextContextMenu() throws MaeDBException {

        List<ExtentTag> tags = getMainController().getExtentTagsInSelectedSpans();
        JPopupMenu contextMenu = new JPopupMenu();

        int mode = getMainController().getMode();
        if (getMainController().isTextSelected()) {
            TagType currentType = getMainController().getAdjudicatingTagType();
            if (getMainController().isAdjudicating()
                    && mode != MODE_ARG_SEL
                    && currentType.isExtent()) {
                contextMenu.add(createSingleTypeMakeTagMenu());
                List<ExtentTag> tagsFromAllDocs = getMainController().getExtentTagsFromAllDocumentsInSelectedSpans();
                tagsFromAllDocs.removeAll(tags);
                List<ExtentTag> interestedTags = new LinkedList<>();
                for (ExtentTag tag : tagsFromAllDocs) {
                    if (tag.getTagtype().equals(currentType)) {
                        interestedTags.add(tag);
                    }
                }
                switch (interestedTags.size()) {
                    case 0:
                        break;
                    case 1:
                        contextMenu.add(createSingleCopyMenu(tagsFromAllDocs.get(0)));
                        break;
                    default:
                        contextMenu.add(createPluralCopyMenu(tagsFromAllDocs));
                }
            } else if (mode != MODE_ARG_SEL) {
                contextMenu.add(createMakeTagMenu(CAT_ETAG));
            } else if (getMainController().getSelectedArguments() != null && getMainController().getSelectedArguments().size() > 0) {
                contextMenu.add(createMakeTagMenu(CAT_LTAG));
            }
        }
        if (!getMainController().isAdjudicating()) {
            // to eliminate unnecessary degree of freedom
            if (contextMenu.getComponentCount() > 0) {
                contextMenu.addSeparator();
            }
            contextMenu.add(createMakeTagMenu(CAT_NCTAG));
            contextMenu.add(createMakeTagMenu(CAT_EMPTY_LTAG));
        }
        switch (tags.size()) {
            case 0:
                break;
            case 1:
                contextMenu.addSeparator();
                contextMenu.add(createSingleDeleteMenu(tags.get(0)));
                contextMenu.add(createSingleSetArgMenu(tags.get(0)));
                break;
            default:
                contextMenu.addSeparator();
                contextMenu.add(createPluralDeleteMenu(tags));
                contextMenu.add(createPluralSetArgMenu(tags));

        }
        if (mode != MODE_NORMAL) {
            contextMenu.addSeparator();
            contextMenu.add(createUndoLastSelectionMenu());
            contextMenu.add(createStartOverMenu());
        }
        if (contextMenu.getComponentCount() == 0) {
            JMenuItem noItem = new JMenuItem("No action available");
            contextMenu.add(noItem);
            noItem.setEnabled(false);
        }

        return contextMenu;

    }

    private JMenuItem createUndoLastSelectionMenu() {
        return new JMenuItem(new UndoLastSelection(MENUITEM_UNDOSELECTION, null, ksUNDO, cmnUNDO, getMainController()));
    }

    private JMenuItem createStartOverMenu() {
        return new JMenuItem(new ResetSelection(MENUITEM_STARTOVER, null, null, cmnSTARTOVER, getMainController()));
    }

    private JMenuItem createSingleCopyMenu(Tag tag) {
        return createCopyMenuItem(tag, String.format(MaeStrings.MENUITEM_COPY_TAG_SINGLE, tag.toString(), tag.getFilename()), cmnCOPY);
    }

    private JMenu createPluralCopyMenu(List<ExtentTag> tags) {
        JMenu copyMenu = new JMenu(MENU_COPY);
        copyMenu.setMnemonic(cmnCOPY);
        // this will assign hotkey
        createMenuItemsWithNumberMnemonics(tags, MENUITEM_COPY_TAG_SINGLE, copyMenu, 10, COPY_MENU);

        return copyMenu;
    }

    private JMenuItem createCopyMenuItem(Tag tag, String label, int mnemonic) {
        MaeActionI copyTagAction = getCopyTagAction(label, mnemonic);
        JMenuItem copyTagItem = new JMenuItem(copyTagAction);
        copyTagItem.setActionCommand(tag.getFilename() + MaeStrings.SEP + tag.getId());
        return copyTagItem;
    }

    private MaeActionI getCopyTagAction(String copyTagLabel, int mnemonic) {
        return new CopyTag(copyTagLabel, null, null, mnemonic, getMainController());

    }


    private JMenuItem createSingleTypeMakeTagMenu() {
        TagType type = getMainController().getAdjudicatingTagType();
        MaeActionI makeTagAction = new MakeTag(String.format(MaeStrings.MENUITEM_CREATE_CERTAIN_ETAG, type.getName()), null, null, cmnCREATE, getMainController());
        JMenuItem makeTagItem = new JMenuItem(makeTagAction);
        makeTagItem.setActionCommand(type.getName());
        return makeTagItem;
    }

    private JMenu createMakeTagMenu(int category) {
        JMenu makeTagMenu = new JMenu(getMakeTagMenuLabel(category));
        makeTagMenu.setMnemonic(getMakeTagMenuMnemonic(category));
        List<TagType> types = null;
        try {
            types = getTagTypes(category);
        } catch (MaeDBException e) {
            getMainController().showError("DB error occurred while creating menus");
        }

        int typeCount = 0;
        for (TagType type : types) {
            JMenuItem makeTagItem = new JMenuItem(getMakeTagAction(category, typeCount++, type));
            makeTagItem.setActionCommand(type.getName());
            makeTagMenu.add(makeTagItem);
        }
        return makeTagMenu;
    }

    private int getMakeTagMenuMnemonic(int category) {
        switch (category) {
            case CAT_NCTAG:
                return cmnCREATENC;
            case CAT_EMPTY_LTAG:
                return cmnCREATELINK;
            default:
                return cmnCREATE;
        }
    }

    private MaeActionI getMakeTagAction(int category, int mnemonicNum, TagType type) {
        String makeTagItemLabel;
        Integer mnemonic;
        if (mnemonicNum < 10) {
            makeTagItemLabel = String.format("(%d) %s", mnemonicNum + 1, type.getName());
            mnemonic = numKeys[mnemonicNum];
        } else {
            makeTagItemLabel = String.format("    %s", type.getName());
            mnemonic = null;
        }
        switch (category) {
            case CAT_LTAG:
                return new MakeLink(makeTagItemLabel, null, null, mnemonic, getMainController());
            case CAT_LTAG_FROM_TABLE:
                return new MakeLinkFromTable(makeTagItemLabel, null, null, mnemonic, getMainController());
            default:
                return new MakeTag(makeTagItemLabel, null, null, mnemonic, getMainController());
        }
    }

    private String getMakeTagMenuLabel(int category) {
        switch (category) {
            case CAT_ETAG:
                return MENUITEM_CREATE_ETAG;
            case CAT_NCTAG:
                return MENUITEM_CREATE_NCTAG;
            case CAT_EMPTY_LTAG:
                return MENUITEM_CREATE_LTAG_EMPTY;
            case CAT_LTAG:
                return MENUITEM_CREATE_LTAG_FROM_SEL;
        }
        return null;
    }

    private List<TagType> getTagTypes(int category) throws MaeDBException {
        switch (category) {
            case CAT_ETAG:
                return getDriver().getExtentTagTypes();
            case CAT_NCTAG:
                List<TagType> ncTypes = new ArrayList<>();
                for (TagType type : getDriver().getExtentTagTypes()) {
                    if (type.isNonConsuming()) ncTypes.add(type);
                }
                return ncTypes;
            default:
                return getDriver().getLinkTagTypes();
        }
    }

    JMenuItem createSingleDeleteMenu(Tag tag) throws MaeDBException {
        return createDeleteMenuItem(tag, String.format(MENUITEM_DELETE_TAG_SINGLE, tag.toString()), cmnDELETE);
    }

    JMenu createPluralDeleteMenu(List<? extends Tag> tags) throws MaeDBException {
        JMenu deleteMenu = new JMenu(MENU_DELETE_TAG);
        deleteMenu.setMnemonic(cmnDELETE);
        deleteMenu.add(createWholeDeleteMenuItem(tags, "(0) " + String.format(MENUITEM_DELETE_TAG_PLURAL, tags.size(), tags.toString()), n0));

        // this will assign hotkey
        createMenuItemsWithNumberMnemonics(tags, MENUITEM_DELETE_TAG_SINGLE, deleteMenu, 9, DELETE_MENU);

        return deleteMenu;
    }

    private JMenuItem createDeleteMenuItem(Tag tag, String label, int mnemonic) {
        MaeActionI deleteTagAction = getDeleteTagAction(label, mnemonic);
        JMenuItem deleteTagItem = new JMenuItem(deleteTagAction);
        deleteTagItem.setActionCommand(tag.getId());
        return deleteTagItem;
    }

    private JMenuItem createWholeDeleteMenuItem(List<? extends Tag> tags, String label, int mnemonic) {
        String tids = "";
        for (Tag tag : tags) {
            tids += tag.getId() + SEP;
        }
        MaeActionI deleteTagAction = new DeleteTag(label, null, null, mnemonic, getMainController());
        JMenuItem deleteTagItem = new JMenuItem(deleteTagAction);
        deleteTagItem.setActionCommand(tids);
        return deleteTagItem;
    }

    private MaeActionI getDeleteTagAction(String deleteTagLabel, int mnemonic) {
        return new DeleteTag(deleteTagLabel, null, null, mnemonic, getMainController());

    }

    JMenuItem createSingleSetArgMenu(ExtentTag tag) {
        return createSetArgMenuItem(tag, String.format(MENUITEM_SETARG_SINGLE, tag.toString()), cmnSETARG);

    }

    private JMenuItem createSetArgMenuItem(Tag tag, String label, int mnemonic) {
        MaeActionI setArgAction = getSetArgTagAction(label, mnemonic);
        JMenuItem setArgItem = new JMenuItem(setArgAction);
        setArgItem.setActionCommand(tag.getId());
        return setArgItem;
    }

    private MaeActionI getSetArgTagAction(String label, int mnemonic) {
        return new SetArgument(label, null, null, mnemonic, getMainController());

    }

    JMenu createPluralSetArgMenu(List<? extends Tag> tags) throws MaeDBException {
        JMenu setArgMenu = new JMenu(MENU_SETARG);
        setArgMenu.setMnemonic(cmnSETARG);
        createMenuItemsWithNumberMnemonics(tags, MENUITEM_SETARG_SINGLE, setArgMenu, 10, SETARG_MENU);
        return setArgMenu;
    }

    private void createMenuItemsWithNumberMnemonics(List<? extends Tag> tags, String labelTemplate, JMenu menu, int endPoint, int menuType) {
        String label;
        Integer mnemonic;
        int i = 0;
        for (Tag tag : tags) {
            String labelWithoutMnemonic = String.format(labelTemplate, tag.toString(), tag.getFilename());
            if (i < endPoint) {
                label = String.format("(%d) %s", i + 1, labelWithoutMnemonic);
                mnemonic = numKeys[i++];
            } else {
                label = String.format("    %s", labelWithoutMnemonic);
                mnemonic = null;
            }
            switch (menuType) {
                case DELETE_MENU:
                    menu.add(createDeleteMenuItem(tag, label, mnemonic));
                    break;
                case SETARG_MENU:
                    menu.add(createSetArgMenuItem(tag, label, mnemonic));
                    break;
                case COPY_MENU:
                    menu.add(createCopyMenuItem(tag, label, mnemonic));
            }
        }
    }

    JPopupMenu createTableContextMenu(JTable table) throws MaeDBException {

        int selected = table.getSelectedRowCount();

        String rowS = selected == 1 ? "row" : "rows";
        JPopupMenu contextMenu = new JPopupMenu(String.format("%d %s selected", selected, rowS));
        TablePanelController.TagTableModel model = (TablePanelController.TagTableModel) table.getModel();

        int selectedRow = table.getSelectedRow();
        if (!getMainController().isAdjudicating()) {
            if (selected == 1) {
                prepareTableContextMenuForSingleSelection(contextMenu, model, selectedRow);
            } else {
                contextMenu.add(createPluralDeleteMenu(model, table.getSelectedRows()));
                if (model.getAssociatedTagType().isExtent()) {
                    contextMenu.add(createMakeLinkFromTableMenu(model, table.getSelectedRows()));
                }
            }
        } else { // multi row selection is disabled in adjudication
            String srcFileName = (String) table.getValueAt(selectedRow, TablePanelController.SRC_COL);
            if (srcFileName.equals(getMainController().getDriver().getAnnotationFileBaseName())) {
                prepareTableContextMenuForSingleSelection(contextMenu, model, selectedRow);

            } else {
                String tid = (String) table.getValueAt(selectedRow, TablePanelController.ID_COL);
                MaeDriverI driver = getMainController().getDriverOf(srcFileName);
                Tag tag = driver.getTagByTid(tid);
                contextMenu.add(createSingleCopyMenu(tag));
            }
        }

        return contextMenu;

    }

    private void prepareTableContextMenuForSingleSelection(JPopupMenu contextMenu, TablePanelController.TagTableModel model, int selectedRow) throws MaeDBException {
        contextMenu.add(createSingleDeleteMenu(model, selectedRow));
        if (model.getAssociatedTagType().isExtent()) {
            contextMenu.add(createSingleSetArgMenu(model, selectedRow));
        }
    }

    private JMenuItem createSingleDeleteMenu(TablePanelController.TagTableModel model, int selectedRow) throws MaeDBException {
        String tid = (String) model.getValueAt(selectedRow, TablePanelController.ID_COL);
        Tag tag = getDriver().getTagByTid(tid);
        return createSingleDeleteMenu(tag);
    }

    private JMenu createPluralDeleteMenu(TablePanelController.TagTableModel model, int[] selectedRows) throws MaeDBException {
        List<Tag> tags = new LinkedList<>();
        for (int row : selectedRows) {
            tags.add(getDriver().getTagByTid((String) model.getValueAt(row, TablePanelController.ID_COL)));
        }
        return createPluralDeleteMenu(tags);
    }

    JMenu createMakeLinkFromTableMenu(TablePanelController.TagTableModel model, int[] selectedRows) throws MaeDBException {
        JMenu makeLinkFromTableMenu = new JMenu(MENUITEM_CREATE_LTAG_FROM_SEL);
        String tids = MaeStrings.SEP;
        for (int row : selectedRows) {
            tids += model.getValueAt(row, TablePanelController.ID_COL) + MaeStrings.SEP;
        }
        int typeCount = 0;
        for (TagType linkType : getTagTypes(CAT_LTAG)) {
            JMenuItem makeLinkFromTableItem = new JMenuItem(getMakeTagAction(CAT_LTAG_FROM_TABLE, typeCount, linkType));
            makeLinkFromTableItem.setActionCommand(linkType.getName() + tids);
            makeLinkFromTableMenu.add(makeLinkFromTableItem);
        }
        return makeLinkFromTableMenu;

    }

    private JMenuItem createSingleSetArgMenu(TablePanelController.TagTableModel model, int selectedRow) throws MaeDBException {
        String tid = (String) model.getValueAt(selectedRow, TablePanelController.ID_COL);
        Tag tag = getDriver().getTagByTid(tid);
        return createSingleSetArgMenu((ExtentTag) tag);
    }

//    private JMenuItem createSingleCopyToGSMenu(TablePanelController.TagTableModel model, int selectedRow) throws MaeDBException {
//        String tid = (String) model.getValueAt(selectedRow, TablePanelController.ID_COL);
//        String source =
//    }
}
