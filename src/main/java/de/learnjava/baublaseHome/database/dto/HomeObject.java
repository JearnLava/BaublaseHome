package de.learnjava.baublaseHome.database.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.Locale;

@Getter
@Setter
public class HomeObject {

    private String uuid;
    private String homeName;
    private String world;
    private Double x;
    private Double y;
    private Double z;
    private Float  yaw;
    private Float  pitch;

    public HomeObject(String uuid, String homeName, String world,
                      double x, double y, double z, float yaw, float pitch) {
        this.uuid     = uuid;
        this.homeName = homeName;
        this.world    = world;
        this.x        = x;
        this.y        = y;
        this.z        = z;
        this.yaw      = yaw;
        this.pitch    = pitch;
    }

    public HomeObject(String uuid, String homeName, String locationString) {
        this.uuid     = uuid;
        this.homeName = homeName;
        String[] parts = locationString.replace(",", ".").split(":");
        this.world = parts[0];
        this.x     = Double.parseDouble(parts[1]);
        this.y     = Double.parseDouble(parts[2]);
        this.z     = Double.parseDouble(parts[3]);
        this.yaw   = Float.parseFloat(parts[4]);
        this.pitch = Float.parseFloat(parts[5]);
    }

    public String getLocationFromString() {
        return String.format(Locale.US, "%s:%.6f:%.6f:%.6f:%.4f:%.4f",
                world, x, y, z, yaw, pitch);
    }
}