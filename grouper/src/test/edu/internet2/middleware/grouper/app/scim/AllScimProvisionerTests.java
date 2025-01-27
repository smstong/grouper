package edu.internet2.middleware.grouper.app.scim;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class AllScimProvisionerTests extends TestCase {
  
  public static Test suite() {
    TestSuite suite = new TestSuite(AllScimProvisionerTests.class.getName());
    //$JUnit-BEGIN$
    suite.addTestSuite(GrouperAwsProvisionerTest.class);
    suite.addTestSuite(GrouperAwsProvisionerTest2.class);
    suite.addTestSuite(GrouperGithubProvisionerTest.class);
    //$JUnit-END$
    return suite;
  }

}
