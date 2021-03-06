package backend;

import FrontEnd.AST;
import AST.*;
import Entity.*;
import Optim.*;
import FrontEnd.Visitor;
import IR.*;
import IR.Label;
import IR.Operand.*;
import Type.*;


import java.util.*;
import java.util.List;

import static IR.Binary.BinaryOp.*;

public class IRBuilder extends Visitor {

    private AST ast;
    private IR Ir = new IR();
    private FunctionEntity currentFunction = null;
    private Label continueLabel, breakLabel, returnLabel;
    private HashMap<String, Operand> history = new HashMap<>();
    private boolean setMode = false;
    private List<FunctionEntity> Record = new ArrayList<>();
    Operand Rc;
    Operand Rd;
    Operand Rs;
    private boolean isRec = false;
    private boolean isIrr = true;
    private FunctionEntity nowInline;
    private int inlineSize;
    private HashMap<Entity, Entity> Tran = new HashMap<>();

    private FunctionEntity malloc, Str_ADD, Str_EQ, Str_NE, Str_LT, Str_GT, Str_LE, Str_GE;

    public IRBuilder(AST ast) {

        this.ast = ast;
        malloc = (FunctionEntity) ast.scope().searchCurrently("malloc");
        Str_ADD = (FunctionEntity) ast.scope().searchCurrently("Str_ADD");
        Str_EQ = (FunctionEntity) ast.scope().searchCurrently("Str_EQ");
        Str_NE = (FunctionEntity) ast.scope().searchCurrently("Str_NE");
        Str_LT = (FunctionEntity) ast.scope().searchCurrently("Str_LT");
        Str_GT = (FunctionEntity) ast.scope().searchCurrently("Str_GT");
        Str_LE = (FunctionEntity) ast.scope().searchCurrently("Str_LE");
        Str_GE = (FunctionEntity) ast.scope().searchCurrently("Str_GE");
        Ir.Record = Record;
    }

    public IR generateIR() {
        Ir.ast = ast;
        for (ClassEntity entity : ast.classEntities()) entity.setOffset();
        for (VariableEntity entity : ast.variableEntities()) {
            //if(entity.type() instanceof StringType) continue;
            Ir.globalInitializer.add(entity);
            entity.setIsGlobal(true);
            //if(entity.type() instanceof StringType) entity.setPos(new GlobalAddr(entity.name() + "__", true));
            //else entity.setPos(new GlobalAddr(entity.name() + "__", false));
            //bugs , but I don't know why
            entity.setPos(new GlobalAddr(entity.name() + "__", false)); // true
        }

        if(ast.variableEntities().size() > 20 && ast.functionEntities().size() == 1) {

            for (VariableEntity entity : ast.variableEntities()) {
                if(entity.type() instanceof IntType) entity.setIsGlobal(false); // entity.Expr() == null &&
            }

        }

        if(ast.functionEntities().size() >= 9) inlineSize = 2;
        else inlineSize = 8;

        for (FunctionEntity entity : ast.functionEntities()) entity.checkInlinable(inlineSize);//

        if(ast.classEntities().size() > 0) isIrr = false;

        if(ast.variableEntities().size() > 0) isIrr = false;

        if(isIrr) {

            IrrelevantMaker maker = new IrrelevantMaker();
            maker.check(ast);

        }

        for (FunctionEntity entity : ast.functionEntities()) {

            FunctionRecorder recorder = new FunctionRecorder(entity);
            boolean f = recorder.check();
            entity.Rec = f;
            if(f) Record.add(entity);

        }

        for (FunctionEntity entity : ast.functionEntities()) {
            currentFunction = entity;
            compileFunction(entity);
        }

        for (ClassEntity entity : ast.classEntities()) {
            for (FunctionDefinitionNode node : entity.memberFuncs()) {
                currentFunction = node.entity();
                compileFunction(node.entity());
            }
        }

        return Ir;
    }

    private void compileFunction(FunctionEntity entity) {

        Label begin = new Label(entity.name());
        Label end = new Label();
        entity.setLabel(begin, end);
        currentFunction = entity;
        for(int i = 0; i < entity.varList().size(); ++i) {
            Operand s;
            if(i < 6) {
                Entity t = (Entity)entity.varList().get(i);
                t.setPos(currentFunction.newReg());
                s = PhiReg.getParameterReg(i);
                currentFunction.addIns(new Assign(t.pos(), s));
            } else {
                Entity t = entity.varList().get(i);
                t.setPos(new Mem(PhiReg.rbp, null, 0, 16 + (i - 6) * 8));
            }
        }

        Label R = new Label();
        Label A = new Label();
        Rc = currentFunction.newReg();
        Rd = currentFunction.newReg();
        Rs = currentFunction.newReg();

        if(entity.Rec) {
            currentFunction.addIns(new Cjump(PhiReg.getParameterReg(0), new Imm(0),  Cjump.Type.LT, A));// LE
            currentFunction.addIns(new Cjump(PhiReg.getParameterReg(0), new Imm(150),  Cjump.Type.GE, A));// le LE LT // GT
            currentFunction.addIns(new Assign(Rs, PhiReg.getParameterReg(0)));//1
            currentFunction.addIns(new Assign(Rd, new GlobalAddr(entity.name() + "__", true)));//false
            currentFunction.addIns(new Assign(Rc, new Mem((Reg)Rd, (Reg)Rs, 8, 0)));
            currentFunction.addIns(new Cjump(Rc, new Imm(0),  Cjump.Type.GT, R));
            currentFunction.addIns(new Jump(A));
            currentFunction.addIns(R);
            currentFunction.addIns(new Assign(PhiReg.rax, Rc));
            currentFunction.addIns(new Jump(end));
            currentFunction.addIns(A);
        }/**/

        if (entity.name().equals("main")) {
            for (DefinitionNode node : ast.definitionNodes()) if (node instanceof VariableDefinitionNode) visit((VariableDefinitionNode) node);

        }
        if(entity.Rec) isRec = true;//
        visit(entity.body());
        if(entity.Rec) isRec = false;//
        if (currentFunction.insList().size() == 0 || !(currentFunction.insList().get(currentFunction.insList().size() - 1) instanceof Jump)) {  // add return
            currentFunction.addIns(new Jump(end));
        }
        currentFunction.addIns(end);
        clear();
    }

    public void visitStmt(StmtNode s) {

        clear();//
        s.accept(this);
    }

    public void visitExpr(ExprNode e) {

        if(e instanceof BinaryOpNode || e instanceof LogicalAndNode || e instanceof LogicalOrNode) {

            if(inlineMode == 0 && history.containsKey(e.hash())) {

                e.setOperand(history.get(e.hash()));//
                System.err.println(e.hash());//
                return;//
            }
        }

        e.accept(this);
    }

    @Override public void visit(VariableDefinitionNode node) {

        if(inlineMode > 0) {
            VariableEntity entity = (VariableEntity) inlineMap.peek().get(node.entity());
            entity = entity == null ? node.entity() : entity;
            node.entity = entity;
        }/**/
        ExprNode init = node.entity().Expr();
        if (init != null) {
            visitExpr(init);
        }
        VariableEntity t = node.entity();
        if (!t.isGlobal()) t.setPos(currentFunction.newReg());
        if (init != null) currentFunction.addIns(new Assign(t.pos(), init.operand()));
        else {
            if(t.type() instanceof ArrayType || t.type() instanceof ClassType) {
                currentFunction.addIns(new Assign(t.pos(), new Imm(0)));
            }
        }
    }

    public void visit(BlockNode node) {

        if (inlineMode > 0) {

            Map<Entity, Entity> map = inlineMap.peek();

            for (Entity entity : node.scope().entities().values()) {
                if (entity instanceof VariableEntity) {
                    VariableEntity clone = ((VariableEntity) entity).copy();
                    map.put(entity, clone);
                }
            }
        }
        for (StmtNode stmt : node.stmts()) stmt.accept(this);
    }

    public void visit(AssignNode node) {


        BinaryChecker checker = new BinaryChecker();

        if(node.rhs() instanceof BinaryOpNode && node.rhs().hash().length() >= 50 && checker.check((BinaryOpNode) node.rhs())) { // 20

            BinaryIniter initer = new BinaryIniter();

            BinaryOpNode rhs = initer.init((BinaryOpNode) node.rhs());

            visitExpr(node.lhs());
            visitExpr(rhs);
            if(node.lhs() instanceof VariableNode) {

                (((VariableNode)node.lhs()).entity()).setValue();//(VariableEntity)
            }
            else clear();
            currentFunction.addIns(new Assign(node.lhs().operand(), rhs.operand()));
            node.setOperand(null);
            return;

        }


        if(isIrr && (inlineMode == 0)) {

            VariableNode n = deepNode(node.lhs());
            if(n.entity().isIrrelevant()){
                System.err.println(node.lhs().hash() + " Is Irrevent");
                return;
            }
        }

        visitExpr(node.lhs());
        visitExpr(node.rhs());
        if(node.lhs() instanceof VariableNode) {

            (((VariableNode)node.lhs()).entity()).setValue();//(VariableEntity)
        }
        else clear();
        currentFunction.addIns(new Assign(node.lhs().operand(), node.rhs().operand()));
        node.setOperand(null);
    }

    @Override public void visit(IfNode node) {

        if(node.elseBody() == null && node.thenBody() != null && ((BlockNode)node.thenBody()).stmts() != null && ((BlockNode)node.thenBody()).stmts().size() == 1 && ((BlockNode)node.thenBody()).stmts().get(0) instanceof ExprStmtNode && ((ExprStmtNode)((BlockNode)node.thenBody()).stmts().get(0)).expr() instanceof UnaryOpNode) {

            UnaryOpNode u = (UnaryOpNode) (((ExprStmtNode)((BlockNode)node.thenBody()).stmts().get(0)).expr());
            if(u.operator() == UnaryOpNode.UnaryOp.SUF_INC || u.operator() == UnaryOpNode.UnaryOp.PRE_INC) {
                BinaryOpNode t = new BinaryOpNode(BinaryOpNode.BinaryOp.ADD, u.expr(), node.cond());
                AssignNode a = new AssignNode(u.expr(), t);
                visit(a);
                return;
            }
            else if(u.operator() == UnaryOpNode.UnaryOp.SUF_DEC || u.operator() == UnaryOpNode.UnaryOp.PRE_DEC) {
                BinaryOpNode t = new BinaryOpNode(BinaryOpNode.BinaryOp.SUB, u.expr(), node.cond());
                AssignNode a = new AssignNode(u.expr(), t);
                visit(a);
                return;
            }

        }

        visitExpr(node.cond());
        Label thenLabel = new Label();
        Label elseLabel = new Label();
        Label endLabel  = new Label();
        currentFunction.addIns(new Cjump(node.cond().operand(), new Imm(1), Cjump.Type.GE, thenLabel));
        currentFunction.addIns(new Jump(elseLabel));
        currentFunction.addIns(thenLabel);
        if (node.thenBody() != null) visitStmt(node.thenBody());
        currentFunction.addIns(new Jump(endLabel));
        currentFunction.addIns(elseLabel);
        if (node.elseBody() != null) visitStmt(node.elseBody());
        currentFunction.addIns(endLabel);
    }

    private void visitLoop(ExprNode init, ExprNode cond, ExprNode incr, StmtNode body) {

        Label lastContinueLabel = continueLabel;
        Label lastBreakLabel = breakLabel;
        Label startLabel = new Label();
        Label trueLabel = new Label();
        continueLabel = new Label();
        breakLabel = new Label();
        if (init != null) visitExpr(init);
        currentFunction.addIns(startLabel);
        if (cond != null) {
            visitExpr(cond);
            currentFunction.addIns(new Cjump(cond.operand(), new Imm(1),Cjump.Type.GE, trueLabel));
            currentFunction.addIns(new Jump(breakLabel));
        }
        else currentFunction.addIns(new Jump(trueLabel));
        currentFunction.addIns(trueLabel);
        if (body != null) visitStmt(body);
        currentFunction.addIns(continueLabel);
        if (incr != null) visitExpr(incr);
        currentFunction.addIns(new Jump(startLabel));
        currentFunction.addIns(breakLabel);
        continueLabel = lastContinueLabel;
        breakLabel = lastBreakLabel;
    }

    @Override public void visit(WhileNode node) {

        visitLoop(null, node.cond(), null, node.body());
    }

    @Override public void visit(ForNode node) {

        if(node.body() == null || node.body().stmts().size() == 0) return; // .stmts()
        visitLoop(node.init(), node.cond(), node.incr(), node.body());
    }

    @Override public void visit(ContinueNode node) {

        currentFunction.addIns(new Jump(continueLabel));
    }

    @Override public void visit(BreakNode node) {

        currentFunction.addIns(new Jump(breakLabel));
    }

    @Override public void visit(ReturnNode node) {

        if (inlineMode > 0) {
            if(node.expr() != null) {
                visitExpr(node.expr());
                currentFunction.addIns(new Assign(inlineReturnPos.peek(), node.expr().operand()));
            }
            if(inlineReturnLabel.peek() != null) currentFunction.addIns(new Jump(inlineReturnLabel.peek())); //
            return;
        }

        if(node.expr() != null) {
            visitExpr(node.expr());
            currentFunction.addIns(new Assign(PhiReg.rax, node.expr().operand()));
            if(isRec) {
                Label A = new Label();
                currentFunction.addIns(new Cjump(Rs, new Imm(0),  Cjump.Type.LT, A));
                currentFunction.addIns(new Cjump(Rs, new Imm(150),  Cjump.Type.GE, A));
                currentFunction.addIns(new Assign(new Mem((Reg)Rd, (Reg)Rs, 8, 0), PhiReg.rax));
                currentFunction.addIns(A);
            }/**/
        }
        currentFunction.addIns(new Jump(currentFunction.endLabel()));
    }

    @Override public void visit(ExprStmtNode node) {

        visitExpr(node.expr());
    }

    @Override public void visit(IntegerLiteralNode node) {

        node.setOperand(new Imm(node.value()));
    }

    @Override public void visit(StringLiteralNode node) {

        node.setOperand(Ir.add((StringConstantEntity)node.entity()));
    }

    @Override public void visit(BoolLiteralNode node) {

        node.setOperand(new Imm(node.value() ? 1 : 0));
    }


    @Override public void visit(BinaryOpNode node) {

        Binary.BinaryOp op;
        switch(node.operator()) {
            case ADD: op = ADD; break;
            case SUB: op = SUB; break;
            case MUL: op = MUL; break;
            case DIV: op = DIV; break;
            case MOD: op = MOD; break;
            case LSHIFT:  op = LSHIFT;  break;
            case RSHIFT:  op = RSHIFT;  break;
            case B_AND: op = B_AND; break;
            case B_XOR: op = B_XOR; break;
            case B_OR:  op = B_OR;  break;
            case L_AND: op = L_AND; break;
            case L_OR:  op = L_OR;  break;
            case GT: op = GT; break;
            case LT: op = LT; break;
            case GE: op = GE; break;
            case LE: op = LE; break;
            case EQ: op = EQ; break;
            case NE: op = NE; break;
            default: throw new Error();
        }
        node.setOperand(currentFunction.newReg());

        visitExpr(node.left());
        visitExpr(node.right());

        if (node.left().operand() instanceof Imm && node.right().operand() instanceof Imm && (((Imm)node.right().operand()).value() != 0)) { // (inlineMode == 0 || (nowInline.body().stmts().size() == 1 && node.operator() != BinaryOpNode.BinaryOp.DIV)) &&  && ((node.operator() != BinaryOpNode.BinaryOp.MOD && node.operator() != BinaryOpNode.BinaryOp.DIV) || ((Imm)node.right().operand()).value() != 0)

            long lvalue = (((Imm)(node.left().operand())).value()), rvalue = ((Imm)node.right().operand()).value();
            long value;

            switch (node.operator()) {
                case ADD: value = (lvalue + rvalue);break;
                case SUB: value = (lvalue - rvalue);break;
                case MUL: value = (lvalue * rvalue);break;
                case DIV: value = (lvalue / rvalue);break;
                case MOD: value = (lvalue % rvalue);break;
                case LSHIFT:  value = (lvalue << rvalue);break;
                case RSHIFT:  value = (lvalue >> rvalue);break;
                case B_AND: value = (lvalue & rvalue);break;
                case B_XOR: value = (lvalue ^ rvalue);break;
                case B_OR: value = (lvalue | rvalue);break;
                case L_AND: value = (lvalue != 0 && rvalue != 0) ? 1 : 0;break;//op = L_AND;
                case L_OR:  value = (lvalue != 0 && rvalue != 0) ? 1 : 0;break;//op = L_OR;
                case GT: value = (lvalue >  rvalue ? 1 : 0);break;
                case LT: value = (lvalue <  rvalue ? 1 : 0);break;
                case GE: value = (lvalue >= rvalue ? 1 : 0);break;
                case LE: value = (lvalue <= rvalue ? 1 : 0);break;
                case EQ: value = (lvalue == rvalue ? 1 : 0);break;
                case NE: value = (lvalue != rvalue ? 1 : 0);break;
                default: throw new Error();
            }

            node.setOperand(new Imm(value));
            return;
        }/**/

        if (node.left().type().isString()) {
            List<Operand> args = new ArrayList<Operand>();
            args.add(node.left().operand());
            args.add(node.right().operand());
            switch(node.operator()) {
                case ADD : currentFunction.addIns(new Call(Str_ADD, args, node.operand())); break;
                case EQ : currentFunction.addIns(new Call(Str_EQ, args, node.operand())); break;
                case NE : currentFunction.addIns(new Call(Str_NE, args, node.operand())); break;
                case GT : currentFunction.addIns(new Call(Str_GT, args, node.operand())); break;
                case LT : currentFunction.addIns(new Call(Str_LT, args, node.operand())); break;
                case LE : currentFunction.addIns(new Call(Str_LE, args, node.operand())); break;
                case GE : currentFunction.addIns(new Call(Str_GE, args, node.operand())); break;
                default : throw new Error();
            }
        }
        else currentFunction.addIns(new Binary(node.operand(), op, node.left().operand(), node.right().operand()));
        if(!setMode && !node.left().type().isString()) history.put(node.hash(), node.operand());
    }

    @Override public void visit(LogicalAndNode node) {

        LogicalChecker checker = new LogicalChecker();
        if(checker.check(node)) {

            LogicalAndChecker A = new LogicalAndChecker();
            if(A.check(node) && A.whole().size() >= 3) {
                //setMode = true;
                List<BinaryOpNode> whole = A.whole();
                setAnd(whole, node);
                //setMode = false;
                if(!setMode) history.put(node.hash(), node.operand());
                return;
            }

            visit((BinaryOpNode) node);
            return;
        }

        if (node.left().operand() instanceof Imm && node.right().operand() instanceof Imm) {
            long lvalue = (((Imm)(node.left().operand())).value()), rvalue = ((Imm)node.right().operand()).value();
            long value;
            value = (lvalue != 0 && rvalue != 0) ? 1 : 0;
            node.setOperand(new Imm(value));
            return;
        }
        node.setOperand(currentFunction.newReg());
        visitExpr(node.left());
        Label FaiLabel = new Label();
        Label OutLabel = new Label();
        currentFunction.addIns(new Cjump(node.left().operand(), new Imm(1), Cjump.Type.GE, FaiLabel));
        currentFunction.addIns(new Assign(node.operand(), new Imm(0)));
        currentFunction.addIns(new Jump(OutLabel));
        currentFunction.addIns(FaiLabel);
        setMode = true;
        visitExpr(node.right());
        setMode = false;
        currentFunction.addIns(new Assign(node.operand(), node.right().operand()));
        currentFunction.addIns(OutLabel);
        if(!setMode) history.put(node.hash(), node.operand());
    }

    @Override public void visit(LogicalOrNode node) {


        /*LogicalChecker checker = new LogicalChecker();
        if(checker.check(node)) {
            visit((BinaryOpNode) node);
            return;
        }*/

        if (node.left().operand() instanceof Imm && node.right().operand() instanceof Imm) {
            long lvalue = (((Imm)(node.left().operand())).value()), rvalue = ((Imm)node.right().operand()).value();
            long value;
            value = (lvalue != 0|| rvalue != 0) ? 1 : 0;
            node.setOperand(new Imm(value));
            return;
        }
        node.setOperand(currentFunction.newReg());
        visitExpr(node.left());
        Label SucLabel = new Label();
        Label OutLabel = new Label();
        currentFunction.addIns(new Cjump(node.left().operand(), new Imm(1), Cjump.Type.GE, SucLabel));
        setMode = true;
        visitExpr(node.right());
        setMode = false;
        currentFunction.addIns(new Assign(node.operand(), node.right().operand()));
        currentFunction.addIns(new Jump(OutLabel));
        currentFunction.addIns(SucLabel);
        currentFunction.addIns(new Assign(node.operand(), new Imm(1)));
        currentFunction.addIns(OutLabel);
        if(!setMode) history.put(node.hash(), node.operand());
    }


    private void setAnd(List<BinaryOpNode> whole, LogicalAndNode node) {

        node.setOperand(currentFunction.newReg());
        int size = whole.size();
        Label OutLabel = new Label();
        for(int i = 0; i < size - 1; ++i) {

            BinaryOpNode t = whole.get(i);
            if(t.left() instanceof VariableNode && t.right() instanceof VariableNode) {

                visitExpr(t.left());
                visitExpr(t.right());

                Cjump.Type op;

                switch (t.operator()) {

                    case LT: op = Cjump.Type.GE; break;
                    case LE: op = Cjump.Type.GT; break;
                    case GT: op = Cjump.Type.LE; break;
                    case GE: op = Cjump.Type.LT; break;
                    case EQ: op = Cjump.Type.NE; break;
                    case NE: op = Cjump.Type.EQ; break;
                    default: throw new Error();
                }

            currentFunction.addIns(new Cjump(t.left().operand(), t.right().operand(), op, OutLabel));
            }/**/
            else {
                setMode = true;
                visitExpr(t);
                setMode = false;
                currentFunction.addIns(new Cjump(t.operand(), new Imm(1), Cjump.Type.LT, OutLabel));
            }
        }

        BinaryOpNode t = whole.get(size - 1);
        setMode = true; //
        visitExpr(t);
        setMode = false; //
        Label FaiLabel = new Label();
        currentFunction.addIns(new Assign(node.operand(), t.operand()));
        currentFunction.addIns(new Jump(FaiLabel));//.right()
        currentFunction.addIns(OutLabel);
        currentFunction.addIns(new Assign(node.operand(), new Imm(0)));
        currentFunction.addIns(FaiLabel);

    }

    @Override public void visit(PrefixOpNode node) {

        visitExpr(node.expr());
        node.setOperand(node.expr().operand());
        clear();
        switch (node.operator()) {
            case PRE_DEC :
                currentFunction.addIns(new Binary(node.operand(), Binary.BinaryOp.SUB, node.operand(), new Imm(1)));
                break;
            case PRE_INC :
                currentFunction.addIns(new Binary(node.operand(), Binary.BinaryOp.ADD, node.operand(), new Imm(1)));
                break;
            default: {
                visit((UnaryOpNode)node);
                // System.out.println(node.operator());
                // throw new Error();
            }
        }
    }

    @Override public void visit(SuffixOpNode node) {

        visitExpr(node.expr());
        node.setOperand(currentFunction.newReg());
        clear();
        currentFunction.addIns(new Assign(node.operand(), node.expr().operand()));
        switch (node.operator()) {
            case SUF_DEC :
                currentFunction.addIns(new Binary(node.expr().operand(), Binary.BinaryOp.SUB, node.expr().operand(), new Imm(1)));
                break;
            case SUF_INC :
                currentFunction.addIns(new Binary(node.expr().operand(), Binary.BinaryOp.ADD, node.expr().operand(), new Imm(1)));
                break;
            default: throw new Error();
        }
    }

    @Override public void visit(UnaryOpNode node) {

        Unary.UnaryOp op;

        switch (node.operator()) {

            case MINUS : op = Unary.UnaryOp.MINUS; break;
            case B_NOT : op = Unary.UnaryOp.B_NOT; break;
            case L_NOT : op = Unary.UnaryOp.L_NOT; break;
            default : throw new Error();
        }
        node.setOperand(currentFunction.newReg());
        currentFunction.addIns(new Assign(node.operand(), node.expr().operand()));
        currentFunction.addIns(new Unary(node.operand(), op));
    }

    @Override public void visit(FuncallNode node) {

        FunctionEntity entity = node.functionType().entity();
        if(entity.name().equals("length") || entity.name().equals("size")) {
            visitExpr(node.varList().get(0));
            Reg t = toReg(node.varList().get(0).operand());
            node.setOperand(new Mem(t, null, 0, -8));
            return ;
        }
        node.setLabel(new Label(entity.name()));
        List<Operand> args = new ArrayList<>();
        for (ExprNode exprNode : node.varList()) {
            visitExpr(exprNode);
            args.add(exprNode.operand());
        }

        boolean f = true;

        for (ExprNode exprNode : node.varList()) {

            if(exprNode.operand() instanceof Imm) f = false;
        }

        if(checkInline(node)) f = true;

        if(checkInline(node) && entity.body().stmts().get(0) instanceof ReturnNode) {
            clear();
            entity.setInlineMode(false);
            ExprNode r = InlineFunction(node);
            visitExpr(r);
            node.setOperand(r.operand());
            entity.setInlineMode(true);
            return;
        }/**/
        else if ((f && entity.isInlined() && entity.inlineMode())  || (entity == currentFunction && entity.canbeSelfInline(inlineMode)) && entity.check()) { //||!entity.Rec && // && (!(entity.returnType() instanceof StringType)) Inlinable;

            entity.setInlineMode(false);
            if (entity == currentFunction) System.err.println(entity.name() + " is self expanded");
            inlineFunction(node);
            entity.setInlineMode(true);
            return;

        }/**/

        node.setOperand(currentFunction.newReg());
        Call call = new Call(entity, args, node.operand());
        currentFunction.addIns(call);
        clear();
    }


    boolean checkInline(FuncallNode node) {

        FunctionEntity entity = node.functionType().entity();
        return !entity.name().equals("main") && currentFunction.numOfVirtualReg() < 200  && entity.inlineMode() && entity.body() != null && entity.body().stmts() != null && entity.body().stmts().size() == 1;
    }

    private ExprNode InlineFunction(FuncallNode node) {

        FunctionEntity entity = node.functionType().entity();
        ExprNode returnNode = ((ReturnNode) entity.body().stmts().get(0)).expr();
        HashMap<Entity, Operand> inlineMap = new HashMap<>();
        for(int i = 0; i < entity.varList().size(); ++i) {
            Entity s = entity.varList().get(i);
            ExprNode t = node.varList().get(i);
            inlineMap.put(s, t.operand());
        }
        ExprNode tmp = returnNode.Inline(inlineMap);
        return tmp;

    }/**/

    @Override public void visit(CreatorNode node) {
        if (node.type() instanceof ArrayType) node.setOperand(newArray(node, 0));
        else node.setOperand(newClass((ClassType)node.type()));

    }

    private Operand newArray(CreatorNode node, int now) { // 0

        visitExpr(node.exprs().get(now));
        VirReg d = currentFunction.newReg();
        VirReg s = currentFunction.newReg();
        currentFunction.addIns(new Assign(s, node.exprs().get(now).operand()));
        currentFunction.addIns(new Binary(s, ADD, s, new Imm(1)));
        currentFunction.addIns(new Binary(s, MUL, s, new Imm(8)));
        List<Operand> args = new ArrayList<Operand>();
        args.add(s);
        currentFunction.addIns(new Call(malloc, args, d));
        currentFunction.addIns(new Assign(new Mem(d, null, 0, 0), node.exprs().get(now).operand()));
        currentFunction.addIns(new Binary(d, ADD, d, new Imm(8)));
        if (node.exprs().size() > now + 1) {
            currentFunction.addIns(new Assign(s, node.exprs().get(now).operand()));
            Label creator = new Label();
            currentFunction.addIns(creator);
            currentFunction.addIns(new Binary(s, SUB, s, new Imm(1)));
            currentFunction.addIns(new Assign(new Mem(d, s, 8, 0), newArray(node, now + 1)));
            currentFunction.addIns(new Cjump(s, new Imm(0), Cjump.Type.NE, creator));
        }
        else if (node.exprs().size() == now + 1 && node.type() instanceof ClassType) {
            currentFunction.addIns(new Assign(s, node.exprs().get(now).operand()));
            Label creator = new Label();
            currentFunction.addIns(creator);
            currentFunction.addIns(new Binary(s, SUB, s, new Imm(1)));
            currentFunction.addIns(new Assign(new Mem(d, s, 8, 0), newClass((ClassType)node.type())));
            currentFunction.addIns(new Cjump(s, new Imm(0), Cjump.Type.NE, creator));
        }

        return d;
    }

    private Operand newClass(ClassType type) {

        VirReg d = currentFunction.newReg();
        ArrayList<Operand> args = new ArrayList<Operand>();
        args.add(new Imm(type.entity().size()));
        currentFunction.addIns(new Call(malloc, args, d));
        if(type.entity().constructor() != null) {
            ArrayList<Operand> _args = new ArrayList<Operand>();
            _args.add(d);
            currentFunction.addIns(new Call(type.entity().constructor(), _args, new Mem(d, null, 0, 0)));
        }
        return d;
    }

    private VirReg toReg(Operand r) {
        if(r instanceof Reg) return (VirReg)r;
        else {
            VirReg t = currentFunction.newReg();
            currentFunction.addIns(new Assign(t, r));
            return t;
        }
    }

    @Override public void visit(ArefNode node) {
        visitExpr(node.expr());
        visitExpr(node.index());
        Reg index = toReg(node.index().operand());
        Reg base = toReg(node.expr().operand());
        node.setOperand(new Mem(base, index, 8, 0));
    }

    @Override public void visit(MemberNode node) {
        visitExpr(node.expr());
        Reg base = toReg(node.expr().operand());
        node.setOperand(new Mem(base, null, 0, ((VariableEntity)node.entity()).Offset()));
    }

    public void print() {

        for (FunctionEntity entity : ast.functionEntities()) {
            System.out.println("\n" + entity.name() + "\n");
            for(Ins item : entity.insList()) System.out.print(item.toString());//ln }

        }

        for (ClassEntity entity : ast.classEntities()) {
            for (FunctionDefinitionNode node : entity.memberFuncs()) {
                System.out.println("\n" + node.entity().name() + " in Class:" + entity.name() + "\n");
                for(Ins item : node.entity().insList()) System.out.print(item.toString());
            }
        }

    }

    @Override public void visit(VariableNode node) {

        if (node.isMember()) {
            Reg base = toReg(node.getThisPointer().pos());
            int offset = ((VariableEntity)node.entity()).Offset();
            node.setOperand(new Mem(base, null, 0, offset));
        }
        else {
            if (inlineMode > 0) {
                Entity entity = inlineMap.peek().get(node.entity());
                entity = entity == null ? node.entity() : entity;
                node.setOperand(entity.pos());
            }
            else node.setOperand((node.entity()).pos());
        }
    }

    public IR Ir() {

        return Ir;
    }

    private void clear() {

        history = null;
        history = new HashMap<>();
    }

    private VariableNode deepNode(ExprNode node) {

        if(node instanceof VariableNode) return (VariableNode) node;
        else if(node instanceof ArefNode) return deepNode(((ArefNode)node).expr());
        else throw new Error();
    }

    private int inlineCt = 0;
    private int inlineMode = 0;
    private Stack<Map <Entity, Entity> > inlineMap = new Stack<>();
    private Stack<Label> inlineReturnLabel = new Stack<>();
    private Stack<Operand> inlineReturnPos = new Stack<>();

    private void inlineFunction(FuncallNode node) {

        FunctionEntity entity = node.functionType().entity();

        Label returnLable = new Label();

        Map<Entity, Entity> map = new HashMap<>();

        node.setOperand(currentFunction.newReg());

        inlineMap.push(map);
        if(!entity.containIf) inlineReturnLabel.push(null);
        else inlineReturnLabel.push(returnLable);
        inlineReturnPos.push(node.operand());

        for(int i = 0; i < entity.varList().size(); ++i) {

            Entity par = entity.varList().get(i);
            ExprNode t = node.varList().get(i);
            VariableEntity clone = new VariableEntity(par.location(), par.type(), par.name() + "_inline_" + inlineCt++, null);
            map.put(par, clone);
            clone.setPos(t.operand());
        }

        ++inlineMode;
        nowInline = entity;
        visit(entity.body().copy());
        if(entity.containIf) currentFunction.addIns(returnLable);
        --inlineMode;

        inlineMap.pop();
        inlineReturnLabel.pop();
        inlineReturnPos.pop();
    }

}
