package io.urmia.cli.admin;

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

import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import org.apache.curator.x.discovery.ServiceInstance;
import io.urmia.naming.model.NodeType;
import io.urmia.naming.service.NamingService;

import java.util.LinkedList;
import java.util.List;

public class DefaultNamingAdminFacadeImpl implements NamingAdminFacade {

    private final NamingService ns;

    public DefaultNamingAdminFacadeImpl(NamingService ns) {
        this.ns = ns;
    }

    @Override
    public void apply(AdminCommand cmd) throws Exception {

        switch (cmd.getCommandType()) {
            case LIST:
                list(cmd);
                break;

            case ADD:
                add(cmd);
                break;

            case REMOVE:
                remove(cmd);
                break;

            default:
                throw new IllegalArgumentException("command not supported: " + cmd.getType());
        }
    }

    private static Joiner liner = Joiner.on("\n").skipNulls();

    private List<ServiceInstance<NodeType>> select(AdminCommand cmd) throws Exception {
        Predicate<ServiceInstance<NodeType>> p = cmd.predicate();
        Optional<NodeType> t = cmd.getTypeOptional();

        final List<ServiceInstance<NodeType>> l;

        if(t.isPresent())
            l = ns.list(t.get(), p);
        else {
            l = new LinkedList<ServiceInstance<NodeType>>();
            for(NodeType type : ns.queryTypes())
                l.addAll(ns.list(type, p));
        }

        return l;
    }

    private void list(AdminCommand cmd) throws Exception {
        List<ServiceInstance<NodeType>> l = select(cmd);
        System.out.println("result size: " + l.size());
        System.out.println(liner.join(l));
    }

    private void remove(AdminCommand cmd) throws Exception {
        List<ServiceInstance<NodeType>> l = select(cmd);
        System.out.println("result size: " + l.size());
        for(ServiceInstance<NodeType> hn : l) {
            System.out.println("removing node: " + hn);
            ns.remove(hn);
        }
    }

    private void add(AdminCommand cmd) throws Exception {
        ServiceInstance<NodeType> si = cmd.getInputAsSI(ns.getRegisteredOnHostCount(cmd.getType(), cmd.getHost()));

        System.out.println("adding node: " + si);
        ns.add(si);
    }

}
