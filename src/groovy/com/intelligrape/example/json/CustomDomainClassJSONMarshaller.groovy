package com.intelligrape.example.json

import grails.converters.JSON;

import org.codehaus.groovy.grails.commons.DomainClassArtefactHandler;
import org.codehaus.groovy.grails.commons.GrailsApplication;
import org.codehaus.groovy.grails.commons.GrailsClassUtils;
import org.codehaus.groovy.grails.commons.GrailsDomainClass;
import org.codehaus.groovy.grails.commons.GrailsDomainClassProperty;
import org.codehaus.groovy.grails.support.proxy.DefaultProxyHandler;
import org.codehaus.groovy.grails.support.proxy.EntityProxyHandler;
import org.codehaus.groovy.grails.support.proxy.ProxyHandler;
import org.codehaus.groovy.grails.web.converters.ConverterUtil;
import org.codehaus.groovy.grails.web.converters.exceptions.ConverterException;
import org.codehaus.groovy.grails.web.converters.marshaller.ObjectMarshaller;
import org.codehaus.groovy.grails.web.json.JSONWriter;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;

public class CustomDomainClassJSONMarshaller implements ObjectMarshaller<JSON> {

    private boolean includeVersion = false;
    private ProxyHandler proxyHandler;
    private GrailsApplication application;
    String jsonPropertyGroup

    public CustomDomainClassJSONMarshaller(boolean includeVersion, GrailsApplication application, String jsonPropertyGroup = "") {
        this(includeVersion, new DefaultProxyHandler(), application, jsonPropertyGroup);
    }

    public CustomDomainClassJSONMarshaller(boolean includeVersion, ProxyHandler proxyHandler, GrailsApplication application, String jsonPropertyGroup) {
        this.includeVersion = includeVersion;
        this.proxyHandler = proxyHandler;
        this.application = application;
        this.jsonPropertyGroup = jsonPropertyGroup
    }

    public boolean isIncludeVersion() {
        return includeVersion;
    }

    public void setIncludeVersion(boolean includeVersion) {
        this.includeVersion = includeVersion;
    }

    public boolean supports(Object object) {
        String name = ConverterUtil.trimProxySuffix(object.getClass().getName());
        return application.isArtefactOfType(DomainClassArtefactHandler.TYPE, name);
    }

    @SuppressWarnings(["unchecked", "rawtypes"])
    public void marshalObject(Object value, JSON json) throws ConverterException {
        JSONWriter writer = json.getWriter();
        value = proxyHandler.unwrapIfProxy(value);
        Class<?> clazz = value.getClass();

        GrailsDomainClass domainClass = (GrailsDomainClass) application.getArtefact(
                DomainClassArtefactHandler.TYPE, ConverterUtil.trimProxySuffix(clazz.getName()));
        BeanWrapper beanWrapper = new BeanWrapperImpl(value);

        writer.object();

        GrailsDomainClassProperty id = domainClass.getIdentifier();
        Object idValue = extractValue(value, id);

        json.property("id", idValue);

        if (isIncludeVersion()) {
            GrailsDomainClassProperty versionProperty = domainClass.getVersion();
            Object version = extractValue(value, versionProperty);
            json.property("version", version);
        }

        GrailsDomainClassProperty[] properties = getPropertiesToBeIncluded(domainClass);

        for (GrailsDomainClassProperty property: properties) {
            addPropertyToJSON(writer, property, beanWrapper, json)
        }
        writer.endObject();
    }

    private addPropertyToJSON(JSONWriter writer, GrailsDomainClassProperty property, BeanWrapperImpl beanWrapper, JSON json) {
        writer.key(property.getName());
        if (!property.isAssociation()) {
            // Write non-relation property
            Object val = beanWrapper.getPropertyValue(property.getName());
            if (property.isEnum()) {
                writer.value(val);
            } else {
                json.convertAnother(val);
            }
        }
        else {
            Object referenceObject = beanWrapper.getPropertyValue(property.getName());
            if (isRenderDomainClassRelations()) {
                if (referenceObject == null) {
                    writer.value(null);
                }
                else {
                    referenceObject = proxyHandler.unwrapIfProxy(referenceObject);
                    if (referenceObject instanceof SortedMap) {
                        referenceObject = new TreeMap((SortedMap) referenceObject);
                    }
                    else if (referenceObject instanceof SortedSet) {
                        referenceObject = new TreeSet((SortedSet) referenceObject);
                    }
                    else if (referenceObject instanceof Set) {
                        referenceObject = new HashSet((Set) referenceObject);
                    }
                    else if (referenceObject instanceof Map) {
                        referenceObject = new HashMap((Map) referenceObject);
                    }
                    else if (referenceObject instanceof Collection) {
                        referenceObject = new ArrayList((Collection) referenceObject);
                    }
                    json.convertAnother(referenceObject);
                }
            }
            else {
                if (referenceObject == null) {
                    json.value(null);
                }
                else {
                    GrailsDomainClass referencedDomainClass = property.getReferencedDomainClass();

                    // Embedded are now always fully rendered
                    if (referencedDomainClass == null || property.isEmbedded() || GrailsClassUtils.isJdk5Enum(property.getType())) {
                        json.convertAnother(referenceObject);
                    }
                    else if (property.isOneToOne() || property.isManyToOne() || property.isEmbedded()) {
                        asShortObject(referenceObject, json, referencedDomainClass.getIdentifier(), referencedDomainClass);
                    }
                    else {
                        GrailsDomainClassProperty referencedIdProperty = referencedDomainClass.getIdentifier();
                        @SuppressWarnings("unused")
                        String refPropertyName = referencedDomainClass.getPropertyName();
                        if (referenceObject instanceof Collection) {
                            Collection o = (Collection) referenceObject;
                            writer.array();
                            for (Object el: o) {
                                asShortObject(el, json, referencedIdProperty, referencedDomainClass);
                            }
                            writer.endArray();
                        }
                        else if (referenceObject instanceof Map) {
                            Map<Object, Object> map = (Map<Object, Object>) referenceObject;
                            for (Map.Entry<Object, Object> entry: map.entrySet()) {
                                String key = String.valueOf(entry.getKey());
                                Object o = entry.getValue();
                                writer.object();
                                writer.key(key);
                                asShortObject(o, json, referencedIdProperty, referencedDomainClass);
                                writer.endObject();
                            }
                        }
                    }
                }
            }
        }
    }

    protected GrailsDomainClassProperty[] getPropertiesToBeIncluded(GrailsDomainClass domainClass) {
        if (jsonPropertyGroup) {
            List<String> namesOfPropertiesToBeIncluded = GrailsClassUtils.getStaticPropertyValue(domainClass.clazz,
                    JSONConstants.JSON_PROPERTIES_MAP_NAME)[jsonPropertyGroup] as List<String>
            return domainClass.getProperties().findAll {it.name in namesOfPropertiesToBeIncluded}
        } else {
            return domainClass.getPersistentProperties()
        }
    }

    protected void asShortObject(Object refObj, JSON json, GrailsDomainClassProperty idProperty, GrailsDomainClass referencedDomainClass) throws ConverterException {

        Object idValue;

        if (proxyHandler instanceof EntityProxyHandler) {
            idValue = ((EntityProxyHandler) proxyHandler).getProxyIdentifier(refObj);
            if (idValue == null) {
                idValue = extractValue(refObj, idProperty);
            }
        }
        else {
            idValue = extractValue(refObj, idProperty);
        }
        JSONWriter writer = json.getWriter();
        writer.object();
        writer.key("id").value(idValue);
        writer.endObject();
    }

    protected Object extractValue(Object domainObject, GrailsDomainClassProperty property) {
        BeanWrapper beanWrapper = new BeanWrapperImpl(domainObject);
        return beanWrapper.getPropertyValue(property.getName());
    }

    protected boolean isRenderDomainClassRelations() {
        return false;
    }
}
