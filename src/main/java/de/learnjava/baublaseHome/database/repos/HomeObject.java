package de.learnjava.baublaseHome.database.repos;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class HomeObject {

    private String uuid;
    private String name;
    private String locationFromString;

    public HomeObject(String uuid, String name, String locationFromString) {
        this.uuid = uuid;
        this.name = name;
        this.locationFromString = locationFromString;
    }
}