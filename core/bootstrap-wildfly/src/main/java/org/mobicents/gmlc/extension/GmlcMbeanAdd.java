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
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;

import java.util.List;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.mobicents.gmlc.extension.GmlcMbeanDefinition.MBEAN_ATTRIBUTES;

class GmlcMbeanAdd extends AbstractAddStepHandler {

  static final GmlcMbeanAdd INSTANCE = new GmlcMbeanAdd();

  private GmlcMbeanAdd() {
  }

  @Override
  protected void populateModel(final ModelNode operation, final ModelNode model) throws OperationFailedException {
    PathAddress address = PathAddress.pathAddress(operation.require(OP_ADDR));
    String name = GmlcMbeanDefinition.NAME_ATTR.getName();
    model.get(name).set(address.getLastElement().getValue());

    //SS7MbeanDefinition.NAME_ATTR.validateAndSet(operation, model);
    for (SimpleAttributeDefinition def : MBEAN_ATTRIBUTES) {
      def.validateAndSet(operation, model);
    }
  }

  @Override
  protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model,
                                ServiceVerificationHandler verificationHandler, List<ServiceController<?>> newControllers)
      throws OperationFailedException {

    final PathAddress address = PathAddress.pathAddress(operation.get(OP_ADDR));
    final String mbeanName = address.getLastElement().getValue();

    // here we can add mbeans with reflection usage
  }
}
