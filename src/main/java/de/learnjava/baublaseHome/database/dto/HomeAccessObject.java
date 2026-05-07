package de.learnjava.baublaseHome.database.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class HomeAccessObject {

    private String ownerUuid;
    private String homeName;
    private String allowedUuid;

    public HomeAccessObject(String ownerUuid, String homeName, String allowedUuid) {
        this.ownerUuid   = ownerUuid;
        this.homeName    = homeName;
        this.allowedUuid = allowedUuid;
    }
}

