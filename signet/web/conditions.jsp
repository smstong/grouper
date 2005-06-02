<!--
  $Id: conditions.jsp,v 1.15 2005-06-02 06:26:04 jvine Exp $
  $Date: 2005-06-02 06:26:04 $
  
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

  <body onload="javascript:selectLimitCheckbox();">
    <script type="text/javascript">
      function selectLimitCheckbox()
      {
        if (hasUnselectedLimits())
        {
          document.form1.completeAssignmentButton.disabled = true;
        }
        else
        {
          document.form1.completeAssignmentButton.disabled = false;
        }
      }
      
      function hasUnselectedLimits()
      {
        var theForm = document.form1;
        var currentLimitName = null;
        var currentLimitSelected = true;
        
        for (var i = 0; i < theForm.elements.length; i++)
        {
          var currentElement = theForm.elements[i];
             
          if (currentElement.name == null)
          {
            continue;
          }
             
          var nameParts = currentElement.name.split(':');
          
          if (nameParts[0] == 'LIMITVALUE_MULTI')
          {
            if ((currentLimitName != null)
                && (currentLimitName != currentElement.name)
                && (currentLimitSelected == false))
            {
              // We've finished examining a Limit, and it had no
              // selected values.
              return true; // We've found an un-selected Limit.
            }
            else if (currentLimitName == currentElement.name)
            {
              // We're looking at a series of values for a single Limit.
              if (currentElement.checked == true)
              {
                currentLimitSelected = true;
              }
            }
            else
            {
              // We're moving on to a previously unexamined Limit. The previous
              // Limits, if any, all had at least one selected value.
              currentLimitName = currentElement.name;
              currentLimitSelected = currentElement.checked;
            }
          }
        }
        
        return !currentLimitSelected;
      }
  </script>
  
<%@ taglib uri="http://struts.apache.org/tags-tiles" prefix="tiles" %>

<%@ page import="java.text.DateFormat" %>
<%@ page import="java.util.Iterator" %>
<%@ page import="java.util.Map" %>
<%@ page import="java.util.Set" %>

<%@ page import="edu.internet2.middleware.signet.PrivilegedSubject" %>
<%@ page import="edu.internet2.middleware.signet.Subsystem" %>
<%@ page import="edu.internet2.middleware.signet.Category" %>
<%@ page import="edu.internet2.middleware.signet.Assignment" %>
<%@ page import="edu.internet2.middleware.signet.Function" %>
<%@ page import="edu.internet2.middleware.signet.tree.TreeNode" %>
<%@ page import="edu.internet2.middleware.signet.Signet" %>
<%@ page import="edu.internet2.middleware.signet.Limit" %>
<%@ page import="edu.internet2.middleware.signet.ui.LimitRenderer" %>

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
         
   Limit[] currentLimits = currentFunction.getLimitsArray();
         
   DateFormat dateFormat = DateFormat.getDateInstance();
   
   String personViewHref
     = "PersonView.do?granteeSubjectTypeId="
       + currentGranteePrivilegedSubject.getSubjectTypeId()
       + "&granteeSubjectId="
       + currentGranteePrivilegedSubject.getSubjectId()
       + "&subsystemId="
       + currentSubsystem.getId();

       
   String functionsHref
     = "Functions.do?select="
       + currentSubsystem.getId();
       
   String orgBrowseHref
   	= "OrgBrowse.do?functionSelectList="
   		+ currentFunction.getId();
%>

    <form name="form1" action="Confirm.do">
      <tiles:insert page="/tiles/header.jsp" flush="true" />
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
          &gt; <!-- displays as text right-angle bracket -->
          <a href="<%=personViewHref%>"> 
            <%=currentGranteePrivilegedSubject.getName()%>
          </a>
          &gt; Grant new privilege
        </span> <!-- select -->
      </div>  <!-- Navbar -->
      
      <div id="Layout">
        <div id="Content">
          <div id="ViewHead">
            Granting new privilege to
            <h1>
              <%=currentGranteePrivilegedSubject.getName()%>
       	    </h1>
       	    <span class="dropback"><%=currentGranteePrivilegedSubject.getDescription()%></span><!--,	Technology Strategy and Support Operations-->
            </div>  <!-- ViewHead -->
          
            <div class="section">
             	<h2>New <%=currentSubsystem.getName()%> privilege
						 		<div class="change">
									<a href="<%=functionsHref%>"><img src="images/arrow_left.gif" />change</a>
								</div>
							</h2>
                <span class="category"><%=currentCategory.getName()%></span> : 
                <span class="function"><%=currentFunction.getName()%></span>
          </div> <!-- section -->
            
          <div class="section">
              <h2>Scope
								<div class="change">
									<a href="<%=orgBrowseHref%>"><img src="images/arrow_left.gif" />change</a>
								</div>
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
          </div><!-- section -->
            
<%
  if (currentLimits.length > 0)
  {
%>
            <div class="section">
              <h2> Limits</h2>
           
<%
    for (int i = 0; i < currentLimits.length; i++)
    {
      request.setAttribute("limitAttr", currentLimits[i]);
      request.setAttribute
        ("grantableChoiceSubsetAttr",
         loggedInPrivilegedSubject.getGrantableChoices
           (currentFunction, currentScope, currentLimits[i]));
%>
              
              <fieldset>
                <legend>
                  <%=currentLimits[i].getName()%>
                </legend>
                <p>
                  <%=currentLimits[i].getHelpText()%>
                </p>
                <blockquote>
                  <tiles:insert
                     page='<%="/tiles/" + currentLimits[i].getRenderer()%>'
                     flush="true">
                    <tiles:put name="limit" beanName="limitAttr" />
                    <tiles:put name="grantableChoiceSubset" beanName="grantableChoiceSubsetAttr" />
                  </tiles:insert>
                </blockquote>
              </fieldset>
<%
    }
%>
            </div> <!-- section -->
<%
  }
%>
		 
            <div class="section">
              <h2> Conditions</h2>

              <fieldset>
                <legend>
                  Extensibility
                </legend>
                <p>
                  Privilege holder can:
                </p>
                  <input
                     name="can_use"
                     id="can_use"
                     type="checkbox"
                     value="checkbox"
                     checked="checked" />
                  <label for="can_use">use this privilege</label>
                  <br />
                  <input name="can_grant" id="can_grant" type="checkbox" value="checkbox" />
                  <label for="can_grant">grant this privilege to others</label>
              </fieldset>
              <p>
                <input
                   name="completeAssignmentButton"
                   type="submit"
                   class="button-def"
                   value="Complete assignment" />
              </p>
              <p>
                <a href="<%=personViewHref%>">
                  <img src="images/arrow_left.gif" alt="" />CANCEL and return to <%=currentGranteePrivilegedSubject.getName()%>'s view
                </a>
              </p>
            </div> <!-- section -->
        </div> <!--Content -->
       <tiles:insert page="/tiles/footer.jsp" flush="true" />
				      
        <div id="Sidebar">
          <div class="helpbox">
			 	  	<h2>Help</h2>
			  		<jsp:include page="grant-help.jsp" flush="true" />          
					</div> <!-- Helpbox -->
        </div> <!-- Sidebar -->
      </div> <!-- Layout -->
    </form>
  </body>
</html>
