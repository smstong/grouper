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

package edu.internet2.middleware.grouper.internal.dao.hib3;
import  edu.internet2.middleware.grouper.internal.dao.GrouperDAOException;
import  edu.internet2.middleware.grouper.internal.dao.RegistrySubjectDAO;
import  edu.internet2.middleware.grouper.internal.dto.RegistrySubjectDTO;
import  edu.internet2.middleware.subject.*;
import  org.hibernate.*;

/**
 * Basic Hibernate <code>RegistrySubject</code> DAO interface.
 * <p><b>WARNING: THIS IS AN ALPHA INTERFACE THAT MAY CHANGE AT ANY TIME.</b></p>
 * @author  blair christensen.
 * @version $Id: Hib3RegistrySubjectDAO.java,v 1.1 2007-08-30 15:52:22 blair Exp $
 * @since   @HEAD@
 */
public class Hib3RegistrySubjectDAO extends Hib3DAO implements RegistrySubjectDAO {
  
  // PRIVATE INSTANCE VARIABLES //
  private String id;
  private String name;
  private String type;


  // PUBLIC INSTANCE METHODS //

  /**
   * @since   @HEAD@
   */
  public String create(RegistrySubjectDTO _subj)
    throws  GrouperDAOException 
  {
    try {
      Session       hs  = Hib3DAO.getSession();
      Transaction   tx  = hs.beginTransaction();
      Hib3DAO  dao = (Hib3DAO) _subj.getDAO();
      try {
        hs.save(dao);
        tx.commit();
      }
      catch (HibernateException eH) {
        tx.rollback();
        throw eH;
      }
      finally {
        hs.close();
      } 
      return dao.getId();
    }
    catch (HibernateException eH) {
      throw new GrouperDAOException( eH.getMessage(), eH );
    }
  } 

  /**
   * @since   @HEAD@
   */
  public void delete(RegistrySubjectDTO _subj)
    throws  GrouperDAOException
  {
    try {
      Session       hs  = Hib3DAO.getSession();
      Transaction   tx  = hs.beginTransaction();
      Hib3DAO  dao = (Hib3DAO) _subj.getDAO();
      try { 
        hs.delete(dao);
        tx.commit();
      }
      catch (HibernateException eH) {
        tx.rollback();
        throw eH;
      }
      finally {
        hs.close();
      }
    }
    catch (HibernateException eH) {
      throw new GrouperDAOException( eH.getMessage(), eH );
    }
  }

  /**
   * @since   @HEAD@
   */
  public RegistrySubjectDAO find(String id, String type) 
    throws  GrouperDAOException,
            SubjectNotFoundException
  {
    try {
      Session hs  = Hib3DAO.getSession();
      Query   qry = hs.createQuery(
        "from Hib3RegistrySubjectDAO as rs where " 
        + "     rs.id   = :id    "
        + " and rs.type = :type  "
      );
      qry.setCacheable(false); 
      qry.setString( "id",   id   );
      qry.setString( "type", type );
      Hib3RegistrySubjectDAO subj = (Hib3RegistrySubjectDAO) qry.uniqueResult();
      hs.close();
      if (subj == null) {
        throw new SubjectNotFoundException("subject not found"); 
      }
      return subj;
    }
    catch (HibernateException eH) {
      throw new GrouperDAOException( eH.getMessage(), eH );
    }
  } // public Hib3RegistrySubjectDAO find(id, type)

  /**
   * @since   @HEAD@
   */
  public String getId() {
    return this.id;
  }

  /**
   * @since   @HEAD@
   */
  public String getName() {
    return this.name;
  }

  /**
   * @since   @HEAD@
   */
  public String getType() {
    return this.type;
  }

  /**
   * @since   @HEAD@
   */
  public RegistrySubjectDAO setId(String id) {
    this.id = id;
    return this;
  }

  /**
   * @since   @HEAD@
   */
  public RegistrySubjectDAO setName(String name) {
    this.name = name;
    return this;
  }

  /**
   * @since   @HEAD@
   */
  public RegistrySubjectDAO setType(String type) {
    this.type = type;
    return this;
  }
   
 
  // PROTECTED CLASS METHODS //

  // @since   @HEAD@
  protected static void reset(Session hs) 
    throws  HibernateException
  {
    hs.createQuery("delete from Hib3RegistrySubjectAttributeDAO").executeUpdate();
    hs.createQuery("delete from Hib3RegistrySubjectDAO").executeUpdate();
  } 

} 

