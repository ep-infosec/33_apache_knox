/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.apache.knox.gateway.topology.discovery.cm.model.solr;

import com.cloudera.api.swagger.model.ApiConfigList;
import com.cloudera.api.swagger.model.ApiRole;
import com.cloudera.api.swagger.model.ApiService;
import com.cloudera.api.swagger.model.ApiServiceConfig;
import org.apache.knox.gateway.topology.discovery.cm.ServiceModel;
import org.apache.knox.gateway.topology.discovery.cm.model.AbstractServiceModelGenerator;

import java.util.Locale;

public class SolrServiceModelGenerator extends AbstractServiceModelGenerator {
  public static final String SERVICE      = "SOLR";
  public static final String SERVICE_TYPE = "SOLR";
  public static final String ROLE_TYPE    = "SOLR_SERVER";

  public static final String DISCOVERY_SERVICE_NAME         = "service-name";
  public static final String DISCOVERY_SERVICE_DISPLAY_NAME = "service-display-name";

  static final String USE_SSL    = "solr_use_ssl";
  static final String HTTP_PORT  = "solr_http_port";
  static final String HTTPS_PORT = "solr_https_port";

  @Override
  public String getService() {
    return SERVICE;
  }

  @Override
  public String getServiceType() {
    return SERVICE_TYPE;
  }

  @Override
  public String getRoleType() {
    return ROLE_TYPE;
  }

  @Override
  public ServiceModel.Type getModelType() {
    return ServiceModel.Type.API;
  }

  @Override
  public ServiceModel generateService(ApiService       service,
                                      ApiServiceConfig serviceConfig,
                                      ApiRole          role,
                                      ApiConfigList    roleConfig) {
    String hostname = role.getHostRef().getHostname();
    String scheme;
    String port;
    String sslEnabled = getServiceConfigValue(serviceConfig, USE_SSL);
    if(Boolean.parseBoolean(sslEnabled)) {
      scheme = "https";
      port = getRoleConfigValue(roleConfig, HTTPS_PORT);
    } else {
      scheme = "http";
      port = getRoleConfigValue(roleConfig, HTTP_PORT);
    }

    ServiceModel model =
        createServiceModel(String.format(Locale.getDefault(), "%s://%s:%s/solr/", scheme, hostname, port));
    model.addServiceProperty(USE_SSL, sslEnabled);
    model.addRoleProperty(getRoleType(), HTTP_PORT, getRoleConfigValue(roleConfig, HTTP_PORT));
    model.addRoleProperty(getRoleType(), HTTPS_PORT, getRoleConfigValue(roleConfig, HTTPS_PORT));

    // Add some service details for qualifying the discovery process for this service
    model.addQualifyingServiceParam(DISCOVERY_SERVICE_NAME, service.getName());
    model.addQualifyingServiceParam(DISCOVERY_SERVICE_DISPLAY_NAME, service.getDisplayName());

    return model;
  }

}