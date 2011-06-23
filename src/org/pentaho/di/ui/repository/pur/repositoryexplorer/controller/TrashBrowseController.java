package org.pentaho.di.ui.repository.pur.repositoryexplorer.controller;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.ResourceBundle;

import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.i18n.BaseMessages;
import org.pentaho.di.repository.ObjectId;
import org.pentaho.di.repository.Repository;
import org.pentaho.di.repository.RepositoryDirectory;
import org.pentaho.di.repository.RepositoryObject;
import org.pentaho.di.repository.RepositoryObjectInterface;
import org.pentaho.di.repository.RepositoryObjectType;
import org.pentaho.di.repository.pur.RepositoryObjectAccessException;
import org.pentaho.di.ui.repository.pur.repositoryexplorer.IUIEEUser;
import org.pentaho.di.ui.repository.pur.repositoryexplorer.model.UIEERepositoryDirectory;
import org.pentaho.di.ui.repository.pur.services.ITrashService;
import org.pentaho.di.ui.repository.repositoryexplorer.ControllerInitializationException;
import org.pentaho.di.ui.repository.repositoryexplorer.controllers.BrowseController;
import org.pentaho.di.ui.repository.repositoryexplorer.model.UIJob;
import org.pentaho.di.ui.repository.repositoryexplorer.model.UIObjectCreationException;
import org.pentaho.di.ui.repository.repositoryexplorer.model.UIObjectRegistry;
import org.pentaho.di.ui.repository.repositoryexplorer.model.UIRepositoryDirectories;
import org.pentaho.di.ui.repository.repositoryexplorer.model.UIRepositoryDirectory;
import org.pentaho.di.ui.repository.repositoryexplorer.model.UIRepositoryObject;
import org.pentaho.di.ui.repository.repositoryexplorer.model.UIRepositoryObjects;
import org.pentaho.di.ui.repository.repositoryexplorer.model.UITransformation;
import org.pentaho.ui.xul.XulComponent;
import org.pentaho.ui.xul.XulException;
import org.pentaho.ui.xul.binding.Binding;
import org.pentaho.ui.xul.binding.BindingConvertor;
import org.pentaho.ui.xul.components.XulButton;
import org.pentaho.ui.xul.components.XulConfirmBox;
import org.pentaho.ui.xul.components.XulPromptBox;
import org.pentaho.ui.xul.containers.XulDeck;
import org.pentaho.ui.xul.containers.XulTree;
import org.pentaho.ui.xul.dom.Document;
import org.pentaho.ui.xul.util.XulDialogCallback;

public class TrashBrowseController extends BrowseController {

  // ~ Static fields/initializers ======================================================================================

  private static final Class<?> PKG = IUIEEUser.class;

  // ~ Instance fields =================================================================================================

  private ResourceBundle messages = new ResourceBundle() {

    @Override
    public Enumeration<String> getKeys() {
      return null;
    }

    @Override
    protected Object handleGetObject(String key) {
      return BaseMessages.getString(PKG, key);
    }
    
  };  
  
  protected XulTree trashFileTable;

  protected XulDeck deck;

  protected List<UIRepositoryObject> selectedTrashFileItems;

  protected TrashDirectory trashDir = new TrashDirectory();

  protected ITrashService trashService;

  protected List<RepositoryObjectInterface> trash;

  protected Repository repository;
  
  protected XulButton undeleteButton;
  
  protected XulButton deleteButton;
  
  // ~ Constructors ====================================================================================================

  public TrashBrowseController() {
    super();
  }

  // ~ Methods =========================================================================================================

  /**
   * Intercept the repositoryDirectory.children and add the Trash directory to the end.
   */
  @Override
  protected Binding createDirectoryBinding() {
    bf.setBindingType(Binding.Type.ONE_WAY);
    return bf.createBinding(this, "repositoryDirectory", folderTree, "elements", //$NON-NLS-1$//$NON-NLS-2$
        new BindingConvertor<UIRepositoryDirectory, UIRepositoryDirectory>() {

          @Override
          public UIRepositoryDirectory sourceToTarget(final UIRepositoryDirectory value) {
            if (value == null || value.size() == 0) {
              return null;
            }
            if (!value.get(value.size() - 1).equals(trashDir)) {
              // add directly to children collection to bypass events
              value.getChildren().add(trashDir);
            }
            return value;
          }

          @Override
          public UIRepositoryDirectory targetToSource(final UIRepositoryDirectory value) {
            // not used
            return value;
          }

        });
  }

  protected class TrashDirectory extends UIEERepositoryDirectory {

    @Override
    public String getImage() {
      return "images/trash.png"; //$NON-NLS-1$
    }

    @Override
    public String getName() {
      return BaseMessages.getString(PKG, "Trash"); //$NON-NLS-1$
    }

    @Override
    public UIRepositoryDirectories getChildren() {
      return new UIRepositoryDirectories();
    }

    @Override
    public UIRepositoryObjects getRepositoryObjects() throws KettleException {
      return new UIRepositoryObjects();
    }
  }

  @Override
  public void init(Repository repository) throws ControllerInitializationException {
    super.init(repository);
    try {
      trashService = (ITrashService) repository.getService(ITrashService.class);
    } catch (Throwable e) {
      throw new ControllerInitializationException(e);
    }
  }

  protected void doCreateBindings() {
    deck = (XulDeck) document.getElementById("browse-tab-right-panel-deck");//$NON-NLS-1$
    trashFileTable = (XulTree) document.getElementById("deleted-file-table"); //$NON-NLS-1$

    deleteButton = (XulButton) document.getElementById("delete-button"); //$NON-NLS-1$
    undeleteButton = (XulButton) document.getElementById("undelete-button"); //$NON-NLS-1$
    
    bf.setBindingType(Binding.Type.ONE_WAY);
    BindingConvertor<List<UIRepositoryObject>, Boolean> buttonConverter = new BindingConvertor<List<UIRepositoryObject>, Boolean>() {

      @Override
      public Boolean sourceToTarget(List<UIRepositoryObject> value) {
        if (value != null && value.size() > 0) {
          return true;
        }
        return false;
      }

      @Override
      public List<UIRepositoryObject> targetToSource(Boolean value) {
        // TODO Auto-generated method stub
        return null;
      }
    };
    bf.createBinding(trashFileTable, "selectedItems", this, "selectedTrashFileItems"); //$NON-NLS-1$ //$NON-NLS-2$
    bf.createBinding(trashFileTable, "selectedItems", deleteButton, "!disabled", buttonConverter); //$NON-NLS-1$ //$NON-NLS-2$
    bf.createBinding(trashFileTable, "selectedItems", undeleteButton, "!disabled", buttonConverter); //$NON-NLS-1$ //$NON-NLS-2$
    bf.createBinding(trashFileTable, "selectedItems", "trash-context-delete", "!disabled", buttonConverter); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ 
    bf.createBinding(trashFileTable, "selectedItems", "trash-context-restore", "!disabled", buttonConverter); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
    
    bf.setBindingType(Binding.Type.ONE_WAY);
    bf.createBinding(this, "trash", trashFileTable, "elements", //$NON-NLS-1$  //$NON-NLS-2$
        new BindingConvertor<List<RepositoryObjectInterface>, UIRepositoryObjects>() {
          @Override
          public UIRepositoryObjects sourceToTarget(List<RepositoryObjectInterface> trash) {
            UIRepositoryObjects listOfObjects = new UIRepositoryObjects();

            for (RepositoryObjectInterface elem : trash) {
              if (elem instanceof RepositoryDirectory) {
                RepositoryDirectory dir = (RepositoryDirectory) elem;
                // TODO fetch parent dir from somewhere
                try {
                  listOfObjects.add(UIObjectRegistry.getInstance().constructUIRepositoryDirectory(dir, dirMap.get(dir.getParent() != null ? dir.getParent()
                      .getObjectId() : null), repository));
                } catch (UIObjectCreationException e) {
                  listOfObjects.add(new UIRepositoryDirectory(dir, dirMap.get(dir.getParent() != null ? dir.getParent()
                      .getObjectId() : null), repository));
                }
              } else {
                RepositoryObject c = (RepositoryObject) elem;
                if (c.getObjectType() == RepositoryObjectType.JOB) {
                  try {
                    listOfObjects.add(UIObjectRegistry.getInstance().constructUIJob(c, dirMap.get(c.getRepositoryDirectory().getObjectId()), repository));
                  } catch (UIObjectCreationException e) {
                    listOfObjects.add(new UIJob(c, dirMap.get(c.getRepositoryDirectory().getObjectId()), repository));
                  }
                  
                } else {
                  try {
                    listOfObjects.add(UIObjectRegistry.getInstance().constructUITransformation(c, dirMap.get(c.getRepositoryDirectory().getObjectId()),repository));
                  } catch (UIObjectCreationException e) {
                    listOfObjects.add(new UITransformation(c, dirMap.get(c.getRepositoryDirectory().getObjectId()),repository));
                  }
                }
              }
            }
            return listOfObjects;
          }

          @Override
          public List<RepositoryObjectInterface> targetToSource(UIRepositoryObjects elements) {
            return null;
          }
        });
  }

  @Override
  public void setSelectedFolderItems(List<UIRepositoryDirectory> selectedFolderItems) {
    if (selectedFolderItems != null && selectedFolderItems.size() == 1 && selectedFolderItems.get(0).equals(trashDir)) {
      try {
        setTrash(trashService.getTrash());
      } catch (KettleException e) {
        throw new RuntimeException(e);
      }
      deck.setSelectedIndex(1);
    } else {
      deck.setSelectedIndex(0);
      super.setSelectedFolderItems(selectedFolderItems);
    }
  }

  public void setTrash(List<RepositoryObjectInterface> trash) {
    this.trash = trash;
    firePropertyChange("trash", null, trash); //$NON-NLS-1$
  }

  public List<RepositoryObjectInterface> getTrash() {
    return trash;
  }

  @Override
  protected void moveFiles(List<UIRepositoryObject> objects, UIRepositoryDirectory targetDirectory) throws Exception {
    // If we're moving into the trash it's really a delete
    if (targetDirectory != trashDir) {
      super.moveFiles(objects, targetDirectory);
    } else {
      for (UIRepositoryObject o : objects) {
        deleteContent(o);
      }
    }
  }

  public void delete() {
    if (selectedTrashFileItems != null && selectedTrashFileItems.size() > 0) {
      List<ObjectId> ids = new ArrayList<ObjectId>();
      for (UIRepositoryObject uiObj : selectedTrashFileItems) {
        ids.add(uiObj.getObjectId());
      }
      try {
        trashService.delete(ids);
        setTrash(trashService.getTrash());
      } catch(Throwable th) {
        displayExceptionMessage(BaseMessages.getString(PKG,
            "TrashBrowseController.UnableToDeleteFile", th.getLocalizedMessage())); //$NON-NLS-1$
      }
    } else {
      // ui probably allowed the button to be enabled when it shouldn't have been enabled
      throw new RuntimeException();
    }
  }

  public void undelete(){
    // make a copy because the selected trash items changes as soon as trashService.undelete is called
    List<UIRepositoryObject> selectedTrashFileItemsSnapshot = new ArrayList<UIRepositoryObject>(selectedTrashFileItems);
    if (selectedTrashFileItemsSnapshot != null && selectedTrashFileItemsSnapshot.size() > 0) {
      List<ObjectId> ids = new ArrayList<ObjectId>();
      for (UIRepositoryObject uiObj : selectedTrashFileItemsSnapshot) {
        ids.add(uiObj.getObjectId());
      }
      try {
        trashService.undelete(ids);
        setTrash(trashService.getTrash());
        // Refresh the root directory once by refreshing the first directory or directory a trans/job was restored to
        for (UIRepositoryObject uiObj : selectedTrashFileItemsSnapshot) {
          if (uiObj instanceof UIRepositoryDirectory) {
            // refresh the whole tree since XUL cannot refresh a portion of the tree at this time
            ((UIRepositoryDirectory) uiObj).refresh();
            break;
          } else {
            uiObj.getParent().refresh();
            break;
          }
        }
        deck.setSelectedIndex(1);
      } catch(Throwable th) {
        displayExceptionMessage(BaseMessages.getString(PKG,
            "TrashBrowseController.UnableToRestoreFile", th.getLocalizedMessage())); //$NON-NLS-1$
      }
    } else {
      // ui probably allowed the button to be enabled when it shouldn't have been enabled
      throw new RuntimeException();
    }
  }

  public void setSelectedTrashFileItems(List<UIRepositoryObject> selectedTrashFileItems) {
    this.selectedTrashFileItems = selectedTrashFileItems;
  }
  
  @Override
  protected void deleteFolder(UIRepositoryDirectory repoDir) throws Exception{
    deleteContent(repoDir);
  }
   
  @Override
  protected void deleteContent(final UIRepositoryObject repoObject) throws Exception {
    try {
      repoObject.delete();
    } catch (KettleException ke) { 
      moveDeletePrompt(ke, repoObject, new XulDialogCallback<Object>() {

        public void onClose(XulComponent sender, Status returnCode, Object retVal) {
          if (returnCode == Status.ACCEPT) {
            try{
              ((UIEERepositoryDirectory)repoObject).delete(true);
            } catch (Exception e) {
              displayExceptionMessage(BaseMessages.getString(PKG, e.getLocalizedMessage()));
            }
          }
        }

        public void onError(XulComponent sender, Throwable t) {
          throw new RuntimeException(t);
        }
        
      });
    }
    
    if (repoObject instanceof UIRepositoryDirectory) {
      directoryBinding.fireSourceChanged();
      if(repoDir != null) {
        repoDir.refresh();        
      }
    }
    selectedItemsBinding.fireSourceChanged();
  }
  
  @Override
  protected void renameRepositoryObject(final UIRepositoryObject repoObject) throws XulException {
    final Document doc = document;
    XulPromptBox prompt = promptForName(repoObject);
    prompt.addDialogCallback(new XulDialogCallback<String>() {
      public void onClose(XulComponent component, Status status, String value) {
        if (status == Status.ACCEPT) {
          final String newName = value;
          try {
            repoObject.setName(newName);
          } catch (KettleException ke) {
            moveDeletePrompt(ke, repoObject, new XulDialogCallback<Object>() {

              public void onClose(XulComponent sender, Status returnCode, Object retVal) {
                if (returnCode == Status.ACCEPT) {
                  try{
                   ((UIEERepositoryDirectory)repoObject).setName(newName, true);
                  } catch (Exception e) {
                    displayExceptionMessage(BaseMessages.getString(PKG, e.getLocalizedMessage()));
                  }
                }
              }

              public void onError(XulComponent sender, Throwable t) {
                throw new RuntimeException(t);
              }
              
            });
          } catch (Exception e) {
            // convert to runtime exception so it bubbles up through the UI
            throw new RuntimeException(e);
          }
        }
      }

      public void onError(XulComponent component, Throwable err) {
        throw new RuntimeException(err);
      }
    });

    prompt.open();
  }
  
  
  
  protected boolean moveDeletePrompt(final KettleException ke, final UIRepositoryObject repoObject, final XulDialogCallback<Object> action) {
    if(ke.getCause() instanceof RepositoryObjectAccessException &&
        ((RepositoryObjectAccessException)ke.getCause()).getObjectAccessType().equals(RepositoryObjectAccessException.AccessExceptionType.USER_HOME_DIR) && 
        repoObject instanceof UIEERepositoryDirectory) {
        
      try {
        confirmBox = (XulConfirmBox) document.createElement("confirmbox");//$NON-NLS-1$
        confirmBox.setTitle(BaseMessages.getString(PKG, "TrashBrowseController.DeleteHomeFolderWarningTitle")); //$NON-NLS-1$
        confirmBox.setMessage(BaseMessages.getString(PKG, "TrashBrowseController.DeleteHomeFolderWarningMessage")); //$NON-NLS-1$
        confirmBox.setAcceptLabel(BaseMessages.getString(PKG, "Dialog.Ok")); //$NON-NLS-1$
        confirmBox.setCancelLabel(BaseMessages.getString(PKG, "Dialog.Cancel")); //$NON-NLS-1$
        confirmBox.addDialogCallback(new XulDialogCallback<Object>() {

          public void onClose(XulComponent sender, Status returnCode, Object retVal) {
            if (returnCode == Status.ACCEPT) {
              action.onClose(sender, returnCode, retVal);
            }
          }

          public void onError(XulComponent sender, Throwable t) {
            throw new RuntimeException(t);
          }
        });
        confirmBox.open();
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }
    return false;
  }
  
  protected void displayExceptionMessage(String msg) {
    messageBox.setTitle(BaseMessages.getString(PKG, "Dialog.Error")); //$NON-NLS-1$
    messageBox.setAcceptLabel(BaseMessages.getString(PKG, "Dialog.Ok")); //$NON-NLS-1$
    messageBox.setMessage(msg);
    messageBox.open();
  }

}
