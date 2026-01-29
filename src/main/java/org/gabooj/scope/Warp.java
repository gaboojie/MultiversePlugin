package org.gabooj.scope;

public class Warp {

    public double x, y, z;
    public float yaw, pitch;
    public final String worldID, name;

    public Warp(String name, double x, double y, double z, float yaw, float pitch, String worldID) {
        this.name = name;
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.pitch = pitch;
        this.worldID = worldID;

        assert name != null;
        assert worldID != null;
    }
}