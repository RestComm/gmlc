/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2011-2018, Telestax Inc and individual contributors
 * by the @authors tag.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.mobicents.gmlc.extension;

import org.jboss.as.controller.*;
import org.jboss.as.controller.descriptions.StandardResourceDescriptionResolver;
import org.jboss.as.controller.operations.common.GenericSubsystemDescribeHandler;
import org.jboss.as.controller.parsing.ExtensionParsingContext;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.OperationEntry;
import org.jboss.logging.Logger;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DESCRIBE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;

/**
 *
 * @author
 *
 */
public class GmlcExtension implements Extension {

  private final Logger log = Logger.getLogger(GmlcExtension.class);

  /**
   * The name of our subsystem within the model.
   */
  public static final String SUBSYSTEM_NAME = "gmlc-extensions";
  private static final int MAJOR_VERSION = 1;
  private static final int MINOR_VERSION = 0;

  protected static final PathElement SUBSYSTEM_PATH = PathElement.pathElement(SUBSYSTEM, SUBSYSTEM_NAME);
  private static final String RESOURCE_NAME = GmlcExtension.class.getPackage().getName() + ".LocalDescriptions";

  static StandardResourceDescriptionResolver getResourceDescriptionResolver(final String keyPrefix) {
    String prefix = SUBSYSTEM_NAME + (keyPrefix == null ? "" : "." + keyPrefix);
    return new StandardResourceDescriptionResolver(prefix, RESOURCE_NAME, GmlcExtension.class.getClassLoader(), true, false);
  }

  @Override
  public void initializeParsers(ExtensionParsingContext context) {
    context.setSubsystemXmlMapping(SUBSYSTEM_NAME, Namespace.CURRENT.getUriString(), GmlcSubsystemParser.getInstance());
  }

  @Override
  public void initialize(ExtensionContext context) {
    final SubsystemRegistration subsystem =
            context.registerSubsystem(SUBSYSTEM_NAME, ModelVersion.create(MAJOR_VERSION, MINOR_VERSION));

    final ManagementResourceRegistration registration = subsystem.registerSubsystemModel(SubsystemDefinition.INSTANCE);

    final OperationDefinition describeOp = new SimpleOperationDefinitionBuilder(DESCRIBE,
        getResourceDescriptionResolver(null))
        .setEntryType(OperationEntry.EntryType.PRIVATE)
        .build();
    registration.registerOperationHandler(describeOp, GenericSubsystemDescribeHandler.INSTANCE, false);

    subsystem.registerXMLElementWriter(GmlcSubsystemParser.getInstance());

    // here we can register submodels
    final ManagementResourceRegistration mbeans = registration.registerSubModel(GmlcMbeanDefinition.INSTANCE);
  }
}
