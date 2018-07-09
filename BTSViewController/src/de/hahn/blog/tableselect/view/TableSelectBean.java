package de.hahn.blog.tableselect.view;

import java.util.ArrayList;

import javax.el.ELContext;
import javax.el.ExpressionFactory;
import javax.el.MethodExpression;

import javax.faces.application.Application;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;

import javax.servlet.ServletContext;

import oracle.adf.model.BindingContext;
import oracle.adf.model.binding.DCIteratorBinding;
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

public class TableSelectBean {
    private RichPanelFormLayout pform;
    private RichTable regionTable;
    private String id;
    private String name;

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

    public void onTableSelect(SelectionEvent selectionEvent) {
        // #{bindings.RegionsView1.collectionModel.makeCurrent}
        //add pre-processing instructions here
        invokeMethodExpression("#{bindings.RegionsView1.collectionModel.makeCurrent}", new Object[] { selectionEvent },
                               new Class[] { SelectionEvent.class });
        //add post processing instructions here
        AdfFacesContext.getCurrentInstance().addPartialTarget(getPform());
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
        this.pform = pform;
    }

    public RichPanelFormLayout getPform() {
        return pform;
    }

    public void setRegionTable(RichTable regionTable) {
        this.regionTable = regionTable;
    }

    public RichTable getRegionTable() {
        return regionTable;
    }

    public void onNavigation(ActionEvent actionEvent) {
        RichButton navButton = (RichButton) actionEvent.getSource();
        //Get ServlerContexct
        FacesContext ctx = FacesContext.getCurrentInstance();
        ServletContext servletContext = (ServletContext) ctx.getExternalContext().getContext();
        // get the binding container
        BindingContainer bindings = BindingContext.getCurrent().getCurrentBindingsEntry();
        //f:attribute set
        String adfAction = (String) navButton.getAttributes().get("adfAction");

        //preserve default behavior #{bindings.Next.execute}
        //in code below
        //binding does not exist
        OperationBinding opButton = (OperationBinding) bindings.get(adfAction);
        opButton.execute();

        DCIteratorBinding iterBind = (DCIteratorBinding) bindings.get("RegionsView1Iterator");
        NavigatableRowIterator iterator = iterBind.getNavigatableRowIterator();
        Row nextRow = iterator.getCurrentRow();
        Key k = nextRow.getKey();
        iterBind.setCurrentRowWithKey(k.toStringFormat(true));
        RowKeySet newSet = createRowKeySet(k);
        getRegionTable().setSelectedRowKeys(newSet);
        openPosition(nextRow);
        AdfFacesContext.getCurrentInstance().addPartialTarget(getRegionTable());
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
}
