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

package org.mobicents.gmlc.service;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.jboss.as.controller.services.path.PathManager;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.mobicents.gmlc.GmlcManagement;
import org.mobicents.gmlc.GmlcShellExecutor;
import org.mobicents.protocols.ss7.scheduler.DefaultClock;
import org.mobicents.protocols.ss7.scheduler.Scheduler;
import org.mobicents.ss7.management.console.ShellExecutor;
import org.mobicents.ss7.management.console.ShellServer;
import org.mobicents.ss7.management.console.ShellServerWildFly;
import org.mobicents.ss7.service.SS7ServiceInterface;

import javolution.util.FastList;

public class GmlcService implements Service<GmlcService> {

  public static final GmlcService INSTANCE = new GmlcService();

  private final Logger log = Logger.getLogger(GmlcService.class);

  public static ServiceName getServiceName() {
    return ServiceName.of("restcomm", "gmlc-service");
  }

  private final InjectedValue<SS7ServiceInterface> ss7Service = new InjectedValue<SS7ServiceInterface>();

  public InjectedValue<SS7ServiceInterface> getSS7Service() {
    return ss7Service;
  }

  private final InjectedValue<PathManager> pathManagerInjector = new InjectedValue<PathManager>();

  public InjectedValue<PathManager> getPathManagerInjector() {
    return pathManagerInjector;
  }

  private final InjectedValue<MBeanServer> mbeanServer = new InjectedValue<MBeanServer>();

  public InjectedValue<MBeanServer> getMbeanServer() {
    return mbeanServer;
  }

  private static final String DATA_DIR = "jboss.server.data.dir";

  private ModelNode fullModel;

  private Scheduler schedulerMBean = null;
  private GmlcManagement gmlcManagementMBean = null;
  private GmlcShellExecutor gmlcShellExecutor = null;

  private ShellServer shellExecutorMBean = null;

  public void setModel(ModelNode model) {
    this.fullModel = model;
  }

  private ModelNode peek(ModelNode node, String... args) {
    for (String arg : args) {
      if (!node.hasDefined(arg)) {
        return null;
      }
      node = node.get(arg);
    }
    return node;
  }

  private String getPropertyString(String mbeanName, String propertyName, String defaultValue) {
    String result = defaultValue;
    ModelNode propertyNode = peek(fullModel, "mbean", mbeanName, "property", propertyName);
    if (propertyNode != null && propertyNode.isDefined()) {
      // log.debug("propertyNode: "+propertyNode);
      // todo: test TYPE?
      result = propertyNode.get("value").asString();
    }
    return (result == null) ? defaultValue : result;
  }

  private int getPropertyInt(String mbeanName, String propertyName, int defaultValue) {
    int result = defaultValue;
    ModelNode propertyNode = peek(fullModel, "mbean", mbeanName, "property", propertyName);
    if (propertyNode != null && propertyNode.isDefined()) {
      // log.debug("propertyNode: "+propertyNode);
      // todo: test TYPE?
      result = propertyNode.get("value").asInt();
    }
    return result;
  }

  @Override
  public GmlcService getValue() throws IllegalStateException, IllegalArgumentException {
    return this;
  }

  @Override
  public void start(StartContext context) throws StartException {

    log.info("Starting GmlcService");

    this.gmlcManagementMBean = initManagementMBean();

    if (shellExecutorExists()) {

      this.schedulerMBean = initSchedulerMBean();
      this.gmlcShellExecutor = initShellExecutor();

      shellExecutorMBean = null;
      try {
        FastList<ShellExecutor> shellExecutors = new FastList<ShellExecutor>();
        shellExecutors.add(gmlcShellExecutor);
        shellExecutors.add(ss7Service.getValue().getBeanTcapExecutor());
        shellExecutors.add(ss7Service.getValue().getBeanM3uaShellExecutor());
        shellExecutors.add(ss7Service.getValue().getBeanSctpShellExecutor());
        shellExecutors.add(ss7Service.getValue().getBeanSccpExecutor());

        String address = getPropertyString("ShellExecutor", "address", "127.0.0.1");
        int port = getPropertyInt("ShellExecutor", "port", 3435);
        String securityDomain = getPropertyString("ShellExecutor", "securityDomain", "jmx-console");

        shellExecutorMBean = new ShellServerWildFly(schedulerMBean, shellExecutors);
        shellExecutorMBean.setAddress(address);
        shellExecutorMBean.setPort(port);
        shellExecutorMBean.setSecurityDomain(securityDomain);
      } catch (Exception e) {
        throw new StartException("ShellExecutor MBean creating is failed: " + e.getMessage(), e);
      }

      // starting
      try {
        schedulerMBean.start();
        shellExecutorMBean.start();
      } catch (Exception e) {
        throw new StartException("MBeans starting is failed: " + e.getMessage(), e);
      }
    }
  }

  private Scheduler initSchedulerMBean() {
    Scheduler schedulerMBean = null;
    try {
      schedulerMBean = new Scheduler();
      DefaultClock ss7Clock = initSs7Clock();
      schedulerMBean.setClock(ss7Clock);
    } catch (Exception e) {
      log.warn("SS7Scheduler MBean creating is failed: " + e);
    }
    return schedulerMBean;
  }

  private DefaultClock initSs7Clock() {
    DefaultClock ss7Clock = null;
    try {
      ss7Clock = new DefaultClock();
    } catch (Exception e) {
      log.warn("SS7Clock MBean creating is failed: " + e);
    }
    return ss7Clock;
  }

  private GmlcManagement initManagementMBean() throws StartException {
    String dataDir = pathManagerInjector.getValue().getPathEntry(DATA_DIR).resolvePath();
    GmlcManagement gmlcManagementMBean = GmlcManagement.getInstance("GmlcManagement");
    gmlcManagementMBean.setPersistDir(dataDir);

    try {
      gmlcManagementMBean.start();
    } catch (Exception e) {
      throw new StartException("gmlcManagementMBean MBean creating is failed: " + e.getMessage(), e);
    }
    registerMBean(gmlcManagementMBean, "org.mobicents.gmlc.domain:name=GmlcManagement");
    return gmlcManagementMBean;
  }

  private GmlcShellExecutor initShellExecutor() throws StartException {
    try {
      GmlcShellExecutor gmlcShellExecutor = new GmlcShellExecutor();
      gmlcShellExecutor.setGmlcManagement(gmlcManagementMBean);
      return gmlcShellExecutor;
    } catch (Exception e) {
      throw new StartException("GmlcShellExecutor MBean creating is failed: " + e.getMessage(), e);
    }
  }

  private boolean shellExecutorExists() {
    ModelNode shellExecutorNode = peek(fullModel, "mbean", "ShellExecutor");
    return shellExecutorNode != null;
  }

  @Override
  public void stop(StopContext context) {
    log.info("Stopping SmppExtension Service");

    // scheduler - stop
    try {
      if (shellExecutorMBean != null)
        shellExecutorMBean.stop();
      if (schedulerMBean != null)
        schedulerMBean.stop();
    } catch (Exception e) {
      log.warn("MBean stopping is failed: " + e);
    }
  }

  private void registerMBean(Object mBean, String name) throws StartException {
    try {
      getMbeanServer().getValue().registerMBean(mBean, new ObjectName(name));
    } catch (Throwable e) {
      throw new StartException(e);
    }
  }

  @SuppressWarnings("unused")
  private void unregisterMBean(String name) {
    try {
      getMbeanServer().getValue().unregisterMBean(new ObjectName(name));
    } catch (Throwable e) {
      log.error("failed to unregister mbean", e);
    }
  }
}
