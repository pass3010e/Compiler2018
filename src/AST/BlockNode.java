package AST;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import Entity.Entity;
import FrontEnd.ASTVisitor;
import IR.Operand.Operand;
import Scope.Scope;
import AST.Location;

public class BlockNode extends StmtNode {

    private List<StmtNode> stmts;

    private Scope scope;

    public BlockNode(Location loc, List<StmtNode> stmts) {

        super(loc);
        this.stmts = stmts;
    }

    public List<StmtNode> stmts() {

        return stmts;
    }

    public Scope scope() {

        return scope;
    }

    public void setScope(Scope scope) {

        this.scope = scope;
    }

    @Override
    public void accept(ASTVisitor visitor) {

        visitor.visit(this);
    }

    public static BlockNode toBlockNode(StmtNode node) {

        if(node == null) return null; //node
        if(node instanceof BlockNode) return (BlockNode)node;
        LinkedList<StmtNode> stmts = new LinkedList<StmtNode>();
        stmts.add(node);
        return new BlockNode(node.location(), stmts);
    }


    @Override public StmtNode Inline(HashMap<Entity, Operand> inlineMap) {

        return this;
    }


    @Override public BlockNode copy() {

        List<StmtNode> s = new LinkedList<>();
        BlockNode node = new BlockNode(this.location, s);
        for (StmtNode stmt : stmts()) s.add(stmt.copy());
        node.scope = scope;
        return node;
    }


}
