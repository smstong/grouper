package edu.internet2.middleware.grouper.app.azure;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.internet2.middleware.grouper.Group;
import edu.internet2.middleware.grouper.GroupSave;
import edu.internet2.middleware.grouper.GrouperSession;
import edu.internet2.middleware.grouper.Stem;
import edu.internet2.middleware.grouper.StemSave;
import edu.internet2.middleware.grouper.app.loader.GrouperLoaderStatus;
import edu.internet2.middleware.grouper.app.loader.db.Hib3GrouperLoaderLog;
import edu.internet2.middleware.grouper.app.provisioning.GrouperProvisioner;
import edu.internet2.middleware.grouper.app.provisioning.GrouperProvisioningAttributeValue;
import edu.internet2.middleware.grouper.app.provisioning.GrouperProvisioningBaseTest;
import edu.internet2.middleware.grouper.app.provisioning.GrouperProvisioningOutput;
import edu.internet2.middleware.grouper.app.provisioning.GrouperProvisioningService;
import edu.internet2.middleware.grouper.app.provisioning.GrouperProvisioningType;
import edu.internet2.middleware.grouper.app.provisioning.ProvisioningEntityWrapper;
import edu.internet2.middleware.grouper.app.provisioning.ProvisioningGroupWrapper;
import edu.internet2.middleware.grouper.app.provisioning.ProvisioningMembershipWrapper;
import edu.internet2.middleware.grouper.changeLog.ChangeLogHelper;
import edu.internet2.middleware.grouper.changeLog.ChangeLogTempToEntity;
import edu.internet2.middleware.grouper.changeLog.esb.consumer.EsbConsumer;
import edu.internet2.middleware.grouper.helper.GrouperTest;
import edu.internet2.middleware.grouper.helper.SubjectTestHelper;
import edu.internet2.middleware.grouper.hibernate.HibernateSession;
import edu.internet2.middleware.grouper.misc.GrouperStartup;
import edu.internet2.middleware.grouper.misc.SaveMode;
import edu.internet2.middleware.grouper.util.CommandLineExec;
import edu.internet2.middleware.grouper.util.GrouperUtil;
import edu.internet2.middleware.grouperClient.jdbc.GcDbAccess;
import edu.internet2.middleware.grouperClient.jdbc.tableSync.GcGrouperSync;
import edu.internet2.middleware.grouperClient.jdbc.tableSync.GcGrouperSyncDao;
import edu.internet2.middleware.grouperClient.jdbc.tableSync.GcGrouperSyncGroup;
import junit.textui.TestRunner;


public class GrouperAzureProvisionerTest extends GrouperProvisioningBaseTest {
  
  public static void main(String[] args) {
    TestRunner.run(new GrouperAzureProvisionerTest("testIncrementalSyncAzure"));
  }

  public GrouperAzureProvisionerTest(String name) {
    super(name);
  }

  public GrouperAzureProvisionerTest() {
    
  }
  
  public static boolean startTomcat = false;
  
  public void setUp() {
    super.setUp();
    AzureProvisionerTestUtils.setupAzureExternalSystem();
  }
  
  @Override
  public String defaultConfigId() {
    return "myAzureProvisioner";
  }

  public void testFullSyncAzure() {
    
    GrouperStartup.startup();
    
    if (startTomcat) {
      CommandLineExec commandLineExec = tomcatStart();
    }
    
    try {
      AzureProvisionerTestUtils.configureAzureProvisioner(
          new AzureProvisionerTestConfigInput().assignGroupAttributeCount(9));
      
      // this will create tables
      List<GrouperAzureGroup> grouperAzureGroups = GrouperAzureApiCommands.retrieveAzureGroups("myAzure");
  
      new GcDbAccess().connectionName("grouper").sql("delete from mock_azure_membership").executeSql();
      new GcDbAccess().connectionName("grouper").sql("delete from mock_azure_group").executeSql();
      new GcDbAccess().connectionName("grouper").sql("delete from mock_azure_user").executeSql();
      
      GrouperSession grouperSession = GrouperSession.startRootSession();
      
      Stem stem = new StemSave(grouperSession).assignName("test").save();
      Stem stem2 = new StemSave(grouperSession).assignName("test2").save();
      
      // mark some folders to provision
      Group testGroup = new GroupSave(grouperSession).assignName("test:testGroup").save();
      Group testGroup2 = new GroupSave(grouperSession).assignName("test2:testGroup2").save();
      
      testGroup.addMember(SubjectTestHelper.SUBJ0, false);
      testGroup.addMember(SubjectTestHelper.SUBJ1, false);
      
      testGroup2.addMember(SubjectTestHelper.SUBJ2, false);
      testGroup2.addMember(SubjectTestHelper.SUBJ3, false);
      
      final GrouperProvisioningAttributeValue attributeValue = new GrouperProvisioningAttributeValue();
      attributeValue.setDirectAssignment(true);
      attributeValue.setDoProvision("myAzureProvisioner");
      attributeValue.setTargetName("myAzureProvisioner");
      attributeValue.setStemScopeString("sub");
      Map<String, Object> metadataNameValues = new HashMap<String, Object>();
      metadataNameValues.put("md_grouper_allowOnlyMembersToPost", true);
      metadataNameValues.put("md_grouper_resourceProvisioningOptionsTeam", true);
      attributeValue.setMetadataNameValues(metadataNameValues);
  
      GrouperProvisioningService.saveOrUpdateProvisioningAttributes(attributeValue, stem);
  
      //lets sync these over
      
      assertEquals(new Integer(0), new GcDbAccess().connectionName("grouper").sql("select count(1) from mock_azure_group").select(int.class));
  
      assertEquals(0, HibernateSession.byHqlStatic().createQuery("from GrouperAzureGroup").list(GrouperAzureGroup.class).size());
      
      GrouperProvisioningOutput grouperProvisioningOutput = fullProvision();
      GrouperProvisioner grouperProvisioner = GrouperProvisioner.retrieveInternalLastProvisioner();
      
      assertTrue(1 <= grouperProvisioningOutput.getInsert());
      assertEquals(1, HibernateSession.byHqlStatic().createQuery("from GrouperAzureGroup").list(GrouperAzureGroup.class).size());
      assertEquals(2, HibernateSession.byHqlStatic().createQuery("from GrouperAzureUser").list(GrouperAzureUser.class).size());
      assertEquals(2, HibernateSession.byHqlStatic().createQuery("from GrouperAzureMembership").list(GrouperAzureMembership.class).size());
      GrouperAzureGroup grouperAzureGroup = HibernateSession.byHqlStatic().createQuery("from GrouperAzureGroup").list(GrouperAzureGroup.class).get(0);
      
      assertTrue(GrouperUtil.length(grouperProvisioner.retrieveGrouperProvisioningData().getProvisioningGroupWrappers()) > 0);
      
      for (ProvisioningGroupWrapper provisioningGroupWrapper: grouperProvisioner.retrieveGrouperProvisioningData().getProvisioningGroupWrappers()) {
        assertTrue(provisioningGroupWrapper.isRecalc());
      }
      
      assertTrue(GrouperUtil.length(grouperProvisioner.retrieveGrouperProvisioningData().getProvisioningEntityWrappers()) > 0);
      
      for (ProvisioningEntityWrapper provisioningEntityWrapper: grouperProvisioner.retrieveGrouperProvisioningData().getProvisioningEntityWrappers()) {
        assertTrue(provisioningEntityWrapper.isRecalc());
      }
      
      assertTrue(GrouperUtil.length(grouperProvisioner.retrieveGrouperProvisioningData().getProvisioningMembershipWrappers()) > 0);
      
      for (ProvisioningMembershipWrapper provisioningMembershipWrapper: grouperProvisioner.retrieveGrouperProvisioningData().getProvisioningMembershipWrappers()) {
        assertTrue(provisioningMembershipWrapper.isRecalc());
      }
      
      assertEquals("test:testGroup", grouperAzureGroup.getDisplayName());
      assertEquals("T", grouperAzureGroup.getResourceBehaviorOptionsAllowOnlyMembersToPostDb());
      assertEquals("T", grouperAzureGroup.getResourceBehaviorOptionsWelcomeEmailDisabledDb());
      assertEquals("T", grouperAzureGroup.getResourceProvisioningOptionsTeamDb());
      
      GcGrouperSync gcGrouperSync = GcGrouperSyncDao.retrieveByProvisionerName(null, "myAzureProvisioner");
      assertEquals(1, gcGrouperSync.getGroupCount().intValue());
      
      GcGrouperSyncGroup gcGrouperSyncGroup = gcGrouperSync.getGcGrouperSyncGroupDao().groupRetrieveByGroupId(testGroup.getId());
      assertEquals(testGroup.getId(), gcGrouperSyncGroup.getGroupId());
      assertEquals(testGroup.getName(), gcGrouperSyncGroup.getGroupName());
      assertEquals(grouperAzureGroup.getId(), gcGrouperSyncGroup.getGroupAttributeValueCache2());
      
      
      //now remove one of the subjects from the testGroup
      testGroup.deleteMember(SubjectTestHelper.SUBJ1);
      
      // now run the full sync again and the member should be deleted from mock_azure_membership also
      
      grouperProvisioningOutput = fullProvision();
      grouperProvisioner = GrouperProvisioner.retrieveInternalLastProvisioner();
      
      assertEquals(1, HibernateSession.byHqlStatic().createQuery("from GrouperAzureGroup").list(GrouperAzureGroup.class).size());
      assertEquals(2, HibernateSession.byHqlStatic().createQuery("from GrouperAzureUser").list(GrouperAzureUser.class).size());
      assertEquals(1, HibernateSession.byHqlStatic().createQuery("from GrouperAzureMembership").list(GrouperAzureMembership.class).size());
      
      //now add one subject
      testGroup.addMember(SubjectTestHelper.SUBJ3);
      
      // now run the full sync again
      grouperProvisioningOutput = fullProvision();
      grouperProvisioner = GrouperProvisioner.retrieveInternalLastProvisioner();

      assertEquals(1, HibernateSession.byHqlStatic().createQuery("from GrouperAzureGroup").list(GrouperAzureGroup.class).size());
      assertEquals(3, HibernateSession.byHqlStatic().createQuery("from GrouperAzureUser").list(GrouperAzureUser.class).size());
      assertEquals(2, HibernateSession.byHqlStatic().createQuery("from GrouperAzureMembership").list(GrouperAzureMembership.class).size());
      
      //now delete the group and sync again
      testGroup.delete();
      
      grouperProvisioningOutput = fullProvision();
      grouperProvisioner = GrouperProvisioner.retrieveInternalLastProvisioner();
      
      assertEquals(0, HibernateSession.byHqlStatic().createQuery("from GrouperAzureGroup").list(GrouperAzureGroup.class).size());
      assertEquals(3, HibernateSession.byHqlStatic().createQuery("from GrouperAzureUser").list(GrouperAzureUser.class).size());
      assertEquals(0, HibernateSession.byHqlStatic().createQuery("from GrouperAzureMembership").list(GrouperAzureMembership.class).size());
      
    } finally {
      
    }

  }
  
  public void testFullSyncAzureDisplayName() {
    
    GrouperStartup.startup();
    
    if (startTomcat) {
      CommandLineExec commandLineExec = tomcatStart();
    }
    
    try {
      AzureProvisionerTestUtils.configureAzureProvisioner(
          new AzureProvisionerTestConfigInput().assignGroupAttributeCount(9).assignDisplayNameMapping("displayName"));
      
      // this will create tables
      List<GrouperAzureGroup> grouperAzureGroups = GrouperAzureApiCommands.retrieveAzureGroups("myAzure");
  
      new GcDbAccess().connectionName("grouper").sql("delete from mock_azure_membership").executeSql();
      new GcDbAccess().connectionName("grouper").sql("delete from mock_azure_group").executeSql();
      new GcDbAccess().connectionName("grouper").sql("delete from mock_azure_user").executeSql();
      
      GrouperSession grouperSession = GrouperSession.startRootSession();
      
      Stem stem = new StemSave(grouperSession).assignName("test").save();
      Stem stem2 = new StemSave(grouperSession).assignName("test2").save();
      
      // mark some folders to provision
      Group testGroup = new GroupSave(grouperSession).assignName("test:testGroup").save();
      Group testGroup2 = new GroupSave(grouperSession).assignName("test2:testGroup2").save();
      
      testGroup.addMember(SubjectTestHelper.SUBJ0, false);
      testGroup.addMember(SubjectTestHelper.SUBJ1, false);
      
      testGroup2.addMember(SubjectTestHelper.SUBJ2, false);
      testGroup2.addMember(SubjectTestHelper.SUBJ3, false);
      
      final GrouperProvisioningAttributeValue attributeValue = new GrouperProvisioningAttributeValue();
      attributeValue.setDirectAssignment(true);
      attributeValue.setDoProvision("myAzureProvisioner");
      attributeValue.setTargetName("myAzureProvisioner");
      attributeValue.setStemScopeString("sub");
      Map<String, Object> metadataNameValues = new HashMap<String, Object>();
      metadataNameValues.put("md_grouper_allowOnlyMembersToPost", true);
      metadataNameValues.put("md_grouper_resourceProvisioningOptionsTeam", true);
      attributeValue.setMetadataNameValues(metadataNameValues);
  
      GrouperProvisioningService.saveOrUpdateProvisioningAttributes(attributeValue, stem);
  
      //lets sync these over
      
      assertEquals(new Integer(0), new GcDbAccess().connectionName("grouper").sql("select count(1) from mock_azure_group").select(int.class));
  
      assertEquals(0, HibernateSession.byHqlStatic().createQuery("from GrouperAzureGroup").list(GrouperAzureGroup.class).size());
      
      GrouperProvisioningOutput grouperProvisioningOutput = fullProvision();
      GrouperProvisioner grouperProvisioner = GrouperProvisioner.retrieveInternalLastProvisioner();
      
      assertTrue(1 <= grouperProvisioningOutput.getInsert());
      assertEquals(1, HibernateSession.byHqlStatic().createQuery("from GrouperAzureGroup").list(GrouperAzureGroup.class).size());
      assertEquals(2, HibernateSession.byHqlStatic().createQuery("from GrouperAzureUser").list(GrouperAzureUser.class).size());
      assertEquals(2, HibernateSession.byHqlStatic().createQuery("from GrouperAzureMembership").list(GrouperAzureMembership.class).size());
      GrouperAzureGroup grouperAzureGroup = HibernateSession.byHqlStatic().createQuery("from GrouperAzureGroup").list(GrouperAzureGroup.class).get(0);
      
      assertTrue(GrouperUtil.length(grouperProvisioner.retrieveGrouperProvisioningData().getProvisioningGroupWrappers()) > 0);
      
      for (ProvisioningGroupWrapper provisioningGroupWrapper: grouperProvisioner.retrieveGrouperProvisioningData().getProvisioningGroupWrappers()) {
        assertTrue(provisioningGroupWrapper.isRecalc());
      }
      
      assertTrue(GrouperUtil.length(grouperProvisioner.retrieveGrouperProvisioningData().getProvisioningEntityWrappers()) > 0);
      
      for (ProvisioningEntityWrapper provisioningEntityWrapper: grouperProvisioner.retrieveGrouperProvisioningData().getProvisioningEntityWrappers()) {
        assertTrue(provisioningEntityWrapper.isRecalc());
      }
      
      assertTrue(GrouperUtil.length(grouperProvisioner.retrieveGrouperProvisioningData().getProvisioningMembershipWrappers()) > 0);
      
      for (ProvisioningMembershipWrapper provisioningMembershipWrapper: grouperProvisioner.retrieveGrouperProvisioningData().getProvisioningMembershipWrappers()) {
        assertTrue(provisioningMembershipWrapper.isRecalc());
      }
      
      assertEquals("test:testGroup", grouperAzureGroup.getDisplayName());
      assertEquals("T", grouperAzureGroup.getResourceBehaviorOptionsAllowOnlyMembersToPostDb());
      assertEquals("T", grouperAzureGroup.getResourceBehaviorOptionsWelcomeEmailDisabledDb());
      assertEquals("T", grouperAzureGroup.getResourceProvisioningOptionsTeamDb());
      
      GcGrouperSync gcGrouperSync = GcGrouperSyncDao.retrieveByProvisionerName(null, "myAzureProvisioner");
      assertEquals(1, gcGrouperSync.getGroupCount().intValue());
      
      GcGrouperSyncGroup gcGrouperSyncGroup = gcGrouperSync.getGcGrouperSyncGroupDao().groupRetrieveByGroupId(testGroup.getId());
      assertEquals(testGroup.getId(), gcGrouperSyncGroup.getGroupId());
      assertEquals(testGroup.getName(), gcGrouperSyncGroup.getGroupName());
      assertEquals(grouperAzureGroup.getId(), gcGrouperSyncGroup.getGroupAttributeValueCache2());
      
      
      //now remove one of the subjects from the testGroup
      testGroup.deleteMember(SubjectTestHelper.SUBJ1);
      
      // now run the full sync again and the member should be deleted from mock_azure_membership also
      grouperProvisioningOutput = fullProvision();
      grouperProvisioner = GrouperProvisioner.retrieveInternalLastProvisioner();
      assertEquals(1, HibernateSession.byHqlStatic().createQuery("from GrouperAzureGroup").list(GrouperAzureGroup.class).size());
      assertEquals(2, HibernateSession.byHqlStatic().createQuery("from GrouperAzureUser").list(GrouperAzureUser.class).size());
      assertEquals(1, HibernateSession.byHqlStatic().createQuery("from GrouperAzureMembership").list(GrouperAzureMembership.class).size());
      
      //now add one subject
      testGroup.addMember(SubjectTestHelper.SUBJ3);
      
      // now run the full sync again
      grouperProvisioningOutput = fullProvision();
      grouperProvisioner = GrouperProvisioner.retrieveInternalLastProvisioner();
      assertEquals(1, HibernateSession.byHqlStatic().createQuery("from GrouperAzureGroup").list(GrouperAzureGroup.class).size());
      assertEquals(3, HibernateSession.byHqlStatic().createQuery("from GrouperAzureUser").list(GrouperAzureUser.class).size());
      assertEquals(2, HibernateSession.byHqlStatic().createQuery("from GrouperAzureMembership").list(GrouperAzureMembership.class).size());
      
      // now edit the group
      testGroup = new GroupSave().assignUuid(testGroup.getUuid()).assignDisplayExtension("newDisplayExtension").assignReplaceAllSettings(false).save();
      
      grouperProvisioningOutput = fullProvision();
      grouperProvisioner = GrouperProvisioner.retrieveInternalLastProvisioner();
      
      assertEquals(1, HibernateSession.byHqlStatic().createQuery("from GrouperAzureGroup").list(GrouperAzureGroup.class).size());
      assertEquals(3, HibernateSession.byHqlStatic().createQuery("from GrouperAzureUser").list(GrouperAzureUser.class).size());
      assertEquals(2, HibernateSession.byHqlStatic().createQuery("from GrouperAzureMembership").list(GrouperAzureMembership.class).size());
      
      GrouperAzureGroup azureGroup = HibernateSession.byHqlStatic().createQuery("from GrouperAzureGroup").list(GrouperAzureGroup.class).get(0);
      assertEquals("test:newDisplayExtension", azureGroup.getDisplayName());
      
      //now delete the group and sync again
      testGroup.delete();
      
      grouperProvisioningOutput = fullProvision();
      grouperProvisioner = GrouperProvisioner.retrieveInternalLastProvisioner();
      
      assertEquals(0, HibernateSession.byHqlStatic().createQuery("from GrouperAzureGroup").list(GrouperAzureGroup.class).size());
      assertEquals(3, HibernateSession.byHqlStatic().createQuery("from GrouperAzureUser").list(GrouperAzureUser.class).size());
      assertEquals(0, HibernateSession.byHqlStatic().createQuery("from GrouperAzureMembership").list(GrouperAzureMembership.class).size());
      
    } finally {
      
    }

  }
  
  public void testIncrementalSyncAzure() {
    
    if (!tomcatRunTests()) {
      return;
    }

    
    AzureProvisionerTestUtils.configureAzureProvisioner(
       new AzureProvisionerTestConfigInput());

    GrouperStartup.startup();
    

    if (startTomcat) {
      CommandLineExec commandLineExec = tomcatStart();
    }
    
    try {
      // this will create tables
      List<GrouperAzureGroup> grouperAzureGroups = GrouperAzureApiCommands.retrieveAzureGroups("myAzure");
  
      new GcDbAccess().connectionName("grouper").sql("delete from mock_azure_membership").executeSql();
      new GcDbAccess().connectionName("grouper").sql("delete from mock_azure_group").executeSql();
      new GcDbAccess().connectionName("grouper").sql("delete from mock_azure_user").executeSql();
      
      assertEquals(new Integer(0), new GcDbAccess().connectionName("grouper").sql("select count(1) from mock_azure_group").select(int.class));
      assertEquals(0, HibernateSession.byHqlStatic().createQuery("from GrouperAzureGroup").list(GrouperAzureGroup.class).size());
      
      fullProvision();
      
      incrementalProvision();
      GrouperProvisioner grouperProvisioner = GrouperProvisioner.retrieveInternalLastProvisioner();
      GrouperProvisioningOutput grouperProvisioningOutput = grouperProvisioner.retrieveGrouperProvisioningOutput(); 

      GrouperSession grouperSession = GrouperSession.startRootSession();
      
      Stem stem = new StemSave(grouperSession).assignName("test").save();
      Stem stem2 = new StemSave(grouperSession).assignName("test2").save();
      
      // mark some folders to provision
      Group testGroup = new GroupSave(grouperSession).assignName("test:testGroup").save();
      Group testGroup2 = new GroupSave(grouperSession).assignName("test2:testGroup2").save();
      
      testGroup.addMember(SubjectTestHelper.SUBJ0, false);
      testGroup.addMember(SubjectTestHelper.SUBJ1, false);
      
      testGroup2.addMember(SubjectTestHelper.SUBJ2, false);
      testGroup2.addMember(SubjectTestHelper.SUBJ3, false);
      
      final GrouperProvisioningAttributeValue attributeValue = new GrouperProvisioningAttributeValue();
      attributeValue.setDirectAssignment(true);
      attributeValue.setDoProvision("myAzureProvisioner");
      attributeValue.setTargetName("myAzureProvisioner");
      attributeValue.setStemScopeString("sub");
  
      GrouperProvisioningService.saveOrUpdateProvisioningAttributes(attributeValue, stem);
  
      assertEquals(new Integer(0), new GcDbAccess().connectionName("grouper").sql("select count(1) from mock_azure_group").select(int.class));
      assertEquals(0, HibernateSession.byHqlStatic().createQuery("from GrouperAzureGroup").list(GrouperAzureGroup.class).size());
      
      incrementalProvision();
      
      assertEquals(1, HibernateSession.byHqlStatic().createQuery("from GrouperAzureGroup").list(GrouperAzureGroup.class).size());
      assertEquals(2, HibernateSession.byHqlStatic().createQuery("from GrouperAzureUser").list(GrouperAzureUser.class).size());
      assertEquals(2, HibernateSession.byHqlStatic().createQuery("from GrouperAzureMembership").list(GrouperAzureMembership.class).size());
      
      //now remove one of the subjects from the testGroup
      testGroup.deleteMember(SubjectTestHelper.SUBJ1);

      incrementalProvision();
      
      assertEquals(1, HibernateSession.byHqlStatic().createQuery("from GrouperAzureGroup").list(GrouperAzureGroup.class).size());
      assertEquals(2, HibernateSession.byHqlStatic().createQuery("from GrouperAzureUser").list(GrouperAzureUser.class).size());
      assertEquals(1, HibernateSession.byHqlStatic().createQuery("from GrouperAzureMembership").list(GrouperAzureMembership.class).size());
      
      testGroup.addMember(SubjectTestHelper.SUBJ3);
      incrementalProvision();
      
      assertEquals(1, HibernateSession.byHqlStatic().createQuery("from GrouperAzureGroup").list(GrouperAzureGroup.class).size());
      assertEquals(3, HibernateSession.byHqlStatic().createQuery("from GrouperAzureUser").list(GrouperAzureUser.class).size());
      assertEquals(2, HibernateSession.byHqlStatic().createQuery("from GrouperAzureMembership").list(GrouperAzureMembership.class).size());
      
      //now delete the group and sync again
      testGroup.delete();
      incrementalProvision();
      
      //assertEquals(1, grouperProvisioningOutput.getDelete());
      assertEquals(0, HibernateSession.byHqlStatic().createQuery("from GrouperAzureGroup").list(GrouperAzureGroup.class).size());
      assertEquals(3, HibernateSession.byHqlStatic().createQuery("from GrouperAzureUser").list(GrouperAzureUser.class).size());
      assertEquals(0, HibernateSession.byHqlStatic().createQuery("from GrouperAzureMembership").list(GrouperAzureMembership.class).size());
      
    } finally {
//      tomcatStop();
//      if (commandLineExec != null) {
//        GrouperUtil.threadJoin(commandLineExec.getThread());
//      }
    }
    
  }
  
  public void testFullSyncAzureGroupType() {
    
    GrouperStartup.startup();
    
    if (startTomcat) {
      CommandLineExec commandLineExec = tomcatStart();
    }
    
    try {
      AzureProvisionerTestUtils.configureAzureProvisioner(
          new AzureProvisionerTestConfigInput().assignGroupAttributeCount(9));
      
      // this will create tables
      List<GrouperAzureGroup> grouperAzureGroups = GrouperAzureApiCommands.retrieveAzureGroups("myAzure");
  
      new GcDbAccess().connectionName("grouper").sql("delete from mock_azure_membership").executeSql();
      new GcDbAccess().connectionName("grouper").sql("delete from mock_azure_group").executeSql();
      new GcDbAccess().connectionName("grouper").sql("delete from mock_azure_user").executeSql();
      
      GrouperSession grouperSession = GrouperSession.startRootSession();
      
      Stem stem = new StemSave(grouperSession).assignName("test").save();
      
      // mark some folders to provision
      Group testGroup = new GroupSave(grouperSession).assignName("test:testGroup").save();
      
      testGroup.addMember(SubjectTestHelper.SUBJ0, false);
      testGroup.addMember(SubjectTestHelper.SUBJ1, false);
      
      final GrouperProvisioningAttributeValue attributeValue = new GrouperProvisioningAttributeValue();
      attributeValue.setDirectAssignment(true);
      attributeValue.setDoProvision("myAzureProvisioner");
      attributeValue.setTargetName("myAzureProvisioner");
      attributeValue.setStemScopeString("sub");
      Map<String, Object> metadataNameValues = new HashMap<String, Object>();
      metadataNameValues.put("md_grouper_allowOnlyMembersToPost", true);
      metadataNameValues.put("md_grouper_resourceProvisioningOptionsTeam", true);
      metadataNameValues.put("md_grouper_azureGroupType", "security");
      metadataNameValues.put("md_grouper_assignableToRole", true);
      attributeValue.setMetadataNameValues(metadataNameValues);
  
      GrouperProvisioningService.saveOrUpdateProvisioningAttributes(attributeValue, stem);
  
      //lets sync these over
      
      assertEquals(new Integer(0), new GcDbAccess().connectionName("grouper").sql("select count(1) from mock_azure_group").select(int.class));
  
      assertEquals(0, HibernateSession.byHqlStatic().createQuery("from GrouperAzureGroup").list(GrouperAzureGroup.class).size());
      
      GrouperProvisioningOutput grouperProvisioningOutput = fullProvision();
      GrouperProvisioner grouperProvisioner = GrouperProvisioner.retrieveInternalLastProvisioner();
      assertTrue(1 <= grouperProvisioningOutput.getInsert());
      assertEquals(1, HibernateSession.byHqlStatic().createQuery("from GrouperAzureGroup").list(GrouperAzureGroup.class).size());
      assertEquals(2, HibernateSession.byHqlStatic().createQuery("from GrouperAzureUser").list(GrouperAzureUser.class).size());
      assertEquals(2, HibernateSession.byHqlStatic().createQuery("from GrouperAzureMembership").list(GrouperAzureMembership.class).size());
      GrouperAzureGroup grouperAzureGroup = HibernateSession.byHqlStatic().createQuery("from GrouperAzureGroup").list(GrouperAzureGroup.class).get(0);
      
      assertTrue(GrouperUtil.length(grouperProvisioner.retrieveGrouperProvisioningData().getProvisioningGroupWrappers()) > 0);
      
      for (ProvisioningGroupWrapper provisioningGroupWrapper: grouperProvisioner.retrieveGrouperProvisioningData().getProvisioningGroupWrappers()) {
        assertTrue(provisioningGroupWrapper.isRecalc());
      }
      
      assertTrue(GrouperUtil.length(grouperProvisioner.retrieveGrouperProvisioningData().getProvisioningEntityWrappers()) > 0);
      
      for (ProvisioningEntityWrapper provisioningEntityWrapper: grouperProvisioner.retrieveGrouperProvisioningData().getProvisioningEntityWrappers()) {
        assertTrue(provisioningEntityWrapper.isRecalc());
      }
      
      assertTrue(GrouperUtil.length(grouperProvisioner.retrieveGrouperProvisioningData().getProvisioningMembershipWrappers()) > 0);
      
      for (ProvisioningMembershipWrapper provisioningMembershipWrapper: grouperProvisioner.retrieveGrouperProvisioningData().getProvisioningMembershipWrappers()) {
        assertTrue(provisioningMembershipWrapper.isRecalc());
      }
      
      assertEquals("test:testGroup", grouperAzureGroup.getDisplayName());
      assertEquals("T", grouperAzureGroup.getResourceBehaviorOptionsAllowOnlyMembersToPostDb());
      assertEquals("T", grouperAzureGroup.getResourceBehaviorOptionsWelcomeEmailDisabledDb());
      assertEquals("T", grouperAzureGroup.getResourceProvisioningOptionsTeamDb());
      
      assertTrue(grouperAzureGroup.isSecurityEnabled());
      assertFalse(grouperAzureGroup.isMailEnabled());
      assertFalse(grouperAzureGroup.isGroupTypeUnified());
      assertFalse(grouperAzureGroup.isGroupTypeDynamic());
      assertTrue(grouperAzureGroup.isAssignableToRole());
      
    } finally {
      
    }

  }
  
}
