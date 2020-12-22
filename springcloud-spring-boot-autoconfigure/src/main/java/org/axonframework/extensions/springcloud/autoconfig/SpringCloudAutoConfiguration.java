/*
 * Copyright (c) 2010-2020. Axon Framework
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.axonframework.extensions.springcloud.autoconfig;

import org.axonframework.commandhandling.CommandBus;
import org.axonframework.commandhandling.distributed.CommandBusConnector;
import org.axonframework.commandhandling.distributed.CommandRouter;
import org.axonframework.commandhandling.distributed.DistributedCommandBus;
import org.axonframework.commandhandling.distributed.RoutingStrategy;
import org.axonframework.common.AxonConfigurationException;
import org.axonframework.extensions.springcloud.DistributedCommandBusProperties;
import org.axonframework.extensions.springcloud.commandhandling.SpringCloudCommandRouter;
import org.axonframework.extensions.springcloud.commandhandling.SpringHttpCommandBusConnector;
import org.axonframework.extensions.springcloud.commandhandling.mode.CapabilityDiscoveryMode;
import org.axonframework.extensions.springcloud.commandhandling.mode.RestCapabilityDiscoveryMode;
import org.axonframework.extensions.springcloud.commandhandling.mode.SimpleCapabilityDiscoveryMode;
import org.axonframework.serialization.Serializer;
import org.axonframework.springboot.autoconfig.InfraConfiguration;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.client.discovery.simple.SimpleDiscoveryClientAutoConfiguration;
import org.springframework.cloud.client.serviceregistry.Registration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.web.client.RestTemplate;

/**
 * Auto configuration class for the defining a {@link SpringCloudCommandRouter} and {@link
 * SpringHttpCommandBusConnector} to be used in a {@link DistributedCommandBus}.
 *
 * @author Steven van Beelen
 * @since 3.0
 */
@Configuration
@AutoConfigureAfter({
        RoutingStrategyAutoConfiguration.class,
        SimpleDiscoveryClientAutoConfiguration.class
})
@AutoConfigureBefore(InfraConfiguration.class)
@EnableConfigurationProperties(DistributedCommandBusProperties.class)
@ConditionalOnProperty("axon.distributed.enabled")
@ConditionalOnClass(name = {
        "org.axonframework.extensions.springcloud.commandhandling.SpringCloudCommandRouter",
        "org.axonframework.extensions.springcloud.commandhandling.SpringHttpCommandBusConnector",
        "org.springframework.cloud.client.discovery.DiscoveryClient",
        "org.springframework.web.client.RestTemplate"
})
public class SpringCloudAutoConfiguration {

    private final DistributedCommandBusProperties properties;
    private final DistributedCommandBusProperties.SpringCloudProperties springCloudProperties;

    public SpringCloudAutoConfiguration(DistributedCommandBusProperties properties) {
        this.properties = properties;
        this.springCloudProperties = properties.getSpringCloud();
    }

    @Bean
    @ConditionalOnMissingBean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Bean
    @ConditionalOnMissingBean
    public CapabilityDiscoveryMode capabilityDiscoveryMode(Serializer serializer, RestTemplate restTemplate) {
        switch (springCloudProperties.getMode()) {
            case REST:
                return buildRestCapabilityDiscoveryMode(
                        RestCapabilityDiscoveryMode.builder(), serializer, restTemplate
                );
            case SIMPLE:
                return buildRestCapabilityDiscoveryMode(
                        SimpleCapabilityDiscoveryMode.builder(), serializer, restTemplate
                );
            default:
                throw new AxonConfigurationException(
                        "The configured mode [" + springCloudProperties.getMode() + "] is not supported"
                );
        }
    }

    private CapabilityDiscoveryMode buildRestCapabilityDiscoveryMode(RestCapabilityDiscoveryMode.Builder builder,
                                                                     Serializer serializer,
                                                                     RestTemplate restTemplate) {
        builder.serializer(serializer)
               .restTemplate(restTemplate)
               .messageCapabilitiesEndpoint(springCloudProperties.getRestModeUrl());
        if (springCloudProperties.shouldDisableIgnoreListing()) {
            builder.disableIgnoreListing();
        }
        return builder.build();
    }

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean(DiscoveryClient.class)
    public CommandRouter springCloudCommandRouter(DiscoveryClient discoveryClient,
                                                  Registration localServiceInstance,
                                                  RoutingStrategy routingStrategy,
                                                  CapabilityDiscoveryMode capabilityDiscoveryMode,
                                                  Serializer serializer) {
        return SpringCloudCommandRouter.builder()
                                       .discoveryClient(discoveryClient)
                                       .localServiceInstance(localServiceInstance)
                                       .routingStrategy(routingStrategy)
                                       .capabilityDiscoveryMode(capabilityDiscoveryMode)
                                       .serializer(serializer)
                                       .build();
    }

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Bean
    @ConditionalOnMissingBean(CommandBusConnector.class)
    public SpringHttpCommandBusConnector springHttpCommandBusConnector(
            @Qualifier("localSegment") CommandBus localSegment,
            RestTemplate restTemplate,
            @Qualifier("messageSerializer") Serializer serializer
    ) {
        return SpringHttpCommandBusConnector.builder()
                                            .localCommandBus(localSegment)
                                            .restOperations(restTemplate)
                                            .serializer(serializer)
                                            .build();
    }

    @Bean
    @Primary
    @ConditionalOnBean(CommandBusConnector.class)
    @ConditionalOnMissingBean
    public DistributedCommandBus distributedCommandBus(CommandRouter commandRouter,
                                                       CommandBusConnector commandBusConnector) {
        DistributedCommandBus commandBus = DistributedCommandBus.builder()
                                                                .commandRouter(commandRouter)
                                                                .connector(commandBusConnector)
                                                                .build();
        commandBus.updateLoadFactor(properties.getLoadFactor());
        return commandBus;
    }
}
