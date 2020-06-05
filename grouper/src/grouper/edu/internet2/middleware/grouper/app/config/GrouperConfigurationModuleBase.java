package edu.internet2.middleware.grouper.app.config;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;

import edu.internet2.middleware.grouper.cfg.dbConfig.ConfigFileName;
import edu.internet2.middleware.grouper.cfg.dbConfig.ConfigItemMetadataType;
import edu.internet2.middleware.grouper.cfg.dbConfig.DbConfigEngine;
import edu.internet2.middleware.grouper.cfg.dbConfig.GrouperConfigHibernate;
import edu.internet2.middleware.grouper.cfg.text.GrouperTextContainer;
import edu.internet2.middleware.grouper.misc.GrouperDAOFactory;
import edu.internet2.middleware.grouper.util.GrouperUtil;
import edu.internet2.middleware.grouperClient.config.ConfigPropertiesCascadeBase;

public abstract class GrouperConfigurationModuleBase {
  
  /** logger */
  private static final Log LOG = GrouperUtil.getLog(GrouperConfigurationModuleBase.class);
  
  /**
   * config id of the daemon
   */
  private String configId;
  
  /**
   * call retrieveAttributes() to get this
   */
  protected Map<String, GrouperConfigurationModuleAttribute> attributeCache = null;
  
  public String getConfigId() {
    return configId;
  }
  
  public void setConfigId(String configId) {
    this.configId = configId;
  }

  public void validatePreSave(boolean isInsert, boolean fromUi, List<String> errorsToDisplay, Map<String, String> validationErrorsToDisplay) {
    
    if (isInsert) {
      if (this.retrieveConfigurationConfigIds().contains(this.getConfigId())) {
        validationErrorsToDisplay.put("#configId", GrouperTextContainer.textOrNull("grouperConfigurationValidationConfigIdUsed"));
      }
    }
    
    // first check if checked the el checkbox then make sure theres a script there
    {
      boolean foundElRequiredError = false;
      for (GrouperConfigurationModuleAttribute grouperConfigModuleAttribute : this.retrieveAttributes().values()) {
        
        if (grouperConfigModuleAttribute.isExpressionLanguage() && StringUtils.isBlank(grouperConfigModuleAttribute.getExpressionLanguageScript())) {
          
          GrouperTextContainer.assignThreadLocalVariable("configFieldLabel", grouperConfigModuleAttribute.getLabel());
          validationErrorsToDisplay.put(grouperConfigModuleAttribute.getHtmlForElementIdHandle(), 
              GrouperTextContainer.textOrNull("grouperConfigurationValidationElRequired"));
          GrouperTextContainer.resetThreadLocalVariableMap();
          foundElRequiredError = true;
        }
        
      }
      if (foundElRequiredError) {
        return;
      }
    }
    
    // types
    for (GrouperConfigurationModuleAttribute grouperConfigModuleAttribute : this.retrieveAttributes().values()) {
      
      GrouperTextContainer.assignThreadLocalVariable("configFieldLabel", grouperConfigModuleAttribute.getLabel());
      try {
        
      ConfigItemMetadataType configItemMetadataType = grouperConfigModuleAttribute.getConfigItemMetadata().getValueType();
      
      String value = null;
      
      try {
        value = grouperConfigModuleAttribute.getEvaluatedValueForValidation();
      } catch (UnsupportedOperationException uoe) {
        // ignore, it will get validated in the post-save
        continue;
      }
      
      // required
      if (StringUtils.isBlank(value)) {
        if (grouperConfigModuleAttribute.getConfigItemMetadata().isRequired()) {

          
          validationErrorsToDisplay.put(grouperConfigModuleAttribute.getHtmlForElementIdHandle(), 
              GrouperTextContainer.textOrNull("grouperConfigurationValidationRequired"));
          
        }
        
        continue;
      }
      String[] valuesToValidate = null;
     
      if (grouperConfigModuleAttribute.getConfigItemMetadata().isMultiple()) {
        valuesToValidate = GrouperUtil.splitTrim(value, ",");
      } else {
        valuesToValidate = new String[] {value};
      }

      for (String theValue : valuesToValidate) {
        
        // validate types
        String externalizedTextKey = configItemMetadataType.validate(theValue);
        if (StringUtils.isNotBlank(externalizedTextKey)) {
          
          validationErrorsToDisplay.put(grouperConfigModuleAttribute.getHtmlForElementIdHandle(), 
              GrouperTextContainer.textOrNull(externalizedTextKey));
          
        } else {
          String mustExtendClass = grouperConfigModuleAttribute.getConfigItemMetadata().getMustExtendClass();
          if (StringUtils.isNotBlank(mustExtendClass)) {
            
            Class mustExtendKlass = GrouperUtil.forName(mustExtendClass);
            Class childClass = GrouperUtil.forName(theValue);
            
            if (!mustExtendKlass.isAssignableFrom(childClass)) {
              
              String error = GrouperTextContainer.textOrNull("grouperConfigurationValidationDoesNotExtendClass");
              error = error.replace("$$mustExtendClass$$", mustExtendClass);
              
              validationErrorsToDisplay.put(grouperConfigModuleAttribute.getHtmlForElementIdHandle(), error);
            }
          }
          
          String mustImplementInterface = grouperConfigModuleAttribute.getConfigItemMetadata().getMustImplementInterface();
          if (StringUtils.isNotBlank(mustImplementInterface)) {
            
            Class mustImplementInterfaceClass = GrouperUtil.forName(mustImplementInterface);
            Class childClass = GrouperUtil.forName(theValue);
            
            if (!mustImplementInterfaceClass.isAssignableFrom(childClass)) {
              
              String error = GrouperTextContainer.textOrNull("grouperConfigurationValidationDoesNotImplementInterface");
              error = error.replace("$$mustImplementInterface$$", mustImplementInterface);
              
              validationErrorsToDisplay.put(grouperConfigModuleAttribute.getHtmlForElementIdHandle(), error);
            }
          }
          
          
        }
      }
    } finally {
      GrouperTextContainer.resetThreadLocalVariableMap();
    }
    }
  }
  
  public abstract Map<String, GrouperConfigurationModuleAttribute> retrieveAttributes();
  
  public abstract ConfigFileName getConfigFileName();
  
  public abstract String getConfigItemPrefix();
  
  public abstract String getConfigIdRegex();
  
  /**
   * 
   * @param suffix
   * @return
   */
  public Boolean showAttributeOverride(String suffix) {
    return null;
  }
  
  /**
   * save the attribute in an insert.  Note, if theres a failure, you should see if any made it
   * @param attributesToSave are the attributes from "retrieveAttributes" with values in there
   * if a value is blank, then dont save that one
   * @param errorsToDisplay call from ui: guiResponseJs.addAction(GuiScreenAction.newMessage(GuiMessageType.error, message.toString()));
   * @param validationErrorsToDisplay call from ui: guiResponseJs.addAction(GuiScreenAction.newValidationMessage(GuiMessageType.error, validationKey, 
   *      validationErrorsToDisplay.get(validationKey)));
   */
  public void insertConfig(boolean fromUi, 
      StringBuilder message, List<String> errorsToDisplay, Map<String, String> validationErrorsToDisplay) {
    
    validatePreSave(true, fromUi, errorsToDisplay, validationErrorsToDisplay);

    if (errorsToDisplay.size() > 0 || validationErrorsToDisplay.size() > 0) {
      return;
    }
    
    Pattern endOfStringNewlinePattern = Pattern.compile(".*<br[ ]*\\/?>$");
    
    // add all the possible ones
    Map<String, GrouperConfigurationModuleAttribute> attributes = this.retrieveAttributes();
    for (String suffix : attributes.keySet()) {
    
      GrouperConfigurationModuleAttribute grouperConfigModuleAttribute = attributes.get(suffix);
      
      if (grouperConfigModuleAttribute.isHasValue()) {
        
        StringBuilder localMessage = new StringBuilder();
        
        DbConfigEngine.configurationFileAddEditHelper2(this.getConfigFileName().getConfigFileName(), 
            grouperConfigModuleAttribute.getFullPropertyName(), 
            grouperConfigModuleAttribute.isExpressionLanguage() ? "true" : "false", 
            grouperConfigModuleAttribute.isExpressionLanguage() ? grouperConfigModuleAttribute.getExpressionLanguageScript() : grouperConfigModuleAttribute.getValue(),
            grouperConfigModuleAttribute.isPassword(), localMessage, new Boolean[] {false},
            new Boolean[] {false}, fromUi, "Added from config editor", errorsToDisplay, validationErrorsToDisplay, false);
        
        if (localMessage.length() > 0) {
          if(message.length() > 0) {
            
            if (fromUi && !endOfStringNewlinePattern.matcher(message).matches()) {
              message.append("<br />\n");
            } else if (!fromUi && message.charAt(message.length()-1) != '\n') {
              message.append("\n");
            }
            message.append(localMessage);
          }
        }
      }
    }

    ConfigPropertiesCascadeBase.clearCache();
    this.attributeCache = null;
  }
  
  /**
   * delete config
   * @param fromUi
   */
  public void deleteConfig(boolean fromUi) {
    
    Map<String, GrouperConfigurationModuleAttribute> attributes = this.retrieveAttributes();
    
    for (GrouperConfigurationModuleAttribute attribute: attributes.values()) {
      
      //DbConfigEngine.configurationFileItemDeleteHelper(this.getConfigFileName().getConfigFileName(), attribute.getFullPropertyName() , fromUi, false);
      
      Set<GrouperConfigHibernate> grouperConfigHibernates = GrouperDAOFactory.getFactory().getConfig()
          .findAll(this.getConfigFileName(), null, attribute.getFullPropertyName());
      
      if (grouperConfigHibernates != null && grouperConfigHibernates.size() > 0) {
        for (GrouperConfigHibernate grouperConfigHibernate: grouperConfigHibernates) {
          DbConfigEngine.configurationFileItemDeleteHelper(grouperConfigHibernate, this.getConfigFileName(), fromUi);
        }
      }
    }
    
    ConfigPropertiesCascadeBase.clearCache();
    this.attributeCache = null;
  }
  
  /**
   * save the attribute in an edit.  Note, if theres a failure, you should see if any made it
   * @param attributesFromUser are the attributes from "retrieveAttributes" with values in there
   * if a value is blank, then dont save that one (delete)
   * @param errorsToDisplay call from ui: guiResponseJs.addAction(GuiScreenAction.newMessage(GuiMessageType.error, message.toString()));
   * @param validationErrorsToDisplay call from ui: guiResponseJs.addAction(GuiScreenAction.newValidationMessage(GuiMessageType.error, validationKey, 
   *      validationErrorsToDisplay.get(validationKey)));
   */
  public void editConfig(boolean fromUi, StringBuilder message, List<String> errorsToDisplay, Map<String, String> validationErrorsToDisplay) {
    
    validatePreSave(false, fromUi, errorsToDisplay, validationErrorsToDisplay);

    if (errorsToDisplay.size() > 0 || validationErrorsToDisplay.size() > 0) {
      return;
    }
    
    Map<String, GrouperConfigurationModuleAttribute> attributes = this.retrieveAttributes();
    
    Set<String> propertyNamesToDelete = new HashSet<String>();

    // add all the possible ones
    for (String suffix : attributes.keySet()) {
    
      GrouperConfigurationModuleAttribute grouperConfigModuleAttribute = attributes.get(suffix);

      propertyNamesToDelete.add(grouperConfigModuleAttribute.getFullPropertyName());
      
    }

    // and all the ones we detect
    if (!StringUtils.isBlank(this.getConfigId())) {
      
      Set<String> configKeys = this.retrieveConfigurationKeysByPrefix(this.getConfigItemPrefix());
      
      if (GrouperUtil.length(configKeys) > 0) {
        propertyNamesToDelete.addAll(configKeys);
      }
    }
    
    Map<String, GrouperConfigurationModuleAttribute> attributesToSave = new HashMap<String, GrouperConfigurationModuleAttribute>();
    
    // remove the edited ones
    for (String suffix : attributes.keySet()) {
    
      GrouperConfigurationModuleAttribute grouperConfigModuleAttribute = attributes.get(suffix);
      
      if (grouperConfigModuleAttribute.isHasValue()) {
        propertyNamesToDelete.remove(grouperConfigModuleAttribute.getFullPropertyName());
        attributesToSave.put(suffix, grouperConfigModuleAttribute);
      }
    }
    // delete some
    for (String key : propertyNamesToDelete) {
      DbConfigEngine.configurationFileItemDeleteHelper(this.getConfigFileName().getConfigFileName(), key , fromUi, false);
    }

    Pattern endOfStringNewlinePattern = Pattern.compile(".*<br[ ]*\\/?>$");
    
    // add/edit all the possible ones
    for (String suffix : attributesToSave.keySet()) {
    
      GrouperConfigurationModuleAttribute grouperConfigModuleAttribute = attributesToSave.get(suffix);
      
      StringBuilder localMessage = new StringBuilder();
      
      DbConfigEngine.configurationFileAddEditHelper2(this.getConfigFileName().getConfigFileName(), 
          grouperConfigModuleAttribute.getFullPropertyName(), 
          grouperConfigModuleAttribute.isExpressionLanguage() ? "true" : "false", 
          grouperConfigModuleAttribute.isExpressionLanguage() ? grouperConfigModuleAttribute.getExpressionLanguageScript() : grouperConfigModuleAttribute.getValue(),
          grouperConfigModuleAttribute.isPassword(), localMessage, new Boolean[] {false},
          new Boolean[] {false}, fromUi, "Added from config editor", errorsToDisplay, validationErrorsToDisplay, false);
      
      if (localMessage.length() > 0) {
        if(message.length() > 0) {
          
          if (fromUi && !endOfStringNewlinePattern.matcher(message).matches()) {
            message.append("<br />\n");
          } else if (!fromUi && message.charAt(message.length()-1) != '\n') {
            message.append("\n");
          }
          message.append(localMessage);
        }
      }
    }

    ConfigPropertiesCascadeBase.clearCache();
    this.attributeCache = null;
  }
  
  /**
   * get configuration names configured by prefix 
   * @param prefix of config e.g. ldap.personLdap.
   * @return the list of configured keys
   */
  public Set<String> retrieveConfigurationKeysByPrefix(String prefix) {
    Set<String> result = new HashSet<String>();
    ConfigFileName configFileName = this.getConfigFileName();
    
    ConfigPropertiesCascadeBase configPropertiesCascadeBase = configFileName.getConfig();
    
    Properties properties = configPropertiesCascadeBase.properties();

    for (Object propertyNameObject : properties.keySet()) {
      String propertyName = (String)propertyNameObject;
      if (propertyName.startsWith(prefix)) {

        if (result.contains(propertyName)) {
          LOG.error("Config key '" + propertyName + "' is defined in '" + configFileName.getConfigFileName() + "' more than once!");
        } else {
          result.add(propertyName);
        }
      }
    }
    return result;
  }
  
  /**
   * get title of the grouper daemon configuration
   * @return
   */
  public String getTitle() {
    String title = GrouperTextContainer.textOrNull("config." + this.getClass().getSimpleName() + ".title");
    if (StringUtils.isBlank(title)) {
      return this.getClass().getSimpleName();
    }
    return title;
  }
  
  /**
   * get description of the external system
   * @return
   */
  public String getDescription() {
    String title = GrouperTextContainer.textOrNull("config." + this.getClass().getSimpleName() + ".description");
    if (StringUtils.isBlank(title)) {
      return this.getClass().getSimpleName();
    }
    return title;
  }
  
  /**
   * get a set of config ids
   * @return
   */
  public Set<String> retrieveConfigurationConfigIds() {
    
    String regex = this.getConfigIdRegex();
    
    if (StringUtils.isBlank(regex)) {
      throw new RuntimeException("Regex is reqired for " + this.getClass().getName());
    }
    
    Set<String> result = new TreeSet<String>();
    
    ConfigFileName configFileName = this.getConfigFileName();
    
    ConfigPropertiesCascadeBase configPropertiesCascadeBase = configFileName.getConfig();
    
    Properties properties = configPropertiesCascadeBase.properties();

    Pattern pattern = Pattern.compile(regex);
    
    for (Object propertyNameObject : properties.keySet()) {
      String propertyName = (String)propertyNameObject;
      
      Matcher matcher = pattern.matcher(propertyName);
      
      if (!matcher.matches()) {
        continue;
      }

      String configId = matcher.group(2);
      result.add(configId);
    }
    return result;
  }
  
}
