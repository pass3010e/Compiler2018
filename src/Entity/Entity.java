package Entity;

import AST.Location;
import Type.Type;

public class Entity {

    protected Location location;
    protected String name;
    protected Type type;

    public Entity(Location loc, Type type, String name) {

        this.location = loc;
        this.type = type;
        this.name = name;
    }

    public String name() {

        return name;
    }

    public Type type() {

        return type;
    }

    public Location location() {
        return location;
    }

}
