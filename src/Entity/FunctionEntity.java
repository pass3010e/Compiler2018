package Entity;

import java.util.List;

import AST.BlockNode;
import Type.Type;
import Type.FunctionType;
import AST.Location;
import Scope.Scope;

public class FunctionEntity extends Entity{

    private Type returnType;
    private List<Entity> varList; //Parameter items tparams
    private BlockNode body;
    private Scope scope;

    private boolean isConstructor = false;

    public FunctionEntity(Location loc, Type returnType, String name, List<Entity> varList, BlockNode body) {
        super(loc, new FunctionType(name), name);
        this.varList = varList;
        this.body = body;
        this.returnType = returnType;
        ((FunctionType)this.type).setEntity(this);
    }

    public Entity addThisPointer(Location loc, ClassEntity entity) {
        Entity thisPointer = new Entity(loc, entity.type(), "this");
        varList.add(0, thisPointer);
        return thisPointer;
    }

    @Override
    public String name() {

        return name;
    }

    @Override
    public String toString() {

        return "function entity : " + name;
    }

    public List<Entity> varList() {

        return varList;
    }

    public BlockNode body() {

        return body;
    }

    public Scope scope() {

        return scope;
    }

    public void setScope(Scope scope) {

        this.scope = scope;
    }

    public Type returnType() {

        return returnType;
    }

    public boolean isConstructor() {

        return isConstructor;
    }

    public void setIsConstructor(boolean constructor) {

        isConstructor = constructor;
    }

}
