package io.orchestrate.client;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Dog extends Animal {
    public Dog(@JsonProperty("name") String name) {
        super(name);
    }
}
