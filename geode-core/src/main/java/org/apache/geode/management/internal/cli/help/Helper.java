/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional information regarding
 * copyright ownership. The ASF licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License. You may obtain a
 * copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package org.apache.geode.management.internal.cli.help;

import org.apache.commons.lang.StringUtils;
import org.apache.geode.management.cli.CliMetaData;
import org.apache.geode.management.internal.cli.GfshParser;
import org.apache.geode.management.internal.cli.i18n.CliStrings;
import org.springframework.shell.core.MethodTarget;
import org.springframework.shell.core.annotation.CliAvailabilityIndicator;
import org.springframework.shell.core.annotation.CliCommand;
import org.springframework.shell.core.annotation.CliOption;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * 
 * 
 * @since GemFire 7.0
 */
public class Helper {

  static final String NAME_NAME = "NAME";
  static final String SYNONYMS_NAME = "SYNONYMS";
  static final String SYNOPSIS_NAME = "SYNOPSIS";
  static final String SYNTAX_NAME = "SYNTAX";
  static final String OPTIONS_NAME = "PARAMETERS";
  static final String IS_AVAILABLE_NAME = "IS AVAILABLE";

  static final String REQUIRED_SUB_NAME = "Required: ";
  static final String SYNONYMS_SUB_NAME = "Synonyms: ";
  static final String SPECIFIEDDEFAULTVALUE_SUB_NAME =
      "Default (if the parameter is specified without value): ";
  static final String UNSPECIFIEDDEFAULTVALUE_VALUE_SUB_NAME =
      "Default (if the parameter is not specified): ";

  static final String VALUE_FIELD = "value";
  static final String TRUE_TOKEN = "true";
  static final String FALSE_TOKEN = "false";

  private final Map<String, Topic> topics = new HashMap<>();
  private final Map<String, Method> commands = new TreeMap<String, Method>();
  private final Map<String, MethodTarget> availabilityIndicators =
      new HashMap<String, MethodTarget>();

  public Helper() {
    initTopic(CliStrings.DEFAULT_TOPIC_GEODE, CliStrings.DEFAULT_TOPIC_GEODE__DESC);
    initTopic(CliStrings.TOPIC_GEODE_REGION, CliStrings.TOPIC_GEODE_REGION__DESC);
    initTopic(CliStrings.TOPIC_GEODE_WAN, CliStrings.TOPIC_GEODE_WAN__DESC);
    initTopic(CliStrings.TOPIC_GEODE_JMX, CliStrings.TOPIC_GEODE_JMX__DESC);
    initTopic(CliStrings.TOPIC_GEODE_DISKSTORE, CliStrings.TOPIC_GEODE_DISKSTORE__DESC);
    initTopic(CliStrings.TOPIC_GEODE_LOCATOR, CliStrings.TOPIC_GEODE_LOCATOR__DESC);
    initTopic(CliStrings.TOPIC_GEODE_SERVER, CliStrings.TOPIC_GEODE_SERVER__DESC);
    initTopic(CliStrings.TOPIC_GEODE_MANAGER, CliStrings.TOPIC_GEODE_MANAGER__DESC);
    initTopic(CliStrings.TOPIC_GEODE_STATISTICS, CliStrings.TOPIC_GEODE_STATISTICS__DESC);
    initTopic(CliStrings.TOPIC_GEODE_LIFECYCLE, CliStrings.TOPIC_GEODE_LIFECYCLE__DESC);
    initTopic(CliStrings.TOPIC_GEODE_M_AND_M, CliStrings.TOPIC_GEODE_M_AND_M__DESC);
    initTopic(CliStrings.TOPIC_GEODE_DATA, CliStrings.TOPIC_GEODE_DATA__DESC);
    initTopic(CliStrings.TOPIC_GEODE_CONFIG, CliStrings.TOPIC_GEODE_CONFIG__DESC);
    initTopic(CliStrings.TOPIC_GEODE_FUNCTION, CliStrings.TOPIC_GEODE_FUNCTION__DESC);
    initTopic(CliStrings.TOPIC_GEODE_HELP, CliStrings.TOPIC_GEODE_HELP__DESC);
    initTopic(CliStrings.TOPIC_GEODE_DEBUG_UTIL, CliStrings.TOPIC_GEODE_DEBUG_UTIL__DESC);
    initTopic(CliStrings.TOPIC_GFSH, CliStrings.TOPIC_GFSH__DESC);
    initTopic(CliStrings.TOPIC_LOGS, CliStrings.TOPIC_LOGS__DESC);
    initTopic(CliStrings.TOPIC_CLIENT, CliStrings.TOPIC_CLIENT__DESC);
  }

  private void initTopic(String topic, String desc) {
    topics.put(topic, new Topic(topic, desc));
  }

  public void addCommand(CliCommand command, Method commandMethod) {
    // put all the command synonyms in the command map
    Arrays.stream(command.value()).forEach(cmd -> {
      commands.put(cmd, commandMethod);
    });

    // resolve the hint message for each method
    CliMetaData cliMetaData = commandMethod.getDeclaredAnnotation(CliMetaData.class);
    if (cliMetaData == null)
      return;
    String[] related = cliMetaData.relatedTopic();

    // for hint message, we only need to show the first synonym
    String commandString = command.value()[0];
    if (related == null) {
      return;
    }
    Arrays.stream(related).forEach(topic -> {
      Topic foundTopic = topics.get(topic);
      if (foundTopic == null) {
        throw new IllegalArgumentException("No such topic found in the initial map: " + topic);
      }
      foundTopic.addRelatedCommand(commandString, command.help());
    });
  }

  public void addAvailabilityIndicator(CliAvailabilityIndicator availability, MethodTarget target) {
    Arrays.stream(availability.value()).forEach(command -> {
      availabilityIndicators.put(command, target);
    });
  }

  public String getHelp(String buffer, int terminalWidth) {
    Method method = commands.get(buffer);
    if (method == null) {
      return "no help exists for this command.";
    }
    return getHelp(method).toString(terminalWidth);
  }

  public String getHint(String buffer) {
    StringBuilder builder = new StringBuilder();
    // if no topic is provided, return a list of topics
    if (StringUtils.isBlank(buffer)) {
      builder.append(CliStrings.HINT__MSG__TOPICS_AVAILABLE).append(GfshParser.LINE_SEPARATOR)
          .append(GfshParser.LINE_SEPARATOR);

      List<String> sortedTopics = new ArrayList<>(topics.keySet());
      Collections.sort(sortedTopics);
      sortedTopics.stream()
          .forEachOrdered(topic -> builder.append(topic).append(GfshParser.LINE_SEPARATOR));
      return builder.toString();
    }

    Topic topic = topics.get(buffer);
    if (topic == null) {
      return CliStrings.format(CliStrings.HINT__MSG__UNKNOWN_TOPIC, buffer);
    }

    builder.append(topic.desc).append(GfshParser.LINE_SEPARATOR).append(GfshParser.LINE_SEPARATOR);
    Collections.sort(topic.relatedCommands);
    topic.relatedCommands.stream().forEachOrdered(command -> builder.append(command.command)
        .append(": ").append(command.desc).append(GfshParser.LINE_SEPARATOR));
    return builder.toString();
  }

  private HelpBlock getHelp(Method method) {
    return getHelp(method.getDeclaredAnnotation(CliCommand.class), method.getParameterAnnotations(),
        method.getParameterTypes());
  }

  HelpBlock getHelp(CliCommand cliCommand, Annotation[][] annotations, Class<?>[] parameterTypes) {
    String commandName = cliCommand.value()[0];
    HelpBlock root = new HelpBlock();
    // First we will have the block for NAME of the command
    HelpBlock name = new HelpBlock(NAME_NAME);
    name.addChild(new HelpBlock(commandName));
    root.addChild(name);

    // add the availability flag
    HelpBlock availability = new HelpBlock(IS_AVAILABLE_NAME);
    boolean available = true;
    MethodTarget target = availabilityIndicators.get(commandName);
    if (target != null) {
      try {
        available = (Boolean) target.getMethod().invoke(target.getTarget());
      } catch (Exception e) {
      }
    }
    availability.addChild(new HelpBlock(available + ""));
    root.addChild(availability);

    // Now add synonyms if any
    String[] allNames = cliCommand.value();
    if (allNames.length > 1) {
      HelpBlock synonyms = new HelpBlock(SYNONYMS_NAME);
      for (int i = 1; i < allNames.length; i++) {
        synonyms.addChild(new HelpBlock(allNames[i]));
      }
      root.addChild(synonyms);
    }

    // Now comes the turn to display synopsis if any
    if (!StringUtils.isBlank(cliCommand.help())) {
      HelpBlock synopsis = new HelpBlock(SYNOPSIS_NAME);
      synopsis.addChild(new HelpBlock(cliCommand.help()));
      root.addChild(synopsis);
    }

    // Now display the syntax for the command
    HelpBlock syntaxBlock = new HelpBlock(SYNTAX_NAME);
    String syntax = getSyntaxString(commandName, annotations, parameterTypes);
    syntaxBlock.addChild(new HelpBlock(syntax));
    root.addChild(syntaxBlock);

    // Detailed description of all the Options
    if (annotations.length > 0) {
      HelpBlock options = new HelpBlock(OPTIONS_NAME);
      for (int i = 0; i < annotations.length; i++) {
        CliOption cliOption = getAnnotation(annotations[i], CliOption.class);
        HelpBlock optionNode = getOptionDetail(cliOption);
        options.addChild(optionNode);
      }
      root.addChild(options);
    }
    return root;
  }

  HelpBlock getOptionDetail(CliOption cliOption) {
    HelpBlock optionNode = new HelpBlock(getPrimaryKey(cliOption));
    String help = cliOption.help();
    optionNode.addChild(new HelpBlock((!StringUtils.isBlank(help) ? help : "")));
    if (getSynonyms(cliOption).size() > 0) {
      StringBuilder builder = new StringBuilder();
      for (String string : getSynonyms(cliOption)) {
        if (builder.length() > 0) {
          builder.append(",");
        }
        builder.append(string);
      }
      optionNode.addChild(new HelpBlock(SYNONYMS_SUB_NAME + builder.toString()));
    }
    optionNode.addChild(
        new HelpBlock(REQUIRED_SUB_NAME + ((cliOption.mandatory()) ? TRUE_TOKEN : FALSE_TOKEN)));
    if (!isNullOrBlank(cliOption.specifiedDefaultValue())) {
      optionNode.addChild(
          new HelpBlock(SPECIFIEDDEFAULTVALUE_SUB_NAME + cliOption.specifiedDefaultValue()));
    }
    if (!isNullOrBlank(cliOption.unspecifiedDefaultValue())) {
      optionNode.addChild(new HelpBlock(
          UNSPECIFIEDDEFAULTVALUE_VALUE_SUB_NAME + cliOption.unspecifiedDefaultValue()));
    }
    return optionNode;
  }

  private <T> T getAnnotation(Annotation[] annotations, Class<?> klass) {
    for (Annotation annotation : annotations) {
      if (klass.isAssignableFrom(annotation.getClass())) {
        return (T) annotation;
      }
    }
    return null;
  }

  String getSyntaxString(String commandName, Annotation[][] annotations, Class[] parameterTypes) {
    StringBuffer buffer = new StringBuffer();
    buffer.append(commandName);
    for (int i = 0; i < annotations.length; i++) {
      CliOption cliOption = getAnnotation(annotations[i], CliOption.class);
      String optionString = getOptionString(cliOption, parameterTypes[i]);
      if (cliOption.mandatory()) {
        buffer.append(" ").append(optionString);
      } else {
        buffer.append(" [").append(optionString).append("]");
      }
    }
    return buffer.toString();
  }

  /**
   * this builds the following format of strings: key (as in sh and help) --key=value --key(=value)?
   * (if has specifiedDefaultValue) --key=value(,value)* (if the value is a list)
   *
   * @return option string
   */
  private static String getOptionString(CliOption cliOption, Class<?> optionType) {
    String key0 = cliOption.key()[0];
    if ("".equals(key0)) {
      return (cliOption.key()[1]);
    }

    StringBuffer buffer = new StringBuffer();
    buffer.append(GfshParser.LONG_OPTION_SPECIFIER).append(key0);

    boolean hasSpecifiedDefault = !isNullOrBlank(cliOption.specifiedDefaultValue());

    if (hasSpecifiedDefault) {
      buffer.append("(");
    }

    buffer.append(GfshParser.OPTION_VALUE_SPECIFIER).append(VALUE_FIELD);

    if (hasSpecifiedDefault) {
      buffer.append(")?");
    }

    if (isCollectionOrArrayType(optionType)) {
      buffer.append("(").append(",").append(VALUE_FIELD).append(")*");
    }

    return buffer.toString();
  }

  private static boolean isCollectionOrArrayType(Class<?> typeToCheck) {
    return typeToCheck != null
        && (typeToCheck.isArray() || Collection.class.isAssignableFrom(typeToCheck));
  }

  private static String getPrimaryKey(CliOption option) {
    String[] keys = option.key();
    if (keys.length == 0) {
      throw new RuntimeException("Invalid option keys");
    } else if ("".equals(keys[0])) {
      return keys[1];
    } else {
      return keys[0];
    }
  }

  private static List<String> getSynonyms(CliOption option) {
    List<String> synonyms = new ArrayList<>();
    String[] keys = option.key();
    if (keys.length < 2)
      return synonyms;
    // if the primary key is empty (like sh and help command), then there should be no synonyms.
    if ("".equals(keys[0]))
      return synonyms;

    for (int i = 1; i < keys.length; i++) {
      synonyms.add(keys[i]);
    }
    return synonyms;
  }

  private static boolean isNullOrBlank(String value) {
    return StringUtils.isBlank(value) || CliMetaData.ANNOTATION_NULL_VALUE.equals(value);
  }

  public Set<String> getCommands() {
    return commands.keySet();
  }
}
