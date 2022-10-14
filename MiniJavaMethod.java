import java.util.*; 

class MiniJavaMethod  {

    private Hashtable<String, String> variables;
    private String myClassName;
    private String myName;
    private String returnType;
    private String arguments;

    public MiniJavaMethod(String myClass, String methodName, String returnType){
        this.variables = new Hashtable<String, String>();
        this.myClassName = myClass;
        this.myName = methodName;
        this.returnType = returnType;
    }

    public void insertArgs(String args){
        this.arguments = args;
    }
    // fix this
    public void insertVariable(String idName, String idType){
        this.variables.put(idName, idType);
    }

    public String toString() {
       // return (type)(name)(arguments)(variables)
       return "("+this.returnType+"){"+this.myName+"}("+this.arguments+")"+this.variables;
    }

    public String getReturnType(){
        return this.returnType;
    }

    public String getArguments(){
        return this.arguments;
    }

    public String getMethodName(){
        return this.myName;
    }

    public String lookupVariable(String idName){
        return this.variables.get(idName);
    }

    public String getOwner(){
        return this.myClassName;
    }
}