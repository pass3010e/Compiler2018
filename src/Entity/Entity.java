package Entity;

import AST.Location;
import IR.Operand.Operand;
import Type.Type;

public class Entity {

    protected Location location;
    protected String name;
    protected Type type;
    private Operand pos;
    int now = 0;
    private boolean IsGlobal = false;
    private boolean irrelevant = true; // false

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

    public void setPos(Operand pos) {//private

        this.pos = pos;
    }

    public Operand pos() {

        return pos;
    }

    public void setName(String name) {

        this.name = name;
    }


    public void setValue() {

        ++now;
    }

    public int now() {

        return now;
    }

    public boolean isGlobal() {
        return IsGlobal;
    }

    public void setIsGlobal(boolean IsGlobal) {

        this.IsGlobal = IsGlobal;
    }

    public boolean isIrrelevant() {

        return irrelevant;
    }

    public void setIrrelevant(boolean irrelevant) {

        this.irrelevant = irrelevant;
    }
}
