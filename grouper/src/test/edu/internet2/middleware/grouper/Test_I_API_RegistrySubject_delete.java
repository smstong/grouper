/*
  Copyright (C) 2004-2007 University Corporation for Advanced Internet Development, Inc.
  Copyright (C) 2004-2007 The University Of Chicago

  Licensed under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
*/

package edu.internet2.middleware.grouper;
import  edu.internet2.middleware.subject.Subject;

/**
 * Test <code>Group.delete()</code>.
 * @author  blair christensen.
 * @version $Id: Test_I_API_RegistrySubject_delete.java,v 1.1 2007-05-31 17:57:45 blair Exp $
 * @since   1.2.0
 */
public class Test_I_API_RegistrySubject_delete extends GrouperTest {

  // PRIVATE INSTANCE VARIABLES //
  private RegistrySubject rSubjX;
  private GrouperSession  s;
  private Subject         subjX;


  // TESTING INFRASTRUCTURE //

  public void setUp() {
    super.setUp();
    try {
      s       = GrouperSession.start( SubjectFinder.findRootSubject() );
      rSubjX  = RegistrySubject.add(s, "subjX", "person", "subjX");
      subjX   = SubjectFinder.findById( rSubjX.getId() );
    }
    catch (Exception eShouldNotHappen) {
      throw new GrouperRuntimeException( eShouldNotHappen.getMessage(), eShouldNotHappen );
    }
  }

  public void tearDown() {
    try {
      s.stop();
    }
    catch (Exception eShouldNotHappen) {
      throw new GrouperRuntimeException( eShouldNotHappen.getMessage(), eShouldNotHappen );
    }
    super.tearDown();
  }


  // TESTS //

  /**
   * Pass in a null <i>GrouperSession</i>.
   * @since   1.2.0
   */
  public void test_delete_failOnNullSession()
    throws  GrouperException,
            InsufficientPrivilegeException
  {
    try {
      rSubjX.delete(null);
      fail("failed to throw expected IllegalStateException");
    }
    catch (IllegalStateException eExpected) {
      assertTrue("threw expected IllegalStateException", true);
    }
  }

  /**
   * Fail to delete an existing <i>RegistrySubject</i> when not root-like.
   * @since   1.2.0
   */
  public void test_delete_failToDeleteWhenNotRoot() 
    throws  GrouperException,
            SessionException
  {
    GrouperSession nrs = GrouperSession.start(subjX);
    try {
      rSubjX.delete(nrs);
      fail("failed to throw expected InsufficientPrivilegeException");
    }
    catch (InsufficientPrivilegeException eExpected) {
      assertTrue("threw expected InsufficientPrivilegeException", true);
    }
    finally {
      nrs.stop();
    }
  }

  /**
   * Delete an existing <i>RegistrySubject</i>.
   * @since   1.2.0
   */
  public void test_delete_ok() 
    throws  GrouperException,
            InsufficientPrivilegeException
  {
    
    rSubjX.delete(s);
    assertTrue("deleted registry subject", true);
  }

  /**
   * Throw <i>GrouperException</i> when attempting to delete a non-existing <i>RegistrySubject</i>.
   * @since   1.2.0
   */
  public void test_delete_failToDeleteNoLongerExistingRegistrySubject() 
    throws  GrouperException,
            InsufficientPrivilegeException
  {
    rSubjX.delete(s);
    try {
      rSubjX.delete(s);
      fail("did not throw expected GrouperException");
    }
    catch (GrouperException expected) {
      assertTrue("threw expected GrouperException", true);
    }
  }

} 

