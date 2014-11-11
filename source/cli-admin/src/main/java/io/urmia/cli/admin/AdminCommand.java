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

import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import io.urmia.naming.service.RandomUuidImpl;
import io.urmia.util.StringUtils;
import io.urmia.naming.service.Uuid;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.ParseException;
import org.apache.curator.x.discovery.ServiceInstance;
import org.apache.curator.x.discovery.ServiceInstanceBuilder;
import org.apache.curator.x.discovery.ServiceType;
import io.urmia.naming.model.NodeType;
import org.apache.curator.x.discovery.UriSpec;

import java.net.Inet4Address;
import java.net.UnknownHostException;

public class AdminCommand {

    private final CommandLine line;
    private final CommandType command;
    private final static Uuid uuid = new RandomUuidImpl();

    public static enum CommandType {
        ADD,
        LIST,
        REMOVE
    }

    public AdminCommand(CommandLine line) {
        this.line = line;

        if (!line.hasOption("command"))
            throw new IllegalArgumentException("command is required.");

        String cmdOpt = line.getOptionValue("command");

        this.command = CommandType.valueOf(cmdOpt.toUpperCase());
    }

    public int getAvailabilityZone() {
        try {
            return line.hasOption("az") ? (Integer) line.getParsedOptionValue("az") : 1;
        } catch (ParseException e) {
            return 1;
        }
    }

    public CommandType getCommandType() {
        return command;
    }

    public Optional<NodeType> getTypeOptional() {
        try {
            return Optional.of(getType());
        } catch (IllegalArgumentException e) {
            return Optional.absent();
        }
    }

    public NodeType getType() {
        final String typeStr = line.getOptionValue("type", "").toUpperCase();
        try {
            return NodeType.valueOf(typeStr);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("not found type: " + typeStr);
        }
    }

    public String getHost() throws UnknownHostException {
        return line.hasOption("host") ? line.getOptionValue("host") : Inet4Address.getLocalHost().getHostName();
    }

    private int getPort(int registeredCount) {
        return line.hasOption("port") ? Integer.parseInt(line.getOptionValue("port")) : getType().defaultPort + registeredCount;
    }

    private String getId() {
        return line.hasOption("id") ? line.getOptionValue("id") : uuid.next();
    }

    private static final String DEFAULT_ZK_SERVER = "localhost:2181";

    public String getZkUrl() {
        return line.hasOption("zk") ? line.getOptionValue("zk") : DEFAULT_ZK_SERVER;
    }

    public Predicate<ServiceInstance<NodeType>> predicate() {

        return new Predicate<ServiceInstance<NodeType>>() {
            @Override
            public boolean apply(ServiceInstance<NodeType> input) {
                if (line.hasOption("host") && !input.getAddress().equals(line.getOptionValue("host")))
                    return false;

                if (line.hasOption("type") && !input.getPayload().equals(NodeType.valueOf(line.getOptionValue("type").toUpperCase())))
                    return false;

                if (line.hasOption("id") && !input.getId().equals(line.getOptionValue("id")))
                    return false;

                if (line.hasOption("port") && !line.getOptionValue("port").equals("" + input.getPort()))
                    return false;

                //noinspection RedundantIfStatement
                if (line.hasOption("uri")) {
                    if (input.getUriSpec() == null || input.getUriSpec().getParts() == null || input.getUriSpec().getParts().isEmpty())
                        return false;
                    String lineUri = line.getOptionValue("uri");
                    String inputUri = input.getUriSpec().build(); //getParts().get(0).getValue();
                    System.out.println("comparing inputUri = " + inputUri + ", lineUri: " + lineUri);
                    if(StringUtils.isBlank(inputUri)) return false;
                    if (!inputUri.contains(lineUri)) return false;
                }

                return true;
            }

            @Override
            public boolean equals(Object object) {
                return false;
            }
        };
    }

    public ServiceInstance<NodeType> getInputAsSI(int registeredCount) throws Exception {
        ServiceInstanceBuilder<NodeType> b = ServiceInstance.builder();

        b.serviceType(ServiceType.PERMANENT)
                .address(getHost())
                .name(getType().name())
                .id(getId())
                .payload(getType())
                .port(getPort(registeredCount));

        if(getType().uriRequired)
            if (!line.hasOption("uri"))
                throw new IllegalArgumentException("URI is required for type: " + getType());
            else
                b.uriSpec(new UriSpec(line.getOptionValue("uri")));

        return b.build();

    }

}
