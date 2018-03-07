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

import org.jboss.as.controller.AbstractBoottimeAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.controller.services.path.PathManager;
import org.jboss.as.controller.services.path.PathManagerService;
import org.jboss.as.jmx.MBeanServerService;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.mobicents.gmlc.service.GmlcService;
import org.mobicents.ss7.service.SS7ExtensionService;
import org.mobicents.ss7.service.SS7ServiceInterface;

import javax.management.MBeanServer;
import java.util.List;

/**
 * Handler responsible for adding the subsystem resource to the model
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
class SubsystemAdd extends AbstractBoottimeAddStepHandler {

  static final SubsystemAdd INSTANCE = new SubsystemAdd();

  private final Logger log = Logger.getLogger(SubsystemAdd.class);

  private SubsystemAdd() {
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected void populateModel(ModelNode operation, ModelNode model) throws OperationFailedException {
    log.info("Populating the model");
    model.setEmptyObject();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void performBoottime(OperationContext context, ModelNode operation, ModelNode model,
                              ServiceVerificationHandler verificationHandler, List<ServiceController<?>> newControllers)
      throws OperationFailedException {

    ModelNode fullModel = Resource.Tools.readModel(context.readResource(PathAddress.EMPTY_ADDRESS));

    GmlcService service = GmlcService.INSTANCE;
    service.setModel(fullModel);

    ServiceName name = GmlcService.getServiceName();
    ServiceController<GmlcService> controller = context.getServiceTarget()
        .addService(name, service)
        .addDependency(PathManagerService.SERVICE_NAME, PathManager.class, service.getPathManagerInjector())
        .addDependency(MBeanServerService.SERVICE_NAME, MBeanServer.class, service.getMbeanServer())
        .addDependency(SS7ExtensionService.getServiceName(), SS7ServiceInterface.class, service.getSS7Service())
        .addListener(verificationHandler)
        .setInitialMode(ServiceController.Mode.ACTIVE)
        .install();
    newControllers.add(controller);

  }
}
