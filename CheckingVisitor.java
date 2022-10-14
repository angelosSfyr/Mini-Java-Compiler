import syntaxtree.*;
import visitor.*;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

public class CheckingVisitor extends GJDepthFirst<String, Void>{

    boolean inClass=false;
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
     * f14 -> ( VarDeclaration() )* 
     * f15 -> ( Statement() )*      go here
     * f16 -> "}"
     * f17 -> "}"
     */


    @Override
    public String visit(MainClass n, Void argu) throws Exception {
        this.inClass = true;
        String classname = n.f1.accept(this, null);
        this.inClass = false;
        Main.ST.enterClass(classname);      
        Main.ST.enterMethod("main");
        n.f14.accept(this, null);
        n.f15.accept(this, null);
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
        this.inClass = true;
        String classname = n.f1.accept(this, null);
        this.inClass = false;
        Main.ST.enterClass(classname);
        n.f4.accept(this, null);
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
        this.inClass = true;
        String classname = n.f1.accept(this, null);
        String parentclassName = n.f3.accept(this, null);;
        this.inClass = false;
        Main.ST.enterClass(classname);
        n.f6.accept(this, null);
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
        n.f4.accept(this, null);
        this.inClass = true;
        String myType = n.f1.accept(this, null);
        String myName = n.f2.accept(this, null);
        this.inClass = false;
        Main.ST.enterMethod(myName);
        String exprType = n.f10.accept(this, null);
        n.f8.accept(this, null);
        if (!myType.equals(exprType)){
            throw new Exception("MethodDeclaration: False return type"+"("+Main.ST.getCurrentMethodName()+")");
        }   // return statement bust be the same as the type
        Main.ST.exitMethod();
        return null;
    }

    /**
    * f0 -> Type()
    * f1 -> Identifier()
    * f2 -> ";"
    */
    
    public String visit(VarDeclaration n, Void argu) throws Exception {  
        this.inClass = true;
        String type = n.f0.accept(this, argu);
        this.inClass = false;
        if (isObject(type)){
            if (!Main.ST.existsClass(type)){
                throw new Exception("VarDeclaration: Class does not exist"+"("+Main.ST.getCurrentMethodName()+")");
            }
        }
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
        this.inClass = true;
        String type = n.f0.accept(this, null);
        this.inClass = false;
        if (isObject(type)){
            if (!Main.ST.existsClass(type)){
                throw new Exception("VarDeclaration: Class does not exist"+"("+Main.ST.getCurrentMethodName()+")");
            }
        }
        return type;
    }

    /**
    * f0 -> BooleanArrayType()
    *       | IntegerArrayType()
    */
    public String visit(ArrayType n, Void argu) throws Exception {
        String type = n.f0.accept(this, null);
        return type;
    }

    public String visit(IntegerArrayType n, Void argu) throws Exception {
        return "int[]";
    }

    public String visit(BooleanArrayType n, Void argu) throws Exception {
        return "boolean[]";
    }

    @Override
    public String visit(BooleanType n, Void argu) {
        return "boolean";
    }
    @Override
    public String visit(IntegerType n, Void argu) {
        return "int";
    }

    /**
    * f0 -> Identifier()
    * f1 -> "="
    * f2 -> Expression()
    * f3 -> ";"
    */
    
    public boolean isObject(String idType){
        return idType != "int" && idType != "int[]" && idType != "boolean" && idType != "boolean[]";
    }


    @Override
    public String visit(AssignmentStatement n, Void argu) throws Exception {

        String idType =  n.f0.accept(this, argu);
        String exprType = n.f2.accept(this, argu);
        if(isObject(idType)){
            if (isObject(exprType)){
                if (idType.equals(exprType) || Main.ST.childHasParent(exprType, idType)){
                    return idType;
                }else{
                    throw new Exception("AssignmentStatement: not same type object or not a parent of child "+exprType+"("+Main.ST.getCurrentMethodName()+")");
                }
            }else{
                throw new Exception("AssignmentStatement: id is object but expr is not"+"("+Main.ST.getCurrentMethodName()+")");
            }
        }else{
            if (idType != exprType){
                throw new Exception("AssignmentStatement: id and expr not the same type"+"("+Main.ST.getCurrentMethodName()+")");
            }
        }

        n.f1.accept(this, argu);
        n.f3.accept(this, argu);
        return idType;
    }

    /**
    * f0 -> Identifier()
    * f1 -> "["
    * f2 -> Expression()
    * f3 -> "]"
    * f4 -> "="
    * f5 -> Expression()
    * f6 -> ";"
    */
    private String arrayType(String type){
        if (type =="boolean[]"){
            return "boolean";
        }else if (type == "int[]"){
            return "int";
        }
        return null;
    }
    /* id must be an int[] and exp must be a int(same for boolean) */
    @Override
    public String visit(ArrayAssignmentStatement n, Void argu) throws Exception {
        // f2 expression must be of type int 
        String _ret=null;
        String idType = n.f0.accept(this, argu);
        n.f1.accept(this, argu);
        if (n.f2.accept(this, argu) != "int"){
            throw new Exception("ArrayAssignmentStatement: expr index is not an integer"+"("+Main.ST.getCurrentMethodName()+")");
        }
        String exprType = n.f5.accept(this, argu);    // its type must be the same as the typeOfArray
        String typeOfArray = arrayType(idType);
        if (typeOfArray == null){
            throw new Exception("ArrayAssignmentStatement: ID IS NOT AN ARRAY"+"("+Main.ST.getCurrentMethodName()+")");
        }else{
            if (exprType != typeOfArray){
                throw new Exception("ArrayAssignmentStatement: id and expr not the same type"+"("+Main.ST.getCurrentMethodName()+")");
            }
            return typeOfArray;
        }
    }

     /**
    * f0 -> "if"
    * f1 -> "("
    * f2 -> Expression()
    * f3 -> ")"
    * f4 -> Statement()
    * f5 -> "else"
    * f6 -> Statement()
    */
    @Override
    public String visit(IfStatement n, Void argu) throws Exception {
        String _ret=null;
        n.f0.accept(this, argu);
        n.f1.accept(this, argu);
        if (n.f2.accept(this, argu)!="boolean"){
            throw new Exception("Not a boolean in if expression"+"("+Main.ST.getCurrentMethodName()+")");
        }
        n.f3.accept(this, argu);
        n.f4.accept(this, argu);
        n.f5.accept(this, argu);
        n.f6.accept(this, argu);
        return _ret;
    }

    /**
    * f0 -> "while"
    * f1 -> "("
    * f2 -> Expression()
    * f3 -> ")"
    * f4 -> Statement()
    */
    @Override
    public String visit(WhileStatement n, Void argu) throws Exception {
        String _ret=null;
        n.f0.accept(this, argu);
        n.f1.accept(this, argu);
        if (n.f2.accept(this, argu)!="boolean"){
            throw new Exception("Not a boolean in while expression"+"("+Main.ST.getCurrentMethodName()+")");
        }
        n.f3.accept(this, argu);
        n.f4.accept(this, argu);
        return _ret;
    }
    /**
    * f0 -> PrimaryExpression()
    * f1 -> "&&"
    * f2 -> PrimaryExpression()
    */
    @Override
    public String visit(AndExpression n, Void argu) throws Exception {
        String _ret=null;
        if (n.f0.accept(this, argu) !="boolean"){
            throw new Exception("Not a boolean in end expression"+"("+Main.ST.getCurrentMethodName()+")");
        }
        n.f1.accept(this, argu);
        if (n.f2.accept(this, argu) !="boolean"){
            throw new Exception("Not a boolean in end expression"+"("+Main.ST.getCurrentMethodName()+")");
        }
        return "boolean";
    }

    /**
    * f0 -> PrimaryExpression()
    * f1 -> "<"
    * f2 -> PrimaryExpression()
    */
    @Override
    public String visit(CompareExpression n, Void argu) throws Exception {
        String _ret=null;
        if (n.f0.accept(this, argu) != "int"){
            throw new Exception("Not an integer in LESS expression"+"("+Main.ST.getCurrentMethodName()+")");
        }
        n.f1.accept(this, argu);
        if (n.f2.accept(this, argu) != "int"){
            throw new Exception("Not an integer in LESS expression"+"("+Main.ST.getCurrentMethodName()+")");
        }
        return "boolean";
    }

    /**
    * f0 -> PrimaryExpression()
    * f1 -> "+"
    * f2 -> PrimaryExpression()
    */
    @Override
    public String visit(PlusExpression n, Void argu) throws Exception {
        String _ret=null;
        String f0Str = n.f0.accept(this, argu);
        String f2Str = n.f2.accept(this, argu);
        if (f0Str != "int"){
            throw new Exception("Not an integer in PLUS expression"+"("+Main.ST.getCurrentMethodName()+")");
        }
        n.f1.accept(this, argu);
        if (f2Str != "int"){
            throw new Exception("Not an integer in PLUS expression"+"("+Main.ST.getCurrentMethodName()+")");
        }
        return "int";
    }
    /**
    * f0 -> PrimaryExpression()
    * f1 -> "-"
    * f2 -> PrimaryExpression()
    */
    @Override
    public String visit(MinusExpression n, Void argu) throws Exception {
        String _ret=null;
        if (n.f0.accept(this, argu) != "int"){
            throw new Exception("Not an integer in MINUS expression"+"("+Main.ST.getCurrentMethodName()+")");
        }
        n.f1.accept(this, argu);
        if (n.f2.accept(this, argu) != "int"){
            throw new Exception("Not an integer in MINUS expression"+"("+Main.ST.getCurrentMethodName()+")");
        }
        return "int";
    }

    /**
    * f0 -> PrimaryExpression()
    * f1 -> "*"
    * f2 -> PrimaryExpression()
    */
    @Override
    public String visit(TimesExpression n, Void argu) throws Exception {
        String _ret=null;
        if (n.f0.accept(this, argu) != "int"){
            throw new Exception("Not an integer in TIMES expression"+"("+Main.ST.getCurrentMethodName()+")");
        }
        n.f1.accept(this, argu);
        if (n.f2.accept(this, argu) != "int"){
            throw new Exception("Not an integer in TIMES expression"+"("+Main.ST.getCurrentMethodName()+")");
        }
        return "int";
    }

    /**
    * f0 -> PrimaryExpression()
    * f1 -> "["
    * f2 -> PrimaryExpression()
    * f3 -> "]"
    */
    @Override
    public String visit(ArrayLookup n, Void argu) throws Exception {
        String _ret=null;
        String arrayType = n.f0.accept(this, argu);
        if (arrayType != "boolean[]" && arrayType != "int[]"){
            throw new Exception("Not an array identigier in ArrayLookup expression"+"("+Main.ST.getCurrentMethodName()+")");
        }
        n.f1.accept(this, argu);
        if (n.f2.accept(this, argu) != "int"){
            throw new Exception("Not an integer in ArrayLookup expression"+"("+Main.ST.getCurrentMethodName()+")");
        }
        if (arrayType=="int[]") return "int";
        return "boolean";
    }

    /**
    * f0 -> PrimaryExpression()
    * f1 -> "."
    * f2 -> "length"
    */
    @Override
    public String visit(ArrayLength n, Void argu) throws Exception {
        String _ret=null;
        String arrayType = n.f0.accept(this, argu);
        if (arrayType != "int[]" && arrayType != "boolean[]"){
            throw new Exception("Not an array id  in ArrayLength expression"+"("+Main.ST.getCurrentMethodName()+")");
        }
        n.f1.accept(this, argu);
        n.f2.accept(this, argu);
        return "int";
    }


    // args1 -> method arguments
    private boolean compareArgs(String args1, String args2)
    {
        String[] array1 = args1.split(",");
        String[] array2 = args2.split(",");
        if (array1.length != array2.length){
            return false;
        }
        for (int i=0; i<array1.length; i++){
            if (!array1[i].equals(array2[i]) && !Main.ST.childHasParent(array2[i], array1[i])) return false;
        }
        return true;
    }
    /**
    * f0 -> PrimaryExpression()
    * f1 -> "."
    * f2 -> Identifier()
    * f3 -> "(" 
    * f4 -> ( ExpressionList() )?
    * f5 -> ")"
    */
    @Override
    public String visit(MessageSend n, Void argu) throws Exception {
        // does super exist in this multiverse?
        String _ret=null;
        String idType = n.f0.accept(this, argu);
        if (!this.isObject(idType)){
            throw new Exception("MessageSendError: identifier is not an object"+"("+Main.ST.getCurrentMethodName()+")");
        }
        this.inClass = true;
        String methodName = n.f2.accept(this, null);
        this.inClass = false;
        if (!Main.ST.classHadMethod(idType, methodName))
        {
            throw new Exception("MessageSendError: class has not method"+"("+Main.ST.getCurrentMethodName()+")");
        }
        String expList = n.f4.accept(this, argu);
        if (expList==null) expList="";
        String methodArgs = Main.ST.getClassMethodArgs(idType, methodName);
        if (!compareArgs(methodArgs, expList)){
            
            throw new Exception("MessageSendError: method arguments are wrong"+"("+Main.ST.getCurrentMethodName()+")");
        }
        return Main.ST.getClassMethodType(idType, methodName);
    }

    /**
    * f0 -> Expression()
    * f1 -> ExpressionTail()
    */
    @Override
    public String visit(ExpressionList n, Void argu) throws Exception {
        return n.f0.accept(this, argu)+n.f1.accept(this, argu);
    }

    /**
        * f0 -> ( ExpressionTerm() )*
    */
    @Override
    public String visit(ExpressionTail n, Void argu) throws Exception {
        String ret = "";
        for ( Node node: n.f0.nodes) {
            ret += "," + node.accept(this, null);
        }
        return ret;
    }

    /**
    * f0 -> ","
    * f1 -> Expression()
    */
    @Override
    public String visit(ExpressionTerm n, Void argu) throws Exception {
        return n.f1.accept(this, argu);
    }
  
    /**
    * f0 -> IntegerLiteral()
    *       | TrueLiteral() .
    *       | FalseLiteral() .
    *       | Identifier() .
    *       | ThisExpression() .
    *       | ArrayAllocationExpression() .
    *       | AllocationExpression()
    *       | NotExpression() .
    *       | BracketExpression() .
    */
    public String visit(PrimaryExpression n, Void argu) throws Exception {
        return n.f0.accept(this, argu);
    }
    /**
    * f0 -> <INTEGER_LITERAL>
    */
    @Override
    public String visit(IntegerLiteral n, Void argu) throws Exception {
        n.f0.accept(this, argu);
        return "int";
    }

    /**
        * f0 -> "true"
        */
    @Override
    public String visit(TrueLiteral n, Void argu) throws Exception {
        n.f0.accept(this, argu);
        return "boolean";
    }

    /**
        * f0 -> "false"
        */
    @Override
    public String visit(FalseLiteral n, Void argu) throws Exception {
        n.f0.accept(this, argu);
        return "boolean";
    }

    /**
        * f0 -> "this"
        */
    @Override
    public String visit(ThisExpression n, Void argu) throws Exception {
        n.f0.accept(this, argu);
        return  Main.ST.getCurrentClassName();
    }



    /**
    * f0 -> "new"
    * f1 -> "int"
    * f2 -> "["
    * f3 -> Expression()
    * f4 -> "]"
    */
    @Override
   public String visit(IntegerArrayAllocationExpression n, Void argu) throws Exception {
      String _ret=null;
      String f3type = n.f3.accept(this, argu);
      if (f3type != "int"){
        throw new Exception("Array size is not na integer"+"("+Main.ST.getCurrentMethodName()+")");
      }
      return "int[]";
   }

    /**
    * f0 -> "new"
    * f1 -> "boolean"
    * f2 -> "["
    * f3 -> Expression()
    * f4 -> "]"
    */
    public String visit(BooleanArrayAllocationExpression n, Void argu) throws Exception {
        String _ret=null;
        String f3type = n.f3.accept(this, argu);
        if (f3type != "int"){
            throw new Exception("Array size is not na integer"+"("+Main.ST.getCurrentMethodName()+")");
        }
        return "boolean[]";
    }

   /**
    * f0 -> "new"
    * f1 -> Identifier()
    * f2 -> "("
    * f3 -> ")"
    */
    //the assignment "A a = new B();" when B extends A is correct
    //the same applies when a method expects a parameter of type A and a B instance is given instead.
    @Override
    public String visit(AllocationExpression n, Void argu) throws Exception {
        String _ret=null;
        n.f0.accept(this, argu);
        this.inClass = true;
        String className = n.f1.accept(this, argu);
        this.inClass = false;
        if (!Main.ST.existsClass(className)){
            throw new Exception("Class+ "+className+" does not exist"+"("+Main.ST.getCurrentMethodName()+")");
        }
        return className;
    }
    /**
        * f0 -> <IDENTIFIER>
    */
    @Override
    public String visit(Identifier n, Void argu) throws Exception {
        String idName =  n.f0.toString();
        if (this.inClass == false){
            return  Main.ST.lookup(idName);
        } 
        return idName;
    }
    
    /**
        * f0 -> "!"
        * f1 -> PrimaryExpression()
        */
    @Override
    public String visit(NotExpression n, Void argu) throws Exception {
        String _ret=null;
        n.f0.accept(this, argu);
        if (n.f1.accept(this, argu) != "boolean"){
            throw new Exception("NotExpression: primary expression is not a boolean"+"("+Main.ST.getCurrentMethodName()+")");
        }
        return "boolean";
    }

     /**
    * f0 -> "("
    * f1 -> Expression()
    * f2 -> ")"
    */
    @Override
    public String visit(BracketExpression n, Void argu) throws Exception {
        n.f0.accept(this, null);
        String type = n.f1.accept(this, null);
        n.f2.accept(this, null);
        return type;
    }
}