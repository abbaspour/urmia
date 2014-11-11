package io.urmia.jail.docker;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.core.DockerClientImpl;

public class TestDockerRun {
    public static void main(String[] args) {
        DockerClient client = new DockerClientImpl("http://192.168.59.103:2375");
        System.out.println("info = " + client.infoCmd().exec());
    }
}
