package de.hahn.blog.tableselect.view;

import java.util.ArrayList;
import java.util.List;

import javax.el.ELContext;
import javax.el.ExpressionFactory;
import javax.el.MethodExpression;

import javax.faces.application.Application;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;

import javax.servlet.ServletContext;

import oracle.adf.model.BindingContext;
import oracle.adf.model.binding.DCIteratorBinding;
import oracle.adf.share.logging.ADFLogger;
import oracle.adf.view.rich.component.rich.data.RichTable;
import oracle.adf.view.rich.component.rich.layout.RichPanelFormLayout;
import oracle.adf.view.rich.component.rich.nav.RichButton;
import oracle.adf.view.rich.context.AdfFacesContext;

import oracle.binding.BindingContainer;
import oracle.binding.OperationBinding;

import oracle.jbo.Key;
import oracle.jbo.NavigatableRowIterator;
import oracle.jbo.Row;
import oracle.jbo.uicli.binding.JUCtrlHierBinding;
import oracle.jbo.uicli.binding.JUCtrlHierNodeBinding;

import org.apache.myfaces.trinidad.event.SelectionEvent;
import org.apache.myfaces.trinidad.model.CollectionModel;
import org.apache.myfaces.trinidad.model.RowKeySet;
import org.apache.myfaces.trinidad.model.RowKeySetImpl;
import org.apache.myfaces.trinidad.util.ComponentReference;

public class TableSelectBean {
    private static ADFLogger log = ADFLogger.createADFLogger(TableSelectBean.class);
    private ComponentReference<RichPanelFormLayout> pform;
    private ComponentReference<RichTable> regionTable;
    private ComponentReference<RichTable> countriesTable;
    private String id;
    private String name;
    private static String[] ITERATORS = { "RegionsView1Iterator", "CountriesView1Iterator" };
    String pageloadTrigger = "false";
    int currentIteratorIndex = -1;


    public void setId(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public TableSelectBean() {
    }

    public void onTableSelect2(SelectionEvent selectionEvent) {
        RichTable _table = (RichTable) selectionEvent.getSource();
        //the Collection Model is the object that provides the
        //structured data
        //for the table to render
        CollectionModel _tableModel = (CollectionModel) _table.getValue();
        //the ADF object that implements the CollectionModel is
        //JUCtrlHierBinding. It is wrapped by the CollectionModel API
        JUCtrlHierBinding _adfTableBinding = (JUCtrlHierBinding) _tableModel.getWrappedData();
        //Acess the ADF iterator binding that is used with
        //ADF table binding
        DCIteratorBinding _tableIteratorBinding = _adfTableBinding.getDCIteratorBinding();

        int newIndex = getIndexFromiteratorName(_tableIteratorBinding.getName());
        if (currentIteratorIndex != newIndex) {
            log.info("Switching tables from " + currentIteratorIndex + " to " + newIndex);
            // clear selection of old table nad set selection in new table
            RowKeySet emptySet = createRowKeySet(null);
            RichTable oldTable = getTableFromIndex(currentIteratorIndex);
            oldTable.setSelectedRowKeys(emptySet);
            currentIteratorIndex = newIndex;
            AdfFacesContext.getCurrentInstance().addPartialTarget(oldTable);
        }

        if (true) {
            _table.setSelectedRowKeys(selectionEvent.getAddedSet());
        }
        //the role of this method is to synchronize the table component
        //selection with the selection in the ADF model
        Object _selectedRowData = _table.getSelectedRowData();
        //cast to JUCtrlHierNodeBinding, which is the ADF object
        //that represents a row
        JUCtrlHierNodeBinding _nodeBinding = (JUCtrlHierNodeBinding) _selectedRowData;
        //get the row key from the node binding and set it
        //as the current row in the iterator
        Key _rwKey = _nodeBinding.getRowKey();
        Row row = _nodeBinding.getRow();
        openPosition(row);
        _tableIteratorBinding.setCurrentRowWithKey(_rwKey.toStringFormat(true));
    }


    /**
     * Method that creates and executes a MethodExpression for the provided EL * string
     * @param methodExpression java.lang.String that represents a valid EL
     * string
     * @param parameters An array of Objects or null that provides the
     * arguments for the executed method
     * @param expectedParamTypes An array of <ClassType>.class or empty array
     * describing the types of the expected parameters
     * @return Object returned by the invoked method
     */
    private Object invokeMethodExpression(String methodExpression, Object[] parameters, Class[] expectedParamTypes) {
        FacesContext fctx = FacesContext.getCurrentInstance();
        ELContext elctx = fctx.getELContext();
        Application app = fctx.getApplication();
        ExpressionFactory exprFactory = app.getExpressionFactory();
        MethodExpression methodExpr =
            exprFactory.createMethodExpression(elctx, methodExpression, Object.class, expectedParamTypes);
        return methodExpr.invoke(elctx, parameters);
    }

    public void setPform(RichPanelFormLayout pform) {
        this.pform = ComponentReference.newUIComponentReference(pform);
    }

    public RichPanelFormLayout getPform() {
        if (null != pform) {
            return pform.getComponent();
        }
        return null;
    }

    public void setRegionTable(RichTable regionTable) {
        this.regionTable = ComponentReference.newUIComponentReference(regionTable);
    }

    public RichTable getRegionTable() {
        if (null != regionTable) {
            return regionTable.getComponent();
        }
        return null;
    }

    public void onNavigation(ActionEvent actionEvent) {
        RichButton navButton = (RichButton) actionEvent.getSource();
        //Get ServlerContexct
        FacesContext ctx = FacesContext.getCurrentInstance();
        ServletContext servletContext = (ServletContext) ctx.getExternalContext().getContext();
        // get the binding container
        BindingContainer bindings = BindingContext.getCurrent().getCurrentBindingsEntry();
        //f:attribute set
        //aktuelle Row holen um first last zu erkennen
        DCIteratorBinding iter = getIteratorFromIndex(currentIteratorIndex);
        Row currentRow = iter.getCurrentRow();
        String adfAction = (String) navButton.getAttributes().get("adfAction");
        String operation = adfAction + ITERATORS[currentIteratorIndex];

        log.info("executing " + operation);
        //preserve default behavior #{bindings.Next.execute}
        //in code below
        //binding does not exist
        OperationBinding opButton = (OperationBinding) bindings.get(operation);
        opButton.execute();
        logOperationError(opButton.getErrors());

        Row nowRow = iter.getCurrentRow();
        boolean switchIterator = false;
        if ("Next".equals(adfAction) && (currentRow == nowRow)) {
            //nächste tabelle verenden, alte selection löschen
            int newIndex = (currentIteratorIndex + 1) % ITERATORS.length;
            log.info("Switching tables from " + currentIteratorIndex + " to " + newIndex);
            // clear selection of old table nad set selection in new table
            RowKeySet emptySet = createRowKeySet(null);
            RichTable oldTable = getTableFromIndex(currentIteratorIndex);
            oldTable.setSelectedRowKeys(emptySet);
            currentIteratorIndex = newIndex;
            AdfFacesContext.getCurrentInstance().addPartialTarget(oldTable);
            log.info("...setting action to first!");
            adfAction = "First";
            switchIterator = true;
        } else if ("Previous".equals(adfAction) && (currentRow == nowRow)) {
            //nächste tabelle verenden, alte selection löschen
            int newIndex = Math.abs((currentIteratorIndex - 1) % ITERATORS.length);
            log.info("Switching tables from " + currentIteratorIndex + " to " + newIndex);
            // clear selection of old table nad set selection in new table
            RowKeySet emptySet = createRowKeySet(null);
            RichTable oldTable = getTableFromIndex(currentIteratorIndex);
            oldTable.setSelectedRowKeys(emptySet);
            currentIteratorIndex = newIndex;
            AdfFacesContext.getCurrentInstance().addPartialTarget(oldTable);
            log.info("...setting action to last!");
            adfAction = "Last";
            switchIterator = true;
        }

        if (switchIterator) {
            operation = adfAction + ITERATORS[currentIteratorIndex];
            log.info("executing " + operation);
            //preserve default behavior #{bindings.Next.execute}
            //in code below
            //binding does not exist
            opButton = (OperationBinding) bindings.get(operation);
            opButton.execute();
            logOperationError(opButton.getErrors());
        }

        DCIteratorBinding iterBind = (DCIteratorBinding) bindings.get(ITERATORS[currentIteratorIndex]);
        NavigatableRowIterator iterator = iterBind.getNavigatableRowIterator();
        Row nextRow = iterator.getCurrentRow();
        Key k = nextRow.getKey();
        iterBind.setCurrentRowWithKey(k.toStringFormat(true));
        RowKeySet newSet = createRowKeySet(k);
        RichTable _table = getTableFromIndex(currentIteratorIndex);
        _table.setSelectedRowKeys(newSet);
        openPosition(nextRow);
        AdfFacesContext.getCurrentInstance().addPartialTarget(_table);
    }

    private RowKeySet createRowKeySet(Key pKey) {
        ArrayList<Key> lst = new ArrayList<Key>(1);
        lst.add(pKey);
        RowKeySetImpl rowKeySetToCreate = new RowKeySetImpl();
        rowKeySetToCreate.add(lst);
        return rowKeySetToCreate;
    }

    private void openPosition(Row row) {
        String id = row.getAttribute(0).toString();
        String val = row.getAttribute(1).toString();
        setId(id);
        setName(val);
        AdfFacesContext.getCurrentInstance().addPartialTarget(getPform());
    }

    public void setCountriesTable(RichTable countriesTable) {
        this.countriesTable = ComponentReference.newUIComponentReference(countriesTable);
        ;
    }

    public RichTable getCountriesTable() {
        if (null != countriesTable) {
            return countriesTable.getComponent();
        }
        return null;
    }

    private RichTable getTableFromIndex(int index) {
        switch (index) {
        case 0:
            return getRegionTable();
        case 1:
            return getCountriesTable();
        }
        return null;
    }

    private DCIteratorBinding getIteratorFromIndex(int index) {
        if (index < 0 || index >= ITERATORS.length) {
            return null;
        }
        BindingContainer bindings = BindingContext.getCurrent().getCurrentBindingsEntry();
        DCIteratorBinding iterBind = (DCIteratorBinding) bindings.get(ITERATORS[index]);
        return iterBind;
    }

    private RichTable getTableFromiteratorName(String name) {
        RichTable res = null;
        int index = 0;
        boolean found = false;
        do {
            if (ITERATORS[index].equals(name)) {
                found = true;
            } else {
                index++;
            }
        } while (index < ITERATORS.length || found);
        if (found) {
            res = getTableFromIndex(index);
        }

        return res;
    }

    private int getIndexFromiteratorName(String name) {
        int res = -1;
        int index = 0;
        boolean found = false;
        do {
            if (ITERATORS[index].equals(name)) {
                found = true;
            } else {
                index++;
            }
        } while (index < ITERATORS.length && !found);
        if (found) {
            res = index;
        }

        return res;
    }

    public void setPageloadTrigger(String pageloadTrigger) {
        this.pageloadTrigger = pageloadTrigger;
    }

    public String getPageloadTrigger() {
        if ("false".equals(pageloadTrigger)) {
            // init first table first row as selected
            currentIteratorIndex = 0;
            RichTable fromIndex = getTableFromIndex(currentIteratorIndex);
            selectFirstRowInTable(fromIndex);
            pageloadTrigger = "true";
        }
        return pageloadTrigger;
    }

    public static void selectFirstRowInTable(RichTable table) {
        // make first row current in iterator
        CollectionModel tableModel = (CollectionModel) table.getValue();
        JUCtrlHierBinding adfTableBinding = (JUCtrlHierBinding) tableModel.getWrappedData();
        DCIteratorBinding tableIteratorBinding = adfTableBinding.getDCIteratorBinding();
        tableIteratorBinding.setCurrentRowIndexInRange(0);

        // explicitly select first row in table
        RowKeySet rks = new RowKeySetImpl();
        CollectionModel model = (CollectionModel) table.getValue();
        model.setRowIndex(0);
        Object key = model.getRowKey();
        rks.add(key);
        table.setSelectedRowKeys(rks);
    }

    private void logOperationError(List exceptions) {
        Exception ex = null;
        if (!exceptions.isEmpty()) {
            ex = (Exception) exceptions.get(0);
            log.warning(ex.getMessage(), ex);
        }
    }

    private void makeCurrent(RichTable table, RowKeySet newCurrentRow, RowKeySet oldCurrentRow) {
        //To make a row current, we need to create a SelectionEvent which
        //expects the following arguments: component, unselected_keys,
        //selected_keys. In our example, we don't have unselected keys and
        //therefore create an empty RowSet for this
        SelectionEvent selectionEvent = new SelectionEvent(oldCurrentRow, newCurrentRow, table);
        selectionEvent.queue();
        AdfFacesContext.getCurrentInstance().addPartialTarget(table);
        //        String adfSelectionListener = "#{bindings.Departments.treeModel.makeCurrent}";
        //        FacesContext fctx = FacesContext.getCurrentInstance();
        //        Application application = fctx.getApplication();
        //        ELContext elCtx = fctx.getELContext();
        //        ExpressionFactory exprFactory = application.getExpressionFactory();
        //
        //        MethodExpression me = null;
        //        me = exprFactory.createMethodExpression(elCtx, adfSelectionListener, Object.class, new Class[] { SelectionEvent.class });
        //        me.invoke(elCtx, new Object[] { selectionEvent });
    }


}
