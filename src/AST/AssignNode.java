package AST;

import FrontEnd.ASTVisitor;
import Type.Type;
import Error.SemanticError;

public class AssignNode extends ExprNode{

    private ExprNode lhs, rhs;

    public AssignNode(ExprNode lhs, ExprNode rhs) {

        super();
        this.lhs = lhs;
        this.rhs = rhs;
        if(lhs instanceof VariableNode && ((VariableNode)lhs).name().equals("this")) throw new SemanticError(new Location(0,0), "This");
    }

    public ExprNode lhs() {

        return lhs;
    }

    public void setLhs(ExprNode lhs) {

        this.lhs = lhs;
    }

    public ExprNode rhs() {

        return rhs;
    }

    public void setRhs(ExprNode rhs) {

        this.rhs = rhs;
    }

    @Override
    public Type type() {

        return lhs.type();
    }

    @Override
    public Location location() {

        return lhs.location();
    }

    @Override
    public void accept(ASTVisitor visitor) {

        visitor.visit(this);
    }

}
