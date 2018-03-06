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

import static org.mobicents.gmlc.extension.GmlcMbeanPropertyDefinition.PROPERTY_ATTRIBUTES;

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.dmr.ModelNode;

class GmlcMbeanPropertyAdd extends AbstractAddStepHandler {

  public static final GmlcMbeanPropertyAdd INSTANCE = new GmlcMbeanPropertyAdd();

  private GmlcMbeanPropertyAdd() {
  }

  @Override
  protected void populateModel(final ModelNode operation, final ModelNode model) throws OperationFailedException {
    GmlcMbeanPropertyDefinition.NAME_ATTR.validateAndSet(operation, model);
    for (SimpleAttributeDefinition def : PROPERTY_ATTRIBUTES) {
      def.validateAndSet(operation, model);
    }
  }
}
