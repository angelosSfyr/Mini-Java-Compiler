import java.util.*; 
//But when we have "class B extends A”, A must be defined before B.

class MiniJavaClass {
    
    protected ArrayList<String> methodNames;
    protected int size;
    protected String myParentName;
    protected String className;
    protected Hashtable <String, String> fields;
    protected Hashtable <String, MiniJavaMethod> methods;
    
    public MiniJavaClass(String name){
        this.methodNames  =  new ArrayList<String>();
        this.size = 8;  // for the methods vtable
        this.fields = new Hashtable <String, String>();
        this.methods = new Hashtable<String, MiniJavaMethod>();
        this.className = name;
        this.myParentName="";
    }

    public ArrayList<String> getMethodNames(){
        return this.methodNames;
    }

    public int getSize(){
        return this.size;
    }

    public boolean hasMethod(String methodName)
    {
        return this.methods.containsKey(methodName);
    }


    public String getParentClassName(){
        return this.myParentName;
    }
    
    public void insertField(String idName, String idType){
        this.size += LLVMVisitor.getTypeBits(idType);
        this.fields.put(idName, idType);
    }

    public void insertArgs(String methodName, String args){
        this.methods.get(methodName).insertArgs(args);
    }

    public String toString() {
        String strMethods = "";
        Iterator<Map.Entry<String, MiniJavaMethod>> itr = this.methods.entrySet().iterator();
        Map.Entry<String, MiniJavaMethod> entry = null;
        while(itr.hasNext()){
            entry = itr.next();
            strMethods += "\t"+entry.getValue()+"\n";
        }
        return this.className + " " + fields + "\n" + strMethods; // add fields and methods
    }

    public void insertClassMethod(MiniJavaMethod method){
        String methodName = method.getMethodName();
        if (!this.methods.containsKey(methodName)){
            this.methodNames.add(methodName);
        }   
        this.methods.put(methodName, method);
    }

    public void insertMethodVariable(String methodName, String idName, String idType){
        this.methods.get(methodName).insertVariable(idName, idType);
    }

    public String lookupVariable(String idName, String currentMethodName){
        String type = null;
        if (currentMethodName !=""){   // if we are inside a function look for the identifier in the method variables
            MiniJavaMethod method = this.methods.get(currentMethodName);
            type = method.lookupVariable(idName);
        }
        if (type != null){  // if identifier is a method variable return it
            return type;
        }                   // else look ofr identifier in current class fields
        type = this.fields.get(idName);
        if (type == null){
            //throw new Exception("Identifier not found");
        }
        return type;
    }

    public boolean idetifierIsClassField(String idName, String currentMethodName){
        MiniJavaMethod method = this.methods.get(currentMethodName);
        String type = method.lookupVariable(idName);
        if (type == null) return true;
        return false;
    }

    public String getMethodArgs(String methodName)
    {
        MiniJavaMethod method = this.methods.get(methodName);
        return method.getArguments();
    }

    public String getMethodReturnType(String methodName)
    {
        MiniJavaMethod method = this.methods.get(methodName);
        return method.getReturnType();
    }

    public String getMethodOwner(String methodName){
        MiniJavaMethod method = this.methods.get(methodName);
        return method.getOwner();
    }

    public int getNumberOfMethods(){
        return this.methods.size();
    }
}
