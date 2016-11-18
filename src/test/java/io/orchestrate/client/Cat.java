package io.orchestrate.client;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Cat extends Animal {
    private int lives;

    public Cat(@JsonProperty("name") String name, @JsonProperty("lives") int lives){
        super(name);
        this.lives = lives;
    }

    public int getLives() {
        return lives;
    }
}
