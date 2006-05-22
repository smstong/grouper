/*
  Copyright 2004-2006 University Corporation for Advanced Internet Development, Inc.
  Copyright 2004-2006 The University Of Chicago

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


import  java.io.Serializable;
import  java.util.*;
import  net.sf.hibernate.*;
import  org.apache.commons.logging.*;


/** 
 * Perform <i>member of</i> calculation.
 * <p />
 * @author  blair christensen.
 * @version $Id: MemberOf.java,v 1.14.2.8 2006-05-22 18:36:15 blair Exp $
 */
class MemberOf implements Serializable {

  // Protected Class Constants //
  protected static final String ERR_CT  = "invalid composite type: ";

  // Private Class Constants //
  private static final Log LOG = LogFactory.getLog(MemberOf.class);


  // Private Instance Variables //
  private Composite       c;
  private Set             deletes     = new LinkedHashSet();
  private Set             effDeletes  = new LinkedHashSet();
  private Set             effSaves    = new LinkedHashSet();
  private Field           f;
  private Member          m;
  private Membership      ms;
  private Owner           o;
  private Set             pending     = new LinkedHashSet();
  private GrouperSession  root;
  private GrouperSession  s;    
  private Set             saves       = new LinkedHashSet();


  // Constructors //
  private MemberOf(GrouperSession s, Owner o, Member m, Membership ms) {
    this.f  = ms.getList();
    this.m  = m;
    this.ms = ms;
    this.o  = o;
    this.s  = s;
    this._setSessions();
  } // private MemberOf(s, o, m, ms)

  private MemberOf(GrouperSession s, Owner o, Composite c) {
    this.c    = c;
    this.f    = Group.getDefaultList();
    this.o    = o;
    this.s    = s;
    this._setSessions();
  } // private MemberOf(s, o, c)


  // Protected Class Methods //
 
  // Calculate addition of a composite membership 
  protected static MemberOf addComposite(
    GrouperSession s, Owner o, Composite c
  )
    throws  ModelException
  {
    MemberOf mof = new MemberOf(s, o, c);
    mof.effSaves.addAll(  mof._evalComposite()  );  // Find the composites
    mof.saves.addAll(     mof.effSaves          );
    mof._resetSessions();
    mof.o.setModified();
    mof.saves.add(mof.c);     // Save the composite
    mof.saves.add(mof.o);     // Update the owner
    return mof;
  } // protected static MemberOf addComposite(s, o, c)

  protected static MemberOf addImmediate(
    GrouperSession s, Owner o, Membership ms, Member m
  )
    throws  GroupNotFoundException, // TODO 
            MemberNotFoundException // TODO
  {
    MemberOf  mof = new MemberOf(s, o, m, ms);
    mof.saves.add(m);       // Save the member
    mof.effSaves.addAll( mof._evalImmediate() );  // Find the new effs
    mof.saves.addAll(   mof.effSaves          );
    mof._resetSessions();
    mof.o.setModified();
    mof.saves.add(ms);      // Save the immediate
    mof.saves.add(mof.o);   // Update the owner
    return mof;
  } // protected static MemberOf addImmediate(s, o, m, ms)

  // Calculate deletion of a composite membership 
  //  TODO  Why do I need to include o?  Can't I just get that from c?
  protected static MemberOf delComposite(
    GrouperSession s, Owner o, Composite c
  )
    throws  ModelException
  {
    //  TODO  I'm really uncertain about this code.  Expect it to be
    //        both flawed and evolving for quite some time.
    MemberOf  mof   = new MemberOf(s, o, c);

    //  We will need to update where this group is a composite factor
    //  after we have take care of everything else.  Argh.
    mof.pending.addAll( CompositeFinder.isFactor(o) );

    //  Delete this group's members
    //  TODO  I have performance concerns with this code
    Iterator  iterH = ( (Group) o ).getMemberships().iterator();
    while (iterH.hasNext()) {
      Membership  ms    = (Membership) iterH.next();
      try {
        MemberOf    msMof = MemberOf.delImmediate(
          s, o, ms, ms.getMember()
        );
        mof.deletes.addAll(     msMof.getDeletes()    );
      }
      catch (Exception e) {
        throw new ModelException(e);
      }
    }

    mof._resetSessions();
    mof.o.setModified();
    mof.deletes.add(mof.c);   // Delete the composite
    mof.saves.add(mof.o);     // Update the owner
    return mof;
  } // protected static MemberOf delComposite(s, o, c)

  protected static MemberOf delImmediate(
    GrouperSession s, Owner o, Membership ms, Member m
  )
    throws  GroupNotFoundException,   // TODO
            HibernateException,
            MemberNotFoundException,  // TODO
            MembershipNotFoundException
  {
    MemberOf  mof = new MemberOf(s, o, m, ms);
    // TODO This seems overly complicated.  Why don't I just query for
    //      the appropriate members?
    mof.effDeletes.addAll( Membership.getPersistent(s, mof._evalImmediate()) );
    mof.deletes.addAll(mof.effDeletes);
    mof.deletes.add(ms);
    mof._resetSessions();
    mof.o.setModified();
    mof.deletes.add(ms);    // Delete the immediate
    mof.saves.add(mof.o);   // Update the owner
    return mof;
  } // protected static MemberOf delImmediate(s, o, m, ms)


  // Protected Instance Methods //
  protected Set getDeletes() {
    return this.deletes;
  } // protected Set getDeletes()
  protected Set getEffDeletes() {
    return this.effDeletes;
  } // protected Set getEffDeletes()
  protected Set getEffSaves() {
    return this.effSaves;
  } // protected Set getEffSaves()
  protected Set getPending() {
    return this.pending;
  } // protected Set getPending()
  protected Set getSaves() {
    return this.saves;
  } // protected Set getSaves()


  // Private Instance Methods //

  // Add m's hasMembers to o
  private Set _addHasMembersToOwner(Set hasMembers) 
    throws  GroupNotFoundException, // TODO
            MemberNotFoundException
  {
    Set     mships  = new LinkedHashSet();
    Iterator iter = hasMembers.iterator();
    while (iter.hasNext()) {
      Membership hasMS = (Membership) iter.next();
      Membership  eff = Membership.newEffectiveMembership(
        this.s, this.ms, hasMS, 1
      );
      mships.add(eff);
    }
    return mships;
  } // private Set _addHasMembersToOwner(hasMembers)

  // Add m's hasMembers to where g isMember
  private Set _addHasMembersToWhereGroupIsMember(Set isMember, Set hasMembers) 
    throws  GroupNotFoundException, // TODO
            MemberNotFoundException
  {
    Set     mships  = new LinkedHashSet();

    // Add the members of m to where g is a member but only if f == "members"
    if (this.f.equals(Group.getDefaultList())) {
      Iterator isIter = isMember.iterator();
      while (isIter.hasNext()) {
        Membership isMS = (Membership) isIter.next();
        Iterator hasIter = hasMembers.iterator();
        while (hasIter.hasNext()) {
          Membership  hasMS = (Membership) hasIter.next();
          Membership  eff   = Membership.newEffectiveMembership(
            this.s, isMS, hasMS, 2
          );
          mships.add(eff);
        }
      }
    }

    return mships;
  } // private Set _addHasMembersToWhereGroupIsMember(isMember, hasMembers)

  // Add m to where g isMember
  private Set _addMemberToWhereGroupIsMember(Set isMember) 
    throws  GroupNotFoundException,
            MemberNotFoundException
  {
    Set     mships  = new LinkedHashSet();

    // Add m to where g is a member if f == "members"
    if (this.f.equals(Group.getDefaultList())) {
      Iterator isIter = isMember.iterator();
      while (isIter.hasNext()) {
        Membership  isMS  = (Membership) isIter.next();
        Membership  eff   = Membership.newEffectiveMembership(
          this.s, isMS, this.ms, 1
        );
        mships.add(eff);
      }
    }

    return mships;
  } // private Set _addHasMembersToWhereGroupIsMember(isMember, hasMembers)

  // Convert a set of memberships into composite memberships
  private Set _createNewMembershipObjects(Set members) 
    throws  ModelException
  {
    Set       mships  = new LinkedHashSet();
    Iterator iter     = members.iterator();
    while (iter.hasNext()) {
      Member      m   = (Member) iter.next();
      Membership  imm = new Membership(o, m, this.f, c);
      imm.setSession(root);
      mships.add(imm);
    }
    return mships;
  } // private Set _createNewMembershipObjects(members)

  // Evaluate a composite membership
  private Set _evalComposite() 
    throws  ModelException 
  {
    Set results = new LinkedHashSet();
    if (this.c.getType().equals(CompositeType.UNION)) {
      results.addAll( this._evalCompositeUnion() );
    }
    else {
      throw new ModelException(ERR_CT + c.getType().toString());
    }
    return results;
  } // private Set _evalComposite()

  // Evaluate a union composite membership
  private Set _evalCompositeUnion() 
    throws  ModelException
  {
    Set   results = new LinkedHashSet();
    Set   tmp     = new LinkedHashSet();
    Group left    = this.c.getLeftGroup();
    Group right   = this.c.getRightGroup();
    tmp.addAll( left.getMembers() );
    tmp.addAll( right.getMembers()  );
    return this._createNewMembershipObjects(tmp);
  } // private Set _evalCompositeUnion()

  // Evaluate an immediate membership
  private Set _evalImmediate() 
    throws  GroupNotFoundException, // TODO
            MemberNotFoundException // TODO
  {
    Set results   = new LinkedHashSet();
    // If we are working on a group, where is it a member
    Set isMember  = new LinkedHashSet();
    if (this.o instanceof Group) {
      isMember = ( (Group) o).toMember().getAllMemberships();
    }
    // Members of m if o is a group
    Set hasMembers  = this._findMembersOfMember();
    // Add m to where o is a member if f == "members"
    results.addAll( this._addMemberToWhereGroupIsMember(isMember) );
    // Add members of m to o
    results.addAll( this._addHasMembersToOwner(hasMembers) ); 
    // Add members of m to where o is a member if f == "members"
    results.addAll( this._addHasMembersToWhereGroupIsMember(isMember, hasMembers) );
    return results;
  } // private Set _evalImmediate()

  // Find m's hasMembers
  private Set _findMembersOfMember() 
    throws  GroupNotFoundException
  {
    Set     hasMembers  = new LinkedHashSet();

    // If member is a group, convert to group and find its members.
    if (this.m.getSubjectTypeId().equals("group")) {
      // Convert member back to a group
      Group gAsM = this.m.toGroup();
      // And attach root session for better looking up of memberships
      gAsM.setSession(this.root);
      // Find members of m 
      hasMembers = gAsM.getMemberships();
    }
    return hasMembers;
  } // private Set _findMembersOfMember()

  // Reset attached sessions to their original state
  private void _resetSessions() {
    if (this.c != null) {
      this.c.setSession(this.s);
    }
    if (this.o != null) {
      this.o.setSession(this.s);
    }
    Set       tmp       = new LinkedHashSet();
    Iterator  saveIter  = this.effSaves.iterator();
    while (saveIter.hasNext()) {
      Membership ms = (Membership) saveIter.next();
      ms.setSession(this.s);
      tmp.add(ms);
    }
    this.saves = tmp;
    tmp = new LinkedHashSet();
    Iterator  delIter   = this.deletes.iterator();
    while (delIter.hasNext()) {
      Membership ms = (Membership) delIter.next();
      ms.setSession(this.s);
      tmp.add(ms);
    }
    this.deletes = tmp;
  } // private void _resetSessions()

  // Switch all sessions to root sessions
  private void _setSessions() {
    this.root = GrouperSessionFinder.getTransientRootSession();
    if (this.c != null) {
      this.c.setSession(this.root);
    }
    if (this.m != null) {
      this.m.setSession(this.root);
    }
    if (this.o != null) {
      this.o.setSession(this.root);
    }
  } // private void _setSessions()

}

