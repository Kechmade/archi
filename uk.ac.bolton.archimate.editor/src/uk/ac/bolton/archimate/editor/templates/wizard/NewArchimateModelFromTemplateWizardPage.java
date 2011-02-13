/*******************************************************************************
 * Copyright (c) 2010 Bolton University, UK.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 *******************************************************************************/
package uk.ac.bolton.archimate.editor.templates.wizard;

import java.io.File;
import java.io.IOException;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.nebula.widgets.gallery.DefaultGalleryItemRenderer;
import org.eclipse.nebula.widgets.gallery.Gallery;
import org.eclipse.nebula.widgets.gallery.GalleryItem;
import org.eclipse.nebula.widgets.gallery.NoGroupRenderer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseMoveListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Scale;
import org.eclipse.ui.PlatformUI;

import uk.ac.bolton.archimate.editor.templates.ITemplate;
import uk.ac.bolton.archimate.editor.templates.ITemplateGroup;
import uk.ac.bolton.archimate.editor.templates.ModelTemplate;
import uk.ac.bolton.archimate.editor.templates.TemplateManager;
import uk.ac.bolton.archimate.editor.templates.dialog.TemplateManagerDialog;
import uk.ac.bolton.archimate.editor.ui.ExtendedWizardDialog;
import uk.ac.bolton.archimate.editor.ui.IArchimateImages;
import uk.ac.bolton.archimate.editor.ui.ImageFactory;
import uk.ac.bolton.archimate.editor.utils.StringUtils;


/**
 * New Archimate Model From Template Wizard Page
 * 
 * @author Phillip Beauvoir
 */
public class NewArchimateModelFromTemplateWizardPage extends WizardPage {
    
    public static String HELPID = "uk.ac.bolton.archimate.help.NewArchimateModelFromTemplateWizardPage"; //$NON-NLS-1$

    private Gallery fGallery;
    private GalleryItem fGalleryRoot;
    private StyledText fDescriptionText;
    
    private TemplateManager fTemplateManager;
    
    private TableViewer fInbuiltTableViewer;
    private TableViewer fUserTableViewer;
    
    private ITemplate fSelectedTemplate;

    /*
     * Track and de-select Viewers so that only one has focus at a time
     */
    private TableViewer fLastViewerFocus;
    
    int DEFAULT_GALLERY_ITEM_SIZE = 120;
    
    private static final String OPEN = "Open...";
    private static final String MANAGE = "Manage...";
    
    public NewArchimateModelFromTemplateWizardPage() {
        super("NewArchimateModelFromTemplateWizardPage");
        setTitle("New ArchiMate Model");
        setDescription("Choose a Template for your Model");
        setImageDescriptor(IArchimateImages.ImageFactory.getImageDescriptor(ImageFactory.ECLIPSE_IMAGE_NEW_WIZARD));

        fTemplateManager = new TemplateManager();
    }

    @Override
    public void createControl(Composite parent) {
        Composite container = new Composite(parent, SWT.NULL);
        container.setLayout(new GridLayout());
        setControl(container);
        
        // Help
        PlatformUI.getWorkbench().getHelpSystem().setHelp(container, HELPID);

        GridData gd;
        
        SashForm sash1 = new SashForm(container, SWT.HORIZONTAL);
        gd = new GridData(SWT.FILL, SWT.FILL, true, true);
        gd.widthHint = 800;
        gd.heightHint = 500;
        sash1.setLayoutData(gd);
        
        Composite tableComposite = new Composite(sash1, SWT.BORDER);
        GridLayout layout = new GridLayout();
        layout.marginWidth = 0;
        layout.marginHeight = 0;
        layout.verticalSpacing = 0;
        tableComposite.setLayout(layout);
        
        // Inbuilt Templates
        gd = new GridData(SWT.FILL, SWT.FILL, true, false);
        fInbuiltTableViewer = createGroupsTableViewer(tableComposite, "Templates", gd);
        fInbuiltTableViewer.setInput(new Object[] { fTemplateManager.getInbuiltTemplateGroup(), OPEN, MANAGE });
        // Mouse UP actions...
        fInbuiltTableViewer.getControl().addMouseListener(new MouseAdapter() {
            @Override
            public void mouseUp(MouseEvent e) {
                Object o = ((IStructuredSelection)fInbuiltTableViewer.getSelection()).getFirstElement();
                // Open...
                if(o == OPEN) {
                    handleOpenAction();
                }
                // Manage...
                else if(o == MANAGE) {
                    handleManageTemplatesAction();
                }
            }
        });
        
        // My Templates
        gd = new GridData(SWT.FILL, SWT.FILL, true, true);
        fUserTableViewer = createGroupsTableViewer(tableComposite, "My Templates", gd);
        fUserTableViewer.setSorter(new ViewerSorter() {
            @Override
            public int category(Object element) {
                if(element == fTemplateManager.AllUserTemplatesGroup) {
                    return 0;
                }
                return 1;
            }
        });
        fUserTableViewer.setInput(fTemplateManager);
        
        SashForm sash2 = new SashForm(sash1, SWT.VERTICAL);
        
        Composite galleryComposite = new Composite(sash2, SWT.NULL);
        layout = new GridLayout();
        layout.marginWidth = 0;
        layout.marginHeight = 0;
        layout.verticalSpacing = 0;
        galleryComposite.setLayout(layout);
        
        fGallery = new Gallery(galleryComposite, SWT.V_SCROLL | SWT.BORDER);
        fGallery.setLayoutData(new GridData(GridData.FILL_BOTH));
        
        // Renderers
        final NoGroupRenderer groupRenderer = new NoGroupRenderer();
        groupRenderer.setItemSize(DEFAULT_GALLERY_ITEM_SIZE, DEFAULT_GALLERY_ITEM_SIZE);
        groupRenderer.setAutoMargin(true);
        groupRenderer.setMinMargin(10);
        fGallery.setGroupRenderer(groupRenderer);
        
        final DefaultGalleryItemRenderer itemRenderer = new DefaultGalleryItemRenderer();
        itemRenderer.setDropShadows(true);
        itemRenderer.setDropShadowsSize(7);
        itemRenderer.setShowRoundedSelectionCorners(false);
        fGallery.setItemRenderer(itemRenderer);
        
        // Root Group
        fGalleryRoot = new GalleryItem(fGallery, SWT.NONE);
        
        // Slider
        final Scale scale = new Scale(galleryComposite, SWT.HORIZONTAL);
        gd = new GridData(SWT.END, SWT.NONE, false, false);
        gd.widthHint = 120;
        scale.setLayoutData(gd);
        scale.setMaximum(480);
        scale.setMinimum(64);
        scale.setIncrement(8);
        scale.setPageIncrement(64);
        scale.setSelection(DEFAULT_GALLERY_ITEM_SIZE);
        scale.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                int inc = scale.getSelection();
                itemRenderer.setDropShadows(inc >= DEFAULT_GALLERY_ITEM_SIZE);
                groupRenderer.setItemSize(inc, inc);
            }
        });
        
        // Description
        fDescriptionText = new StyledText(sash2, SWT.V_SCROLL | SWT.READ_ONLY | SWT.WRAP | SWT.BORDER);
        
        fGallery.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                if(e.item instanceof GalleryItem) {
                    ITemplate template = (ITemplate)((GalleryItem)e.item).getData();
                    updateWizard(template);
                }
                else {
                    updateWizard(null);
                }
             }
        });
        
        // Double-clicks
        fGallery.addListener(SWT.MouseDoubleClick, new Listener() {
            public void handleEvent(Event event) {
                GalleryItem item = fGallery.getItem(new Point(event.x, event.y));
                if(item != null) {
                    ((ExtendedWizardDialog)getContainer()).finishPressed();
                }
            }
        });
        
        // Dispose of the images in TemplateManager here not in the main dispose() method because if the help system is showing then 
        // the TrayDialog is resized and this control is asked to relayout.
        fGallery.addDisposeListener(new DisposeListener() {
            @Override
            public void widgetDisposed(DisposeEvent e) {
                fTemplateManager.dispose();
            }
        });
        
        // Mouse move shows thumbnails
        registerMouseMoveHandler();
        
        // Select first group on table
        selectFirstTableItem();
        
        sash1.setWeights(new int[] { 30, 70 });
        sash2.setWeights(new int[] { 70, 30 });
        
        setPageComplete(true); // Yes it's OK
    }
    
    public ITemplate getSelectedTemplate() {
        return fSelectedTemplate;
    }
    
    /**
     * Create a table
     */
    private TemplateGroupsTableViewer createGroupsTableViewer(Composite parent, String labelText, GridData gd) {
        CLabel label = new CLabel(parent, SWT.NULL);
        label.setText(labelText);
        label.setImage(IArchimateImages.ImageFactory.getImage(IArchimateImages.ICON_MODELS_16));
        
        Composite tableComp = new Composite(parent, SWT.NULL);
        tableComp.setLayout(new TableColumnLayout());
        tableComp.setLayoutData(gd);
        final TemplateGroupsTableViewer tableViewer = new TemplateGroupsTableViewer(tableComp, SWT.NULL);
        tableViewer.addSelectionChangedListener(new ISelectionChangedListener() {
            public void selectionChanged(SelectionChangedEvent event) {
                Object o = ((IStructuredSelection)event.getSelection()).getFirstElement();
                handleTableItemSelected(o);
                deFocusTable(tableViewer);
            }
        });
        
        return tableViewer;
    }
    
    /**
     * Set the focus on one table at a time
     */
    private void deFocusTable(TableViewer viewer) {
        if(fLastViewerFocus != null && !fLastViewerFocus.getControl().isDisposed() && fLastViewerFocus != viewer) {
                fLastViewerFocus.getTable().deselectAll();
        }
        fLastViewerFocus = viewer;
    }
    
    private void handleTableItemSelected(Object item) {
        // Template Group
        if(item instanceof ITemplateGroup) {
            clearGallery();
            updateGallery((ITemplateGroup)item);
        }
    }
    
    /**
     * User wants to open Template from file
     */
    private void handleOpenAction() {
        getContainer().getShell().setVisible(false);
        
        FileDialog dialog = new FileDialog(getShell(), SWT.OPEN);
        dialog.setText("Open Template");
        dialog.setFilterExtensions(new String[] { TemplateManager.ARCHIMATE_TEMPLATE_FILE_WILDCARD, "*.*" } );
        String path = dialog.open();
        if(path == null) {
            selectFirstTableItem();
            getContainer().getShell().setVisible(true);
            return;
        }
        
        final File file = new File(path);
        
        BusyIndicator.showWhile(null, new Runnable() { 
            @Override
            public void run() {
                try {
                    if(!TemplateManager.isValidTemplateFile(file)) { // This could take a while
                        throw new IOException("Unknown format");
                    }
                    else {
                        // Create dummy template and Finish
                        ITemplate template = new ModelTemplate();
                        template.setFile(file);
                        fSelectedTemplate = template;
                        ((ExtendedWizardDialog)getContainer()).finishPressed();
                    }
                }
                catch(IOException ex) {
                    MessageDialog.openError(getShell(), "Error opening file", ex.getMessage());
                    getContainer().getShell().setVisible(true);
                }
            }
        });
    }
    
    /**
     * User wants to manage Templates
     */
    private void handleManageTemplatesAction() {
        getContainer().getShell().setVisible(false);
        
        TemplateManagerDialog dialog = new TemplateManagerDialog(getShell());
        if(dialog.open() == Window.OK) {
            fTemplateManager.reset();
            fUserTableViewer.refresh();
        }
        
        selectFirstTableItem();
        getContainer().getShell().setVisible(true);
    }
    
    private void selectFirstTableItem() {
        // Select first group on table
        Object o = fInbuiltTableViewer.getElementAt(0);
        if(o != null) {
            fInbuiltTableViewer.setSelection(new StructuredSelection(o));
            fInbuiltTableViewer.getControl().setFocus();
        }
    }
    
    /*
     * Show the thumbnail images as the mouse moves
     */
    private void registerMouseMoveHandler() {
        fGallery.addMouseMoveListener(new MouseMoveListener() {
            int mouse_movement_factor = 6;
            int index = 0;
            GalleryItem selectedItem;
            int last_x;
            
            @Override
            public void mouseMove(MouseEvent event) {
                GalleryItem item = fGallery.getItem(new Point(event.x, event.y));
                
                // Not enough thumbnails
                if(item != null) {
                    ITemplate template = (ITemplate)item.getData();
                    if(template.getThumbnails().length < 2) {
                        return;
                    }
                }
                
                if(item != selectedItem) {
                    if(selectedItem != null) {
                        ITemplate template = (ITemplate)selectedItem.getData();
                        selectedItem.setImage(template.getKeyThumbnail());
                    }
                    
                    selectedItem = item;
                    index = 0;
                    last_x = event.x;
                }
                
                if(item != null) {
                    ITemplate template = (ITemplate)item.getData();
                    
                    if(event.x < last_x - mouse_movement_factor) {
                        index--;
                        if(index < 0) {
                            index = template.getThumbnails().length - 1;
                        }
                    }
                    else if(event.x > last_x + mouse_movement_factor) {
                        index++;
                        if(index == template.getThumbnails().length) {
                            index = 0;
                        }
                    }
                    else {
                        return;
                    }
                    
                    last_x = event.x;

                    item.setImage(template.getThumbnails()[index]);
                }
            }
        });
    }
    
    /**
     * Clear old root group
     */
    private void clearGallery() {
        if(fGalleryRoot != null && !fGallery.isDisposed() && fGallery.getItemCount() > 0) {
            while(fGalleryRoot.getItemCount() > 0) {
                GalleryItem item = fGalleryRoot.getItem(0);
                fGalleryRoot.remove(item);
            }
        }
    }
    
    /**
     * Update the Gallery
     * @param group
     */
    private void updateGallery(ITemplateGroup group) {
        for(ITemplate template : group.getSortedTemplates()) {
            GalleryItem item = new GalleryItem(fGalleryRoot, SWT.NONE);
            item.setText(StringUtils.safeString(template.getName()));
            item.setImage(template.getKeyThumbnail());
            item.setData(template);
        }
        
        if(fGalleryRoot.getItem(0) != null) {
            fGallery.setSelection(new GalleryItem[] { fGalleryRoot.getItem(0) });
            updateWizard((ITemplate)fGalleryRoot.getItem(0).getData());
        }
        else {
            updateWizard(null);
        }
    }
    
    /**
     * Update the wizard to show selected description text
     * @param template
     */
    private void updateWizard(ITemplate template) {
        fSelectedTemplate = template;

        if(template == null) {
            fDescriptionText.setText(""); //$NON-NLS-1$
            setPageComplete(false);
        }
        else {
            // Update description
            String text = StringUtils.safeString(template.getDescription());
            String desc = StringUtils.safeString(template.getName()) + ":"; //$NON-NLS-1$
            fDescriptionText.setText(desc + "   " + text); //$NON-NLS-1$
            StyleRange style = new StyleRange();
            style.start = 0;
            style.length = desc.length();
            style.fontStyle = SWT.BOLD;
            fDescriptionText.setStyleRange(style);
            setPageComplete(true);
        }
    }
}