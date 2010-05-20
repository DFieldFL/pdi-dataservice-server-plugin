package org.pentaho.di.repository.pur.model;

import org.pentaho.di.trans.TransMeta;

public class EETransMeta extends TransMeta implements ILockable{
  private RepositoryLock repositoryLock;

  /**
   * @return the repositoryLock
   */
  public RepositoryLock getRepositoryLock() {
    return repositoryLock;
  }

  /**
   * @param repositoryLock the repositoryLock to set
   */
  public void setRepositoryLock(RepositoryLock repositoryLock) {
    this.repositoryLock = repositoryLock;
  }
}