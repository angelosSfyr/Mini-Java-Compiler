import syntaxtree.*;
import visitor.*;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;



public class MyVisitor extends GJDepthFirst<String, Void>{
    /**
     * f0 -> "class"
     * f1 -> Identifier()
     * f2 -> "{"
     * f3 -> "public"
     * f4 -> "static"
     * f5 -> "void"
     * f6 -> "main"
     * f7 -> "("
     * f8 -> "String"
     * f9 -> "["
     * f10 -> "]"
     * f11 -> Identifier()
     * f12 -> ")"
     * f13 -> "{"
     * f14 -> ( VarDeclaration() )* go here
     * f15 -> ( Statement() )*      go here
     * f16 -> "}"
     * f17 -> "}"
     */
    @Override
    public String visit(MainClass n, Void argu) throws Exception {
        String classname = n.f1.accept(this, null);
        Main.ST.enterClass(classname);
        Main.ST.insertClass();
        Main.ST.enterMethod("main");
        Main.ST.insertMethod("void");
        Main.ST.insertMethodArgs("");

        super.visit(n, argu);
        Main.ST.exitMethod();
        return null;
    }

    /**
     * f0 -> "class"
     * f1 -> Identifier()
     * f2 -> "{"
     * f3 -> ( VarDeclaration() )*
     * f4 -> ( MethodDeclaration() )*
     * f5 -> "}"
     */
    @Override
    public String visit(ClassDeclaration n, Void argu) throws Exception {
        String classname = n.f1.accept(this, null);
        Main.ST.enterClass(classname);
        Main.ST.insertClass();
        super.visit(n, argu);
        return null;
    }

    /**
     * f0 -> "class"
     * f1 -> Identifier()
     * f2 -> "extends"
     * f3 -> Identifier()
     * f4 -> "{"
     * f5 -> ( VarDeclaration() )*
     * f6 -> ( MethodDeclaration() )*
     * f7 -> "}"
     */
    @Override
    public String visit(ClassExtendsDeclaration n, Void argu) throws Exception {
        String classname = n.f1.accept(this, null);
        String parentclassName = n.f3.accept(this, null);;
        Main.ST.enterClass(classname);
        Main.ST.insertExtendedClass(parentclassName);
        super.visit(n, argu);
        return null;
    }

    /**
     * f0 -> "public"
     * f1 -> Type()
     * f2 -> Identifier()
     * f3 -> "("
     * f4 -> ( FormalParameterList() )?
     * f5 -> ")"
     * f6 -> "{"
     * f7 -> ( VarDeclaration() )*
     * f8 -> ( Statement() )*
     * f9 -> "return"
     * f10 -> Expression()
     * f11 -> ";"
     * f12 -> "}"
     */
    @Override
    public String visit(MethodDeclaration n, Void argu) throws Exception {
        // if the parlis is present then arglist = parlist
        String myType = n.f1.accept(this, null);
        String myName = n.f2.accept(this, null);
        Main.ST.enterMethod(myName);
        Main.ST.insertMethod(myType);
        String argumentList = n.f4.present() ? n.f4.accept(this, null) : "";
        Main.ST.insertMethodArgs(argumentList);
        super.visit(n, argu);
        Main.ST.exitMethod();
        return null;
    }

    /**
    * f0 -> Type()
    * f1 -> Identifier()
    * f2 -> ";"
    */
    
    public String visit(VarDeclaration n, Void argu) throws Exception {
        // save to the right primitive symbol table <id, type>    
        String type = n.f0.accept(this, argu);
        String name = n.f1.accept(this, argu);
        n.f2.accept(this, argu);
        Main.ST.insertPrimitive(name, type);
        return null;
    }

    /**
     * f0 -> FormalParameter()
     * f1 -> FormalParameterTail()
     */
    @Override
    public String visit(FormalParameterList n, Void argu) throws Exception {
        String ret = n.f0.accept(this, null);

        if (n.f1 != null) {
            ret += n.f1.accept(this, null);
        }
        return ret;
    }

    /**
     * f0 -> FormalParameter()
     * f1 -> FormalParameterTail()
     */
    public String visit(FormalParameterTerm n, Void argu) throws Exception {
        return n.f1.accept(this, argu);
    }

    /**
     * f0 -> ","
     * f1 -> FormalParameter()
     */
    @Override
    public String visit(FormalParameterTail n, Void argu) throws Exception {
        String ret = "";
        for ( Node node: n.f0.nodes) {
            ret += "," + node.accept(this, null);
        }
        return ret;
    }

    /**
     * f0 -> Type()
     * f1 -> Identifier()
     */
    @Override
    public String visit(FormalParameter n, Void argu) throws Exception{
        String type = n.f0.accept(this, null);
        String name = n.f1.accept(this, null);
        Main.ST.insertPrimitive(name, type);
        return type;// + " " + name;
    }

    /**
    * f0 -> BooleanArrayType()
    *       | IntegerArrayType()
    */
    public String visit(ArrayType n, Void argu) throws Exception {
        String type = n.f0.accept(this, null);
        return type;
    }
    /**
        * f0 -> "boolean"
        * f1 -> "["
        * f2 -> "]"
    */
    public String visit(BooleanArrayType n, Void argu) throws Exception {
        return "boolean[]";
    }

    /**
    * f0 -> "int"
    * f1 -> "["
    * f2 -> "]"
    */
    public String visit(IntegerArrayType n, Void argu) throws Exception {
        return "int[]";
    }

    public String visit(BooleanType n, Void argu) {
        return "boolean";
    }

    public String visit(IntegerType n, Void argu) {
        return "int";
    }

    @Override
    public String visit(Identifier n, Void argu) {
        return n.f0.toString();
    }
}