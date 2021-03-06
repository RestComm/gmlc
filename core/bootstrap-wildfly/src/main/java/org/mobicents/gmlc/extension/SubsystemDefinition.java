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

import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.registry.ManagementResourceRegistration;

/**
 * @author <a href="mailto:tcerar@redhat.com">Tomaz Cerar</a>
 */
public class SubsystemDefinition extends SimpleResourceDefinition {
  public static final SubsystemDefinition INSTANCE = new SubsystemDefinition();

  private SubsystemDefinition() {
    super(GmlcExtension.SUBSYSTEM_PATH,
        GmlcExtension.getResourceDescriptionResolver(null),
        //We always need to add an 'add' operation
        SubsystemAdd.INSTANCE,
        //Every resource that is added, normally needs a remove operation
        SubsystemRemove.INSTANCE);
  }

  @Override
  public void registerOperations(ManagementResourceRegistration resourceRegistration) {
    super.registerOperations(resourceRegistration);
    //you can register aditional operations here
  }

  @Override
  public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
    //you can register attributes here
  }
}
