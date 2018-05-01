package FrontEnd;

import Entity.Entity;
import Entity.ClassEntity;
import Entity.FunctionEntity;
import Entity.VariableEntity;
import AST.*;
import Error.SemanticError;
import AST.DefinitionNode;
import Scope.Scope;
import Type.NullType;

import java.util.List;

public class AST {

    private Scope scope;

    private List<DefinitionNode> definitionNodes;
    private List<ClassEntity> classEntities;
    private List<FunctionEntity> functionEntities;
    private List<VariableEntity> variableEntities;

    public AST(List<DefinitionNode> definitionNodes, List<ClassEntity> Class, List<FunctionEntity> Function, List<VariableEntity> Variable) {

        super();
        this.definitionNodes  = definitionNodes;
        this.classEntities    = Class;
        this.functionEntities = Function;
        this.variableEntities = Variable;
        this.scope = new Scope(true);
    }

    public void BulidScope() {

        scope.insert(new Entity(new Location(0,0), new NullType(), "null"));
        LoadLibrary LibF = new LoadLibrary();
        for(FunctionEntity entity : LoadLibrary.LibFunc()) scope.insert(entity);
        for (ClassEntity entity : classEntities) scope.insert(entity);
        for (FunctionEntity entity : functionEntities) scope.insert(entity);
        ScopeBulider resolver = new ScopeBulider(scope);
        for (DefinitionNode d : definitionNodes) resolver.visitDefinition(d);
    }

    public void checkSemantic() {

        SemanticChecker semanticChecker = new SemanticChecker(scope);
        for (DefinitionNode d : definitionNodes) semanticChecker.visitDefinition(d);
        FunctionEntity MainFunction = (FunctionEntity)scope.search("main");
        if (MainFunction == null || !MainFunction.returnType().isInt()) throw new SemanticError(new Location(0,0), "main Error");
    }

}
