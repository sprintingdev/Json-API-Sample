package com.intelligrape.example.json

import org.codehaus.groovy.grails.support.proxy.ProxyHandler
import org.codehaus.groovy.grails.support.proxy.DefaultProxyHandler
import org.codehaus.groovy.grails.web.converters.ConverterUtil
import grails.converters.JSON
import org.codehaus.groovy.grails.web.converters.exceptions.ConverterException
import org.codehaus.groovy.grails.web.json.JSONWriter
import org.codehaus.groovy.grails.commons.GrailsDomainClass
import org.springframework.beans.BeanWrapper
import org.springframework.beans.BeanWrapperImpl
import org.codehaus.groovy.grails.commons.GrailsDomainClassProperty
import org.codehaus.groovy.grails.support.proxy.EntityProxyHandler
import org.codehaus.groovy.grails.web.converters.marshaller.ObjectMarshaller
import org.codehaus.groovy.grails.commons.GrailsClassUtils

class CustomDomainClassJSONMarshaller implements ObjectMarshaller<JSON>{

    private boolean includeVersion = false;
    private ProxyHandler proxyHandler;

    public CustomDomainClassJSONMarshaller(boolean includeVersion) {
        this(includeVersion, new DefaultProxyHandler());
    }

    public CustomDomainClassJSONMarshaller(boolean includeVersion, ProxyHandler proxyHandler) {
        this.includeVersion = includeVersion;
        this.proxyHandler = proxyHandler;
    }

    public boolean isIncludeVersion() {
        return includeVersion;
    }

    public void setIncludeVersion(boolean includeVersion) {
        this.includeVersion = includeVersion;
    }

    public boolean supports(Object object) {
        return ConverterUtil.isDomainClass(object.getClass());
    }

    public void marshalObject(Object value, JSON json) throws ConverterException {
        JSONWriter writer = json.getWriter();
        value = proxyHandler.unwrapIfProxy(value);
        Class<?> clazz = value.getClass();
        GrailsDomainClass domainClass = ConverterUtil.getDomainClass(clazz.getName());
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

        GrailsDomainClassProperty[] properties = domainClass.getPersistentProperties();

        for (GrailsDomainClassProperty property : properties) {
            writer.key(property.getName());
            if (!property.isAssociation()) {
                // Write non-relation property
                Object val = beanWrapper.getPropertyValue(property.getName());

                //If enum, write the value as enum name instead of having a json of the form {declaringType: value} etc

                if(property.isEnum()){
                    writer.value(val)
                } else{
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
                        else if (referenceObject instanceof Collection){
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
                        if(referencedDomainClass == null || property.isEmbedded() || GrailsClassUtils.isJdk5Enum(property.getType())) {
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
                                for (Object el : o) {
                                    asShortObject(el, json, referencedIdProperty, referencedDomainClass);
                                }
                                writer.endArray();
                            }
                            else if (referenceObject instanceof Map) {
                                Map<Object, Object> map = (Map<Object, Object>) referenceObject;
                                for (Map.Entry<Object, Object> entry : map.entrySet()) {
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
        writer.endObject();
    }

    protected void asShortObject(Object refObj, JSON json, GrailsDomainClassProperty idProperty, GrailsDomainClass referencedDomainClass) throws ConverterException {

        Object idValue;

        if(proxyHandler instanceof EntityProxyHandler) {
            idValue = ((EntityProxyHandler) proxyHandler).getProxyIdentifier(refObj);
            if(idValue == null) {
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
