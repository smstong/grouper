package edu.internet2.middleware.grouper.app.provisioning;


public class ProvisioningStateGlobal {
  
  private GrouperProvisioner grouperProvisioner;
  
  private boolean selectResultProcessedGroups;
  
  private boolean selectResultProcessedEntities;

  private boolean selectResultProcessedMemberships;
<<<<<<< GROUPER_5_BRANCH
<<<<<<< GROUPER_5_BRANCH
  
  private boolean selectResultProcessedIndividualMemberships;
  
  

  
  public boolean isSelectResultProcessedIndividualMemberships() {
    return selectResultProcessedIndividualMemberships;
  }


  
  public void setSelectResultProcessedIndividualMemberships(
      boolean selectResultProcessedIndividualMemberships) {
    this.selectResultProcessedIndividualMemberships = selectResultProcessedIndividualMemberships;
  }

=======
>>>>>>> dad5d51 Provisioning related changes, wip
=======
  
  private boolean selectResultProcessedIndividualMemberships;
  
  

  
  public boolean isSelectResultProcessedIndividualMemberships() {
    return selectResultProcessedIndividualMemberships;
  }


  
  public void setSelectResultProcessedIndividualMemberships(
      boolean selectResultProcessedIndividualMemberships) {
    this.selectResultProcessedIndividualMemberships = selectResultProcessedIndividualMemberships;
  }

>>>>>>> d89ba3e Provisioning related changes. testing pending

  public GrouperProvisioner getGrouperProvisioner() {
    return grouperProvisioner;
  }

  
  public void setGrouperProvisioner(GrouperProvisioner grouperProvisioner) {
    this.grouperProvisioner = grouperProvisioner;
  }

  
  public boolean isSelectResultProcessedGroups() {
    return selectResultProcessedGroups;
  }

  
  public void setSelectResultProcessedGroups(boolean selectResultProcessedGroups) {
    this.selectResultProcessedGroups = selectResultProcessedGroups;
  }

  
  public boolean isSelectResultProcessedEntities() {
    return selectResultProcessedEntities;
  }

  
  public void setSelectResultProcessedEntities(boolean selectResultProcessedEntities) {
    this.selectResultProcessedEntities = selectResultProcessedEntities;
  }

  
  public boolean isSelectResultProcessedMemberships() {
    return selectResultProcessedMemberships;
  }

  
  public void setSelectResultProcessedMemberships(boolean selectResultProcessedMemberships) {
    this.selectResultProcessedMemberships = selectResultProcessedMemberships;
  }
  
}
