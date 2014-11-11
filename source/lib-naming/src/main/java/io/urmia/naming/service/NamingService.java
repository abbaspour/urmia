package io.urmia.naming.service;

/**
 *
 * Copyright 2014 by Amin Abbaspour
 *
 * This file is part of Urmia.io
 *
 * Urmia.io is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Urmia.io is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Urmia.io.  If not, see <http://www.gnu.org/licenses/>.
 */

import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import org.apache.curator.x.discovery.ServiceInstance;
import org.apache.curator.x.discovery.ServiceInstanceBuilder;
import org.apache.curator.x.discovery.ServiceType;
import io.urmia.naming.model.NodeType;

import java.util.List;

/**
 * this is static data.
 * when you add/remove a node with cli-admin, it's found with this service.
 */
public interface NamingService {

    ServiceInstanceBuilder<NodeType> builder(NodeType type, ServiceType serviceType) throws Exception;

    // -- naming (static) --
    List<NodeType> queryTypes() throws Exception;

    ServiceInstance<NodeType> get(NodeType type, String id) throws Exception;

    List<ServiceInstance<NodeType>> list(NodeType type, Predicate<ServiceInstance<NodeType>> p) throws Exception;

    int getRegisteredOnHostCount(NodeType type, String host) throws Exception;

    void remove(ServiceInstance<NodeType> si) throws Exception;

    void add(ServiceInstance<NodeType> si) throws Exception;

    // -- discovery (dynamic) --
    ServiceInstance<NodeType> discover(NodeType type, String id) throws Exception;

    List<ServiceInstance<NodeType>> suggestStorage(int durability) throws Exception; // todo: should throw NotEnoughNodes

    void register(ServiceInstance<NodeType> si) throws Exception;

    void deregister(ServiceInstance<NodeType> node) throws Exception;


    // -- mixed --

    /**
     * detect best guess of who am i based on:
     * - input NodeType
     * - hostname
     * - existing nodes in discovery
     * - does registration if all good (?)
     * @return optional instance of ServiceInstance
     */
    Optional<ServiceInstance<NodeType>> whoAmI(NodeType t, boolean autoRegister) throws Exception;

    int getRunningCount(ServiceInstance si) throws Exception;

    Optional<ServiceInstance<NodeType>> getOfType(NodeType t, String host, int order) throws Exception;

    //boolean isUp(/*ServiceInstance<NodeType> si*/String name, String id) throws Exception;


}
