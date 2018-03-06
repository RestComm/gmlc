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
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelType;

import java.util.HashMap;
import java.util.Map;

/**
 * @author sergey.povarnin@telestax.com
 */
public class GmlcMbeanDefinition extends SimpleResourceDefinition {

  public enum Element {
    // must be first
    UNKNOWN(null),
    NAME("name"),
    TYPE("type");

    private final String name;

    Element(final String name) {
      this.name = name;
    }

    public String localName() {
      return name;
    }

    private static final Map<String, Element> MAP;

    static {
      final Map<String, Element> map = new HashMap<String, Element>();
      for (Element element : values()) {
        final String name = element.localName();
        if (name != null)
          map.put(name, element);
      }
      MAP = map;
    }

    public static Element of(final String localName) {
      final Element element = MAP.get(localName);
      return element == null ? UNKNOWN : element;
    }

  }

  protected static final SimpleAttributeDefinition NAME_ATTR =
      new SimpleAttributeDefinitionBuilder(Element.NAME.localName(), ModelType.STRING)
          .setXmlName(Element.NAME.localName())
          .setAllowNull(true) // todo should be false, but 'add' won't validate then
          .build();

  protected static final SimpleAttributeDefinition TYPE_ATTR =
      new SimpleAttributeDefinitionBuilder(Element.TYPE.localName(), ModelType.STRING)
          .setXmlName(Element.TYPE.localName())
          .setAllowNull(true) // todo should be false, but 'add' won't validate then
          .build();

  public static final String MBEAN = "mbean";
  public static final PathElement MBEAN_PATH = PathElement.pathElement(MBEAN);
  public static final GmlcMbeanDefinition INSTANCE = new GmlcMbeanDefinition();

  protected static final SimpleAttributeDefinition[] MBEAN_ATTRIBUTES = {
      //NAME, // name is read-only
      TYPE_ATTR,
  };

  private GmlcMbeanDefinition() {
    super(MBEAN_PATH,
        GmlcExtension.getResourceDescriptionResolver(MBEAN),
        GmlcMbeanAdd.INSTANCE,
        GmlcMbeanRemove.INSTANCE);
  }

  @Override
  public void registerChildren(ManagementResourceRegistration resourceRegistration) {
    super.registerChildren(resourceRegistration);
    resourceRegistration.registerSubModel(GmlcMbeanPropertyDefinition.INSTANCE);
  }

  @Override
  public void registerAttributes(ManagementResourceRegistration mbeans) {
    mbeans.registerReadOnlyAttribute(NAME_ATTR, null);
    for (SimpleAttributeDefinition def : MBEAN_ATTRIBUTES) {
      mbeans.registerReadWriteAttribute(def, null, new ReloadRequiredWriteAttributeHandler(def));
    }
  }

}
