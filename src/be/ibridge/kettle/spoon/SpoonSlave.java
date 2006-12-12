/**********************************************************************
 **                                                                   **
 **               This code belongs to the KETTLE project.            **
 **                                                                   **
 ** Kettle, from version 2.2 on, is released into the public domain   **
 ** under the Lesser GNU Public License (LGPL).                       **
 **                                                                   **
 ** For more details, please read the document LICENSE.txt, included  **
 ** in this project                                                   **
 **                                                                   **
 ** http://www.kettle.be                                              **
 ** info@kettle.be                                                    **
 **                                                                   **
 **********************************************************************/

package be.ibridge.kettle.spoon;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.swt.widgets.TreeItem;

import be.ibridge.kettle.cluster.SlaveServer;
import be.ibridge.kettle.core.ColumnInfo;
import be.ibridge.kettle.core.Const;
import be.ibridge.kettle.core.dialog.EnterSelectionDialog;
import be.ibridge.kettle.core.widget.TreeMemory;
import be.ibridge.kettle.trans.step.BaseStepDialog;
import be.ibridge.kettle.trans.step.StepMeta;
import be.ibridge.kettle.trans.step.StepStatus;
import be.ibridge.kettle.www.GetStatusHandler;
import be.ibridge.kettle.www.GetTransStatusHandler;
import be.ibridge.kettle.www.SlaveServerStatus;
import be.ibridge.kettle.www.SlaveServerTransStatus;

/**
 * SpoonLog handles the display of the logging information in the Spoon logging window.
 *  
 * @see be.ibridge.kettle.spoon.Spoon
 * @author Matt
 * @since  17 may 2003
 */
public class SpoonSlave extends Composite
{
	public static final long UPDATE_TIME_VIEW = 15000L; // 15s
	public static final long UPDATE_TIME_LOG = 30000L; // 30s
	public static final long REFRESH_TIME = 100L;
    
    public static final String STRING_SLAVE_LOG_TREE_NAME = "SLAVE_LOG : ";

	private Shell shell;
	private Display display;
    private SlaveServer slaveServer;
    private Spoon spoon;

	private ColumnInfo[] colinf;

	private Tree wTree;
	private Text wText;

	private Button wError;
    private Button wRefresh;

    private FormData fdText, fdSash;
    private SelectionListener lsRefresh, lsError;

	private long lastUpdateView;
    private StringBuffer busy;
    private boolean refresh_busy;
    private SlaveServerStatus slaveServerStatus;

	public SpoonSlave(Composite parent, int style, final Spoon spoon, SlaveServer slaveServer)
	{
		super(parent, style);
		this.shell = parent.getShell();
        this.display = shell.getDisplay();
		this.spoon = spoon;
		this.slaveServer = slaveServer;

		lastUpdateView = 0L;

		FormLayout formLayout = new FormLayout();
		formLayout.marginWidth = Const.FORM_MARGIN;
		formLayout.marginHeight = Const.FORM_MARGIN;

		setLayout(formLayout);

		setVisible(true);
		spoon.props.setLook(this);

		SashForm sash = new SashForm(this, SWT.VERTICAL);
		spoon.props.setLook(sash);

		sash.setLayout(new FillLayout());

		colinf = new ColumnInfo[] { 
                new ColumnInfo(Messages.getString("SpoonSlave.Column.Stepname"), ColumnInfo.COLUMN_TYPE_TEXT, false, true), //$NON-NLS-1$
                new ColumnInfo(Messages.getString("SpoonSlave.Column.Status"), ColumnInfo.COLUMN_TYPE_TEXT, false, true), //$NON-NLS-1$
				new ColumnInfo(Messages.getString("SpoonSlave.Column.Copynr"), ColumnInfo.COLUMN_TYPE_TEXT, false, true), //$NON-NLS-1$
				new ColumnInfo(Messages.getString("SpoonSlave.Column.Read"), ColumnInfo.COLUMN_TYPE_TEXT, false, true), //$NON-NLS-1$
				new ColumnInfo(Messages.getString("SpoonSlave.Column.Written"), ColumnInfo.COLUMN_TYPE_TEXT, false, true), //$NON-NLS-1$
				new ColumnInfo(Messages.getString("SpoonSlave.Column.Input"), ColumnInfo.COLUMN_TYPE_TEXT, false, true), //$NON-NLS-1$
				new ColumnInfo(Messages.getString("SpoonSlave.Column.Output"), ColumnInfo.COLUMN_TYPE_TEXT, false, true), //$NON-NLS-1$
				new ColumnInfo(Messages.getString("SpoonSlave.Column.Updated"), ColumnInfo.COLUMN_TYPE_TEXT, false, true), //$NON-NLS-1$
				new ColumnInfo(Messages.getString("SpoonSlave.Column.Errors"), ColumnInfo.COLUMN_TYPE_TEXT, false, true), //$NON-NLS-1$
				new ColumnInfo(Messages.getString("SpoonSlave.Column.Active"), ColumnInfo.COLUMN_TYPE_TEXT, false, true), //$NON-NLS-1$
				new ColumnInfo(Messages.getString("SpoonSlave.Column.Time"), ColumnInfo.COLUMN_TYPE_TEXT, false, true), //$NON-NLS-1$
				new ColumnInfo(Messages.getString("SpoonSlave.Column.Speed"), ColumnInfo.COLUMN_TYPE_TEXT, false, true), //$NON-NLS-1$
				new ColumnInfo(Messages.getString("SpoonSlave.Column.PriorityBufferSizes"), ColumnInfo.COLUMN_TYPE_TEXT, false, true), //$NON-NLS-1$
				new ColumnInfo(Messages.getString("SpoonSlave.Column.Sleeps"), ColumnInfo.COLUMN_TYPE_TEXT, false, true) //$NON-NLS-1$
		};

		colinf[1].setAllignement(SWT.RIGHT);
        colinf[2].setAllignement(SWT.LEFT);
		colinf[3].setAllignement(SWT.RIGHT);
		colinf[4].setAllignement(SWT.RIGHT);
		colinf[5].setAllignement(SWT.RIGHT);
		colinf[6].setAllignement(SWT.RIGHT);
		colinf[7].setAllignement(SWT.RIGHT);
		colinf[8].setAllignement(SWT.RIGHT);
		colinf[9].setAllignement(SWT.RIGHT);
		colinf[10].setAllignement(SWT.RIGHT);
		colinf[11].setAllignement(SWT.RIGHT);
		colinf[12].setAllignement(SWT.RIGHT);
		colinf[13].setAllignement(SWT.RIGHT);

		wTree = new Tree(sash, SWT.SINGLE | SWT.V_SCROLL | SWT.H_SCROLL);
        wTree.setHeaderVisible(true);
        TreeMemory.addTreeListener(wTree, STRING_SLAVE_LOG_TREE_NAME+slaveServer.toString());
        for (int i=0;i<colinf.length;i++)
        {
            ColumnInfo columnInfo = colinf[i];
            TreeColumn treeColumn = new TreeColumn(wTree, columnInfo.getAllignement());
            treeColumn.setText(columnInfo.getName());
        }
        wTree.addSelectionListener(new SelectionAdapter()
            {
                public void widgetSelected(SelectionEvent arg0)
                {
                    showLog();
                }
            }
        );
        
		wText = new Text(sash, SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL | SWT.READ_ONLY);
		spoon.props.setLook(wText);
		wText.setVisible(true);
        
		wRefresh = new Button(this, SWT.PUSH);
		wRefresh.setText(Messages.getString("SpoonSlave.Button.Refresh"));
        wRefresh.setEnabled(true);
        
		wError = new Button(this, SWT.PUSH);
		wError.setText(Messages.getString("SpoonSlave.Button.ShowErrorLines")); //$NON-NLS-1$
        
        BaseStepDialog.positionBottomButtons(this, new Button[] { wRefresh, wError }, Const.MARGIN, null);
        
		// Put text in the middle
		fdText = new FormData();
		fdText.left = new FormAttachment(0, 0);
		fdText.top = new FormAttachment(0, 0);
		fdText.right = new FormAttachment(100, 0);
		fdText.bottom = new FormAttachment(100, 0);
		wText.setLayoutData(fdText);

		fdSash = new FormData();
		fdSash.left = new FormAttachment(0, 0); // First one in the left top corner
		fdSash.top = new FormAttachment(0, 0);
		fdSash.right = new FormAttachment(100, 0);
		fdSash.bottom = new FormAttachment(wRefresh, -5);
		sash.setLayoutData(fdSash);

		pack();

		lsError = new SelectionAdapter()
		{
			public void widgetSelected(SelectionEvent e)
			{
				showErrors();
			}
		};
        
        lsRefresh = new SelectionAdapter()
        {
            public void widgetSelected(SelectionEvent e)
            {
                showErrors();
            }
        };

		final Timer tim = new Timer();
        busy = new StringBuffer("N");

        TimerTask timtask = new TimerTask()
        {
            public void run()
            {
                if (display != null && !display.isDisposed())
                {
                    display.asyncExec(
                        new Runnable()
                        {
                            public void run()
                            {
                                refresh();
                            }
                        }
                    );
                }
            }
        };

        tim.schedule(timtask, 0L, REFRESH_TIME); // schedule to repeat a couple of times per second to get fast feedback 

		wError.addSelectionListener(lsError);
		wRefresh.addSelectionListener(lsRefresh);
		addDisposeListener(new DisposeListener() { public void widgetDisposed(DisposeEvent e) { tim.cancel(); } } );
	}
    
    /**
     * Someone clicks on a line: show the log or error message associated with that in the text-box
     */
    public void showLog()
    {
        TreeItem ti[] = wTree.getSelection();
        if (ti.length==1)
        {
            TreeItem treeItem = ti[0];
            String[] path = Const.getTreeStrings(treeItem);
            if (path.length==1) // transformation name
            {
                String transName = path[0];
                SlaveServerTransStatus transStatus = slaveServerStatus.findTransStatus(transName);
                if (transStatus!=null)
                {
                    StringBuffer message = new StringBuffer();
                    String errorDescription = transStatus.getErrorDescription();
                    if (!Const.isEmpty(errorDescription))
                    {
                        message.append(errorDescription).append(Const.CR);
                    }
                    
                    
                        
                    wText.setText(message.toString());
                }
            }
        }
    }

    private void refresh()
    {
        if (busy.toString().equals("N"))
        {
            busy.setCharAt(0, 'Y');
            refreshView();
            busy.setCharAt(0, 'N');
        }
    }

	private void refreshView()
	{
  		if (wTree.isDisposed()) return;
		if (refresh_busy) return;
		refresh_busy = true;


        long time = new Date().getTime();
		long msSinceLastUpdate = time - lastUpdateView;
		if (msSinceLastUpdate > UPDATE_TIME_VIEW)
		{
            lastUpdateView = time;
            wTree.removeAll();
            
            // Determine the transformations on the slave servers
            
            try
            {
                String xml = slaveServer.execService(GetStatusHandler.CONTEXT_PATH+"?xml=Y");
                slaveServerStatus = SlaveServerStatus.fromXML(xml);
            }
            catch(Exception e)
            {
                slaveServerStatus = new SlaveServerStatus("Error contacting server");
                slaveServerStatus.setErrorDescription(Const.getStackTracker(e));
            }
            
            for (int i = 0; i < slaveServerStatus.getTransStatusList().size(); i++)
			{
                SlaveServerTransStatus transStatus = (SlaveServerTransStatus) slaveServerStatus.getTransStatusList().get(i);
                TreeItem transItem = new TreeItem(wTree, SWT.NONE);
                transItem.setText(0, transStatus.getTransName());
                transItem.setText(8, transStatus.getStatusDescription());
                
                try
                {
                    String xml = slaveServer.execService(GetTransStatusHandler.CONTEXT_PATH+"?xml=Y");
                    SlaveServerTransStatus ts = SlaveServerTransStatus.fromXML(xml);
                    List stepStatusList = ts.getStepStatusList();
                    transStatus.setStepStatusList(stepStatusList);
                    
                    for (int s=0;s<stepStatusList.size();s++)
                    {
                        StepStatus stepStatus = (StepStatus) stepStatusList.get(i);
                        TreeItem stepItem = new TreeItem(transItem, SWT.NONE);
                        stepItem.setText(stepStatus.getSpoonSlaveLogFields());
                    }
                }
                catch (Exception e)
                {
                    transStatus.setErrorDescription("Unable to access transformation details : "+Const.CR+Const.getStackTracker(e));
                } 
			}
		}

		refresh_busy = false;
	}


	public void showErrors()
	{
		String all = wText.getText();
		ArrayList err = new ArrayList();

		int i = 0;
		int startpos = 0;
		int crlen = Const.CR.length();

		while (i < all.length() - crlen)
		{
			if (all.substring(i, i + crlen).equalsIgnoreCase(Const.CR))
			{
				String line = all.substring(startpos, i);
				String uLine = line.toUpperCase();
				if (uLine.indexOf(Messages.getString("SpoonLog.System.ERROR")) >= 0 || //$NON-NLS-1$
						uLine.indexOf(Messages.getString("SpoonLog.System.EXCEPTION")) >= 0 || //$NON-NLS-1$
						uLine.indexOf("ERROR") >= 0 || // i18n for compatibilty to non translated steps a.s.o. //$NON-NLS-1$ 
						uLine.indexOf("EXCEPTION") >= 0 // i18n for compatibilty to non translated steps a.s.o. //$NON-NLS-1$
				)
				{
					err.add(line);
				}
				// New start of line
				startpos = i + crlen;
			}

			i++;
		}
		String line = all.substring(startpos);
		String uLine = line.toUpperCase();
		if (uLine.indexOf(Messages.getString("SpoonLog.System.ERROR2")) >= 0 || //$NON-NLS-1$
				uLine.indexOf(Messages.getString("SpoonLog.System.EXCEPTION2")) >= 0 || //$NON-NLS-1$
				uLine.indexOf("ERROR") >= 0 || // i18n for compatibilty to non translated steps a.s.o. //$NON-NLS-1$ 
				uLine.indexOf("EXCEPTION") >= 0 // i18n for compatibilty to non translated steps a.s.o. //$NON-NLS-1$
		)
		{
			err.add(line);
		}

		if (err.size() > 0)
		{
			String err_lines[] = new String[err.size()];
			for (i = 0; i < err_lines.length; i++)
				err_lines[i] = (String) err.get(i);

			EnterSelectionDialog esd = new EnterSelectionDialog(shell, err_lines, Messages.getString("SpoonLog.Dialog.ErrorLines.Title"), Messages.getString("SpoonLog.Dialog.ErrorLines.Message")); //$NON-NLS-1$ //$NON-NLS-2$
			line = esd.open();
			if (line != null)
			{
				for (i = 0; i < spoon.getTransMeta().nrSteps(); i++)
				{
					StepMeta stepMeta = spoon.getTransMeta().getStep(i);
					if (line.indexOf(stepMeta.getName()) >= 0)
					{
						spoon.editStep(stepMeta.getName());
					}
				}
				// System.out.println("Error line selected: "+line);
			}
		}
	}

	public String toString()
	{
		return Spoon.APP_NAME;
	}
}
