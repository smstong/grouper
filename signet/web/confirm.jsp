<!--
  $Id: confirm.jsp,v 1.4 2005-02-23 22:05:55 acohen Exp $
  $Date: 2005-02-23 22:05:55 $
  
  Copyright 2004 Internet2 and Stanford University.  All Rights Reserved.
  Licensed under the Signet License, Version 1,
  see doc/license.txt in this distribution.
-->

<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
  <head>
    <meta http-equiv="Content-Type" content="text/html; charset=iso-8859-1" />
    <meta name="robots" content="noindex, nofollow" />
    <title>
      Signet
    </title>
    <link href="styles/signet.css" rel="stylesheet" type="text/css" />
    <script language="JavaScript" type="text/javascript" src="scripts/signet.js">
    </script>
  </head>

  <body>

<%@ page import="java.text.DateFormat" %>
<%@ page import="java.util.Iterator" %>
<%@ page import="java.util.Map" %>
<%@ page import="java.util.Set" %>
<%@ page import="java.util.SortedSet" %>
<%@ page import="java.util.TreeSet" %>

<%@ page import="edu.internet2.middleware.signet.PrivilegedSubject" %>
<%@ page import="edu.internet2.middleware.signet.Subsystem" %>
<%@ page import="edu.internet2.middleware.signet.Category" %>
<%@ page import="edu.internet2.middleware.signet.Assignment" %>
<%@ page import="edu.internet2.middleware.signet.Function" %>
<%@ page import="edu.internet2.middleware.signet.tree.TreeNode" %>
<%@ page import="edu.internet2.middleware.signet.Signet" %>
<%@ page import="edu.internet2.middleware.signet.Limit" %>
<%@ page import="edu.internet2.middleware.signet.LimitValue" %>

<% 
  Signet signet
     = (Signet)
         (request.getSession().getAttribute("signet"));
   
  PrivilegedSubject loggedInPrivilegedSubject
     = (PrivilegedSubject)
         (request.getSession().getAttribute("loggedInPrivilegedSubject"));
   
   PrivilegedSubject currentGranteePrivilegedSubject
     = (PrivilegedSubject)
         (request.getSession().getAttribute("currentGranteePrivilegedSubject"));
         
   Subsystem currentSubsystem
     = (Subsystem)
         (request.getSession().getAttribute("currentSubsystem"));
         
   Category currentCategory
     = (Category)
         (request.getSession().getAttribute("currentCategory"));
         
   Function currentFunction
     = (Function)
         (request.getSession().getAttribute("currentFunction"));
         
   TreeNode currentScope
     = (TreeNode)
         (request.getSession().getAttribute("currentScope"));
         
   Assignment currentAssignment
     = (Assignment)
         (request.getSession().getAttribute("currentAssignment"));
         
   DateFormat dateFormat = DateFormat.getDateInstance();
   
   String personViewHref
     = "PersonView.do?granteeSubjectTypeId="
       + currentGranteePrivilegedSubject.getSubjectTypeId()
       + "&granteeSubjectId="
       + currentGranteePrivilegedSubject.getSubjectId();
       
   String functionsHref
     = "Functions.do?select="
       + currentSubsystem.getId();
%>

    <form name="form1" action="">
    
      <div id="Header">
        <div id="Logo">
          <img src="images/KITN.gif" width="216" height="60" alt="logo" />
        </div> <!-- Logo -->
        <div id="Signet">
          <a href="Start.do">
            <img src="images/signet.gif" width="49" height="60" alt="Signet" />
          </a>
        </div> <!-- Signet -->
      </div> <!-- Header -->
      
      <div id="Navbar">
        <span class="logout">
          <a href="NotYetImplemented.do">
            <%=loggedInPrivilegedSubject.getName()%>: Logout
          </a>
        </span> <!-- logout -->
        <span class="select">
          <a href="Start.do">
            Home
          </a>
          >  <!-- This single right-angle-bracket is just a text element, not an HTML token. -->
          <a href="<%=personViewHref%>"
            >  <!-- This single right-angle-bracket is just a text element, not an HTML token. -->
            <%=currentGranteePrivilegedSubject.getName()%>
          </a>
          &gt; Grant new privilege
        </span> <!-- select -->
      </div>  <!-- Navbar -->
      
      <div id="Layout">
        <div id="Content">
          <div id="ViewHead">
            Privilege granted to
            <h1>
              <%=currentGranteePrivilegedSubject.getName()%>
            </h1>
            <span class="dropback"><%=currentGranteePrivilegedSubject.getDescription()%></span><!--,  Technology Strategy and Support Operations-->
          </div>
           
          <div class="section">
            <h2><%=currentSubsystem.getName()%> privilege granted</h2>
            <ul class="none">
              <li>
                <%=currentCategory.getName()%>
                <ul class="arrow">
                  <li>
                    <%=currentFunction.getName()%>
                  </li>
                </ul>
              </li>
            </ul>
          </div><!-- section -->
              

          <!-- tableheader -->
          <div class="section">
            <h2>
              scope
            </h2>
            <ul class="none">
                
              <%=signet.displayAncestry
                    (currentScope,
                     "<ul class=\"arrow\">\n",  // childSeparatorPrefix
                     "\n<li>\n",                // levelPrefix
                     "\n</li>\n",               // levelSuffix
                     "\n</ul>")                 // childSeparatorSuffix
                 %>
                
            </ul>
          </div> <!-- section -->
              
       
          <div class="section">
            <h2>
              Limits
            </h2>
            <table class="invis">
<%
  Limit[] limits = currentAssignment.getFunction().getLimitsArray();
  LimitValue[] limitValues = currentAssignment.getLimitValuesArray();
  for (int limitIndex = 0; limitIndex < limits.length; limitIndex++)
  {
    Limit limit = limits[limitIndex];
%>
              <tr>
                <td align="right">
                  <%=limit.getName()%>:
                </td>
                <td>
<%
    int limitValuesPrinted = 0;
    for (int limitValueIndex = 0;
         limitValueIndex < limitValues.length;
         limitValueIndex++)
    {
      LimitValue limitValue = limitValues[limitValueIndex];
      if (limitValue.getLimit().equals(limit))
      {
%>
                  <%=(limitValuesPrinted++ > 0) ? "<br />" : ""%>
                  <%=limitValue.getValue()%>
<%
      }
    }
%>
               
                </td>
              </tr>
<%
  }
%>
            </table>
          </div> <!-- section -->
              
       
          <div class="section">
            <h2>
              Conditions
            </h2>
            <table class="invis">
              <tr>
                <td align="right">
                  Privilege holder can:
                </td>
                <td>
                  <%=(currentAssignment.isGrantOnly() ? "" : "use this privilege")%>
                  <br />
                  <%=(currentAssignment.isGrantable() ? "grant this privilege to others" : "")%>
                </td>
              </tr>
            </table>
          </div> <!-- section -->
                    

           <div class="section">
          <h2>
             Continue
           </h2>
             <p>
               <a href="<%=personViewHref%>">
                 <img src="images/icon_arrow_right.gif" width="16" height="16" class="icon" />View all <%=currentGranteePrivilegedSubject.getName()%>'s privileges
               </a>
             </p>
             <p>
               <a href="Functions.do?select=<%=currentSubsystem.getId()%>">
                 <img src="images/icon_arrow_right.gif" width="16" height="16" class="icon" />Grant another privilege to <%=currentGranteePrivilegedSubject.getName()%>
               </a>
             </p>
             <p>
               <a href="Start.do">
                 <img src="images/icon_arrow_right.gif" width="16" height="16" class="icon" />Return to home page
               </a>
             </p>
           </div>
           <!-- section -->
                    
          <jsp:include page="footer.jsp" flush="true" /> 
         </div><div id="Sidebar">
          <div class="findperson">
            <h2>
              Find a person
            </h2>
            
            <div class="actionbox">
              <p>
                <input name="words" type="text" class="short" id="words" style="width:100px" size="15" maxlength="500" />
                <input name="searchbutton" type="button" class="button1" onclick="javascript:showResult();" value="Search" />
              <br />
                <span class="dropback">
                  Enter a person's name, and click "Search."
                </span>
              </p>
              <div id="Results" style="display:none">
                Your search found:
              <ol>
          
<%
  Set privilegedSubjects
    = signet.getPrivilegedSubjects();
      
  SortedSet sortSet = new TreeSet(privilegedSubjects);
  Iterator sortSetIterator = sortSet.iterator();
  while (sortSetIterator.hasNext())
  {
    PrivilegedSubject listSubject
      = (PrivilegedSubject)(sortSetIterator.next());
%>
            <li>
              <a href="PersonView.do?granteeSubjectTypeId=<%=listSubject.getSubjectTypeId()%>&granteeSubjectId=<%=listSubject.getSubjectId()%>">
                <%=listSubject.getName()%>
              </a>
              <br />
              <!--Stanford Linear Accelerator Center, --><%=listSubject.getDescription()%>
            </li>
<%
  }
%>
              </ol>
            </div> <!-- results -->
           </div> <!-- actionbox -->
          </div> <!-- findperson -->

          <div class="views">
            <h2>
              View privileges...
            </h2>
            <div class="actionbox">
              <p>
                <a href="Start.do">
              <img src="images/icon_arrow_right.gif" width="16" height="16" class="icon" />you have granted</a></p>
              <p>
                <a href="PersonView.do?granteeSubjectTypeId=<%=loggedInPrivilegedSubject.getSubjectTypeId()%>&granteeSubjectId=<%=loggedInPrivilegedSubject.getSubjectId()%>">
                  <img src="images/icon_arrow_right.gif" width="16" height="16" class="icon" />assigned to you</a>
              </p>
              <p>
                <a href="NotYetImplemented.do">
                  <img src="images/icon_arrow_right.gif" width="16" height="16" class="icon" />by scope
                </a>
              </p>
            </div>
          </div> <!-- views -->
                  
          <div class="helpbox">
            <h2>Help</h2>
            <!-- actionheader -->
            <jsp:include page="confirm-help.jsp" flush="true" />          
          </div>  <!-- end helpbox -->
         </div> <!-- Sidebar -->
        
      </div> <!-- Layout -->
    </form>
  </body>
</html>
