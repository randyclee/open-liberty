/*******************************************************************************
 * Copyright (c) 2019, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.metrics.monitor;

import java.lang.management.ManagementFactory;
import java.util.HashSet;
import java.util.Set;

import javax.management.MBeanServer;

import org.eclipse.microprofile.metrics.Metadata;
import org.eclipse.microprofile.metrics.MetricID;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.MetricType;
import org.eclipse.microprofile.metrics.Tag;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.microprofile.metrics.impl.SharedMetricRegistries;

public class MonitorMetrics {

	private static final TraceComponent tc = Tr.register(MonitorMetrics.class);
	
	protected String objectName;
	protected String mbeanStatsName;
	protected MBeanServer mbs;
	protected Set<MetricID> metricIDs;

	public MonitorMetrics(String objectName) {
		this.mbs = ManagementFactory.getPlatformMBeanServer();
		this.objectName = objectName;
		this.metricIDs = new HashSet<MetricID>();
		
	}

	public void createMetrics(SharedMetricRegistries sharedMetricRegistry, String[][] data) {
		
        MetricRegistry vendorRegistry = sharedMetricRegistry.getOrCreate(MetricRegistry.Type.VENDOR.getName());
        MetricRegistry baseRegistry = sharedMetricRegistry.getOrCreate(MetricRegistry.Type.BASE.getName());
        MetricRegistry metricRegistry = null;
        
        for (String[] metricData : data) {
        	
            String metricName = metricData[MappingTable.METRIC_NAME];
            String metricTagName = metricData[MappingTable.MBEAN_STATS_NAME];
            
            Tag metricTag = null;
            if (metricTagName != null) {
            	metricTag = new Tag(metricTagName, getMBeanStatsString());
            }
            MetricID metricID = new MetricID(metricName, metricTag);
            MetricType type = MetricType.valueOf(metricData[MappingTable.METRIC_TYPE]);
            
            /*
             * New for the REST metrics (which are registered under base)
             * Will there be future optional base metrics?
             */
            metricRegistry = (metricData[MappingTable.METRIC_REGISTRY_TYPE].equalsIgnoreCase("vendor") ) ? vendorRegistry : baseRegistry;
            
            if (MetricType.COUNTER.equals(type)) {
                MonitorCounter mc = metricData[MappingTable.MBEAN_SUBATTRIBUTE] == null ? 
                        new MonitorCounter(mbs, objectName, metricData[MappingTable.MBEAN_ATTRIBUTE]) :
                        new MonitorCounter(mbs, objectName, metricData[MappingTable.MBEAN_ATTRIBUTE], metricData[MappingTable.MBEAN_SUBATTRIBUTE]);
                
                        metricRegistry.register(
        				Metadata.builder().withName(metricName).withDisplayName(metricData[MappingTable.METRIC_DISPLAYNAME]).withDescription(metricData[MappingTable.METRIC_DESCRIPTION]).withType(type).withUnit(metricData[MappingTable.METRIC_UNIT]).build(), 
        			mc, metricTag);
        		metricIDs.add(metricID);
        		Tr.debug(tc, "Registered " + metricID.toString());
        	} else if (MetricType.GAUGE.equals(type)) {
            	MonitorGauge<Number> mg =  metricData[MappingTable.MBEAN_SUBATTRIBUTE] == null ?
            		new MonitorGauge<Number>(mbs, objectName, metricData[MappingTable.MBEAN_ATTRIBUTE]) :
            		new MonitorGauge<Number>(mbs, objectName, metricData[MappingTable.MBEAN_ATTRIBUTE], metricData[MappingTable.MBEAN_SUBATTRIBUTE]);
            		metricRegistry.register(
        				Metadata.builder().withName(metricName).withDisplayName(metricData[MappingTable.METRIC_DISPLAYNAME]).withDescription(metricData[MappingTable.METRIC_DESCRIPTION]).withType(type).withUnit(metricData[MappingTable.METRIC_UNIT]).build(),
        			mg, metricTag);
        		metricIDs.add(metricID);
        		Tr.debug(tc, "Registered " + metricID.toString());
        	} 
        	// Only REST_Stats is using SIMPLETIMER at the moment
            else if (MetricType.SIMPLE_TIMER.equals(type)) {
	        		
        		/*
        		 * Since only REST Stats is the only one using SIMPLETIMER
        		 * we're leveraging the sub-attribute at the moment as the second attribute to retreive
        		 */
        		
        		MonitorSimpleTimer mst = new MonitorSimpleTimer(mbs, objectName, metricData[MappingTable.MBEAN_ATTRIBUTE], metricData[MappingTable.MBEAN_SUBATTRIBUTE], metricData[MappingTable.MBEAN_SECOND_ATTRIBUTE], metricData[MappingTable.MBEAN_SECOND_SUBATTRIBUTE]);
        		String[] objName_rest = getRESTMBeanStatsTags();
        		
        		Tag classTag = new Tag("class" , objName_rest[1]);
        		Tag methodTag= new Tag("method" , objName_rest[2]);
        		
        		metricRegistry.register(
        				Metadata.builder().withName(metricName).withDisplayName(metricData[MappingTable.METRIC_DISPLAYNAME]).withDescription(metricData[MappingTable.METRIC_DESCRIPTION]).withType(type).withUnit(metricData[MappingTable.METRIC_UNIT]).build(),
        			mst, classTag, methodTag);
        		metricIDs.add(metricID);
        		Tr.debug(tc, "Registered " + metricID.toString());
            } else {
            	Tr.debug(tc, "Falied to register " + metricName + " because of invalid type " + type);
            }
            
            //reset
            metricRegistry = null;
        }		
	}

	protected String[] getRESTMBeanStatsTags() {
		String[] Tags = new String[3];
		for (String subString : objectName.split(",")) {
			subString = subString.trim();
            if (subString.contains("name=")) {
            	Tags = subString.split("/");
            	//blank method
            	Tags[2] = Tags[2].replaceAll("\\(\\)", "");
            	//otherwise first bracket becomes underscores
            	Tags[2] = Tags[2].replaceAll("\\(", "_");
            	//second bracket is removed
            	Tags[2] = Tags[2].replaceAll("\\(", "");
            	break;
            }
		}
		return Tags;
	}
	
	
	protected String getMBeanStatsString() {
    	if (mbeanStatsName == null) {
    		String serviceName = null;
    		String serviceURL = null;
    		String portName = null;
    		String mbeanObjName = null;
    		StringBuffer sb = new StringBuffer();
            for (String subString : objectName.split(",")) {
                subString = subString.trim();
                if (subString.contains("service=")) {	
                	serviceName = getMBeanStatsServiceName(subString);
                	serviceURL = getMBeanStatsServiceURL(subString);
                	continue;
                }
                if (subString.contains("port=")) {
                	portName = getMBeanStatsPortName(subString);
                	continue;
                }
                if (subString.contains("name=")) {
                	mbeanObjName = getMBeanStatsName(subString);
                	break;
                }
            }
            if (serviceURL != null && serviceName != null && portName != null) {
            	sb.append(serviceURL);
            	sb.append(".");
            	sb.append(serviceName);
            	sb.append(".");
            	sb.append(portName);
            }
            else if (mbeanObjName != null) {
            	sb.append(mbeanObjName);
            }
            else {
            	sb.append("unknown");
            }
            
            mbeanStatsName = sb.toString();
    	}
        return mbeanStatsName;
	}
	
	private String getMBeanStatsName(String nameStr) {
		String mbeanName = nameStr.split("=")[1];
		mbeanName = mbeanName.replaceAll(" ", "_"); 
		mbeanName = mbeanName.replaceAll("/", "_");
		mbeanName = mbeanName.replaceAll("[^a-zA-Z0-9._]", "_");
    	return mbeanName;
	}
	
	private String getMBeanStatsServiceName(String serviceStr) {
    	serviceStr = serviceStr.split("=")[1];
    	serviceStr = serviceStr.replaceAll("\"", "");
    	String serviceName = serviceStr.substring(serviceStr.indexOf("}") + 1);
    	return serviceName;
	}
	
	private String getMBeanStatsServiceURL(String serviceStr) {
    	serviceStr = serviceStr.split("=")[1];
    	serviceStr = serviceStr.replaceAll("\"", "");
    	String serviceURL = serviceStr.substring(serviceStr.indexOf("{") + 1, serviceStr.indexOf("}"));
    	serviceURL = serviceURL.replace("http://", "").replace("https://", "").replace("/", ".");
    	return serviceURL;
	}
	
	private String getMBeanStatsPortName(String portStr) {
		portStr = portStr.split("=")[1];
    	String portName = portStr.replaceAll("\"", "");	
    	return portName;
	}

	public void unregisterMetrics(SharedMetricRegistries sharedMetricRegistry) {
		MetricRegistry registry = sharedMetricRegistry.getOrCreate(MetricRegistry.Type.VENDOR.getName());
		for (MetricID metricID : metricIDs) {
			boolean rc = registry.remove(metricID);
			Tr.debug(tc, "Unregistered " + metricID.toString() + " " + (rc ? "successfully" : "unsuccessfully"));
		}
		metricIDs.clear();
	}
}