/*
 * Copyright (c) 2019, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *
 */


package org.wso2.carbon.logging.updater;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.ops4j.pax.logging.PaxLoggingConstants;
import org.osgi.framework.Constants;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.wso2.carbon.logging.updater.internal.DataHolder;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.attribute.FileTime;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Properties;

/**
 * Logging configuration updater implementation to check and update pax-logging configuration realtime.
 */
public class LogConfigUpdater implements Runnable {

    static final Log LOG = LogFactory.getLog(LogConfigUpdater.class);

    private ConfigurationAdmin configurationAdmin;

    public LogConfigUpdater(ConfigurationAdmin configurationAdmin) {

        this.configurationAdmin = configurationAdmin;
    }

    @Override
    public void run() {

        try {
            FileTime modifiedTime = LoggingUpdaterUtil.readModifiedTime();
            FileTime lastModifiedTime = DataHolder.getInstance().getModifiedTime();
            if (lastModifiedTime != null) {
                if (modifiedTime.compareTo(lastModifiedTime) > 0) {
                    DataHolder.getInstance().setModifiedTime(modifiedTime);
                    updateLoggingConfiguration();
                }
            }
        } catch (LoggingUpdaterException e) {
            LOG.error("Error while reading modified Time", e);
        } catch (IOException e) {
            LOG.error("Error while updating logging configuration", e);
        }
    }

    private void updateLoggingConfiguration() throws IOException {

        Configuration configuration =
                configurationAdmin.getConfiguration(LoggingUpdaterConstants.PAX_LOGGING_CONFIGURATION_PID, "?");
        Dictionary properties = new Hashtable<>();
        properties.put(Constants.SERVICE_PID, PaxLoggingConstants.LOGGING_CONFIGURATION_PID);
        Hashtable paxLoggingProperties = getPaxLoggingProperties();
        paxLoggingProperties.forEach(properties::put);
        configuration.update(properties);
    }

    private Hashtable getPaxLoggingProperties() throws IOException {
        String paxPropertiesFileLocation = System.getProperty("org.ops4j.pax.logging.property.file");
        if (StringUtils.isNotEmpty(paxPropertiesFileLocation)) {
            File file = new File(paxPropertiesFileLocation);
            if (file.exists()) {
                Properties properties = new Properties();
                properties.load(new FileInputStream(file));
                return properties;
            }
        }
        return new Hashtable();
    }
}
