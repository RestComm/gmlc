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

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.parsing.Attribute;
import org.jboss.as.controller.parsing.ParseUtils;
import org.jboss.as.controller.persistence.SubsystemMarshallingContext;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;
import org.jboss.staxmapper.XMLElementReader;
import org.jboss.staxmapper.XMLElementWriter;
import org.jboss.staxmapper.XMLExtendedStreamReader;
import org.jboss.staxmapper.XMLExtendedStreamWriter;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import java.util.Collections;
import java.util.List;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.*;
import static org.jboss.as.controller.parsing.ParseUtils.*;

/**
 * /**
 * The subsystem parser, which uses stax to read and write to and from xml
 */
class GmlcSubsystemParser implements XMLStreamConstants, XMLElementReader<List<ModelNode>>,
    XMLElementWriter<SubsystemMarshallingContext> {

  private static final GmlcSubsystemParser INSTANCE = new GmlcSubsystemParser();

  static GmlcSubsystemParser getInstance() {
    return INSTANCE;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void writeContent(XMLExtendedStreamWriter writer, SubsystemMarshallingContext context) throws XMLStreamException {
    context.startSubsystemElement(Namespace.CURRENT.getUriString(), false);

    final ModelNode node = context.getModelNode();
    final ModelNode mbean = node.get(GmlcMbeanDefinition.MBEAN);

    for (Property mbeanProp : mbean.asPropertyList()) {
      writer.writeStartElement(GmlcMbeanDefinition.MBEAN);

      final ModelNode mbeanEntry = mbeanProp.getValue();

      GmlcMbeanDefinition.NAME_ATTR.marshallAsAttribute(mbeanEntry, true, writer);
      GmlcMbeanDefinition.TYPE_ATTR.marshallAsAttribute(mbeanEntry, true, writer);

      final ModelNode property = mbeanEntry.get(GmlcMbeanPropertyDefinition.PROPERTY);
      if (property != null && property.isDefined()) {
        for (Property propertyProp : property.asPropertyList()) {
          writer.writeStartElement(GmlcMbeanPropertyDefinition.PROPERTY);

          final ModelNode propertyEntry = propertyProp.getValue();

          GmlcMbeanPropertyDefinition.NAME_ATTR.marshallAsAttribute(propertyEntry, true, writer);
          GmlcMbeanPropertyDefinition.TYPE_ATTR.marshallAsAttribute(propertyEntry, true, writer);
          GmlcMbeanPropertyDefinition.VALUE_ATTR.marshallAsAttribute(propertyEntry, true, writer);

          writer.writeEndElement();
        }
      }

      writer.writeEndElement();
    }

    writer.writeEndElement();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void readElement(XMLExtendedStreamReader reader, List<ModelNode> list) throws XMLStreamException {
    PathAddress address = PathAddress.pathAddress(PathElement.pathElement(SUBSYSTEM, GmlcExtension.SUBSYSTEM_NAME));

    final ModelNode subsystem = new ModelNode();
    subsystem.get(OP).set(ADD);
    subsystem.get(OP_ADDR).set(address.toModelNode());
    list.add(subsystem);

    // mbean elements
    while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
      switch (Namespace.forUri(reader.getNamespaceURI())) {
        case SMPPEXT_1_0: {
          final String tagName = reader.getLocalName();
          if (tagName.equals(GmlcMbeanDefinition.MBEAN)) {
            parseMbean(reader, address, list);
          }
          break;
        }
        default: {
          throw unexpectedElement(reader);
        }
      }
    }
  }

  static void parseMbean(XMLExtendedStreamReader reader, PathAddress parent, List<ModelNode> list)
      throws XMLStreamException {
    String name = null;
    final ModelNode mbean = new ModelNode();

    // MBean Attributes
    final int count = reader.getAttributeCount();
    for (int i = 0; i < count; i++) {
      requireNoNamespaceAttribute(reader, i);
      final String attribute = reader.getAttributeLocalName(i);
      final String value = reader.getAttributeValue(i);
      switch (GmlcMbeanDefinition.Element.of(attribute)) {
        case NAME: {
          name = value;
          GmlcMbeanDefinition.NAME_ATTR.parseAndSetParameter(value, mbean, reader);
          break;
        }
        case TYPE: {
          GmlcMbeanDefinition.TYPE_ATTR.parseAndSetParameter(value, mbean, reader);
          break;
        }
        default: {
          throw unexpectedAttribute(reader, i);
        }
      }
    }

    //ParseUtils.requireNoContent(reader);

    if (name == null) {
      throw missingRequired(reader, Collections.singleton(Attribute.NAME));
    }

    mbean.get(OP).set(ADD);
    PathAddress address = PathAddress.pathAddress(parent,
        PathElement.pathElement(GmlcMbeanDefinition.MBEAN, name));
    mbean.get(OP_ADDR).set(address.toModelNode());
    list.add(mbean);

    // properties elements
    while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
      switch (Namespace.forUri(reader.getNamespaceURI())) {
        case SMPPEXT_1_0: {
          final String tagName = reader.getLocalName();
          switch (tagName) {
            case GmlcMbeanPropertyDefinition.PROPERTY: {
              parseProperty(reader, address, list);
              break;
            }
            default: {
              break;
            }
          }
          break;
        }
        default: {
          throw unexpectedElement(reader);
        }
      }
    }
  }

  static void parseProperty(XMLExtendedStreamReader reader, PathAddress parent, List<ModelNode> list)
      throws XMLStreamException {
    String name = null;
    final ModelNode property = new ModelNode();

    final int count = reader.getAttributeCount();
    for (int i = 0; i < count; i++) {
      requireNoNamespaceAttribute(reader, i);
      final String attribute = reader.getAttributeLocalName(i);
      final String value = reader.getAttributeValue(i);
      switch (GmlcMbeanPropertyDefinition.Element.of(attribute)) {
        case NAME: {
          name = value;
          GmlcMbeanPropertyDefinition.NAME_ATTR.parseAndSetParameter(value, property, reader);
          break;
        }
        case TYPE: {
          GmlcMbeanPropertyDefinition.TYPE_ATTR.parseAndSetParameter(value, property, reader);
          break;
        }
        case VALUE: {
          GmlcMbeanPropertyDefinition.VALUE_ATTR.parseAndSetParameter(value, property, reader);
          break;
        }
        default: {
          throw unexpectedAttribute(reader, i);
        }
      }
    }

    ParseUtils.requireNoContent(reader);

    if (name == null) {
      throw missingRequired(reader, Collections.singleton(Attribute.NAME));
    }

    property.get(OP).set(ADD);
    PathAddress address = PathAddress.pathAddress(parent,
        PathElement.pathElement(GmlcMbeanPropertyDefinition.PROPERTY, name));
    property.get(OP_ADDR).set(address.toModelNode());
    list.add(property);
  }
}
