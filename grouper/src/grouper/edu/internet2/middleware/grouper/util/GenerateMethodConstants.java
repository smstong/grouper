/*
 * @author mchyzer
 * $Id: GenerateMethodConstants.java,v 1.2 2008-07-10 00:46:54 mchyzer Exp $
 */
package edu.internet2.middleware.grouper.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import edu.internet2.middleware.grouper.hooks.GroupHooks;
import edu.internet2.middleware.grouper.hooks.LifecycleHooks;
import edu.internet2.middleware.grouper.hooks.MembershipHooks;


/**
 *
 */
public class GenerateMethodConstants {
  /**
   * 
   * @param args
   */
  public static void main(String[] args) {
    generateConstants(LifecycleHooks.class);
  }
  /**
   * <pre>
   * generate the Java for the constant fields
   * to ignore a field, annotate with GrouperIngoreFieldConstant
   * </pre>
   * @param theClass 
   *
   */
  private static void generateConstants(Class theClass) {
    Set<String> methodNamesSet = GrouperUtil.methodNames(theClass,theClass, true, false);

    //sort
    List<String> methodNames = new ArrayList<String>(methodNamesSet);
    Collections.sort(methodNames);
    StringBuilder result = new StringBuilder();
    System.out.println("  //*****  START GENERATED WITH GenerateMethodConstants.java *****//\n");
    for (String methodName: methodNames) {
      String caps = "METHOD_" + GrouperUtil.oracleStandardNameFromJava(methodName);
      
      result.append("  /** constant for method name for: " + methodName + " */\n");
      result.append("  public static final String " + caps + " = \"" + methodName + "\";\n\n");
    }
    System.out.print(result.toString());
    System.out.println("  //*****  END GENERATED WITH GenerateMethodConstants.java *****//");
  }

}
