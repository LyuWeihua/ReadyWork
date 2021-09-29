#--
生成的源代码格式如下：

package com.xxx;
import work.ready.core.aop.Invocation;

public class Target$$EnhancerByReadyWork extends Target {
	public String test(String p0, int p1) {
		Invocation inv = new Invocation(this, 123L,
			args -> {
				return super.test(
							(String)args[0],
							(int)args[1]
						);
			},
			p0, p1);

		inv.invoke();

		return inv.getReturnValue();
	}
}
--#

package #(pkg);
import work.ready.core.aop.Invocation;
#for(x : classImports)
import #(x);
#end
public class #(name)#(classTypeVars) extends #(targetName)#(targetTypeVars) {
    #for(x : classProperties)
    #(x);
    #end
#for(x : constructorList)
    #(x.modifier) #(name)(#for(y : x.paramTypes)#(y) p#(for.index)#(for.last ? "" : ", ")#end) {
        super(#for(y : x.paramTypes)p#(for.index)#(for.last ? "" : ", ")#end);
    }
#end

#for(x : methodList)
	public #(x.methodTypeVars) #(x.returnType) #(x.name)(#for(y : x.paramTypes)#(y) p#(for.index)#(for.last ? "" : ", ")#end) #(x.throws){
	#if(x.hasInterceptor)
		#if(x.singleArrayParam)
		#@newInvocationForSingleArrayParam()
		#else
		#@newInvocationForCommon()
		#end

		inv.invoke();
		#if (x.returnType != "void")

		return inv.getReturnValue();
		#end
	#else
        #if (x.returnType != "void")
        #(x.returnTypeForVar) returnObject = null;
        #end
        #for(generator : x.codeGenerator)
            #(generator.getInsertCode())
	    #end

	    #if(x.hasReplace)
            #for(generator : x.codeGenerator)
                #(generator.getReplaceCode())
            #end
	    #else
            #if(x.singleArrayParam)
                #if (x.returnType != "void")returnObject = #end#(name).super.#(x.name)(
            						p0
            					);
            #else
                #if (x.returnType != "void")returnObject = #end#(name).super.#(x.name)(
                						#for(y : x.paramTypes)
                						(#(y.replace("...", "[]")))p#(for.index)#(for.last ? "" : ",")
                						#end
                					);
            #end
	    #end

        #for(generator : x.codeGenerator)
            #(generator.getAppendCode())
	    #end
        #if (x.returnType != "void")
        return returnObject;
        #end
	#end
	}
#end
}

#--
   一般参数情况
--#
#define newInvocationForCommon()
		Invocation inv = new Invocation(this, #(x.proxyMethodKey)L,
			args -> {
			    #if (x.returnType != "void" && x.codeGenerator)
                #(x.returnTypeForVar) returnObject = null;
                #end
                #for(generator : x.codeGenerator)
                #(generator.getInsertCode())
            	#end

                #if(x.hasReplace)
                    #for(generator : x.codeGenerator)
                        #(generator.getReplaceCode())
                    #end
                #else
				#if (x.returnType != "void")#if (x.codeGenerator)returnObject = #else return #end#end#(name).super.#(x.name)(
						#for(y : x.paramTypes)
						(#(y.replace("...", "[]")))args[#(for.index)]#(for.last ? "" : ",")
						#end
					);
				#end

				#for(generator : x.codeGenerator)
                #(generator.getAppendCode())
                #end
                #if (x.returnType != "void")
                #if(x.codeGenerator)
                return returnObject;
                #end
                #else
                return null;
                #end
			}
			#for(y : x.paramTypes), p#(for.index)#end);
#end
#--
   只有一个参数，且该参数是数组或者可变参数
--#
#define newInvocationForSingleArrayParam()
		Invocation inv = new Invocation(this, #(x.proxyMethodKey)L,
			args -> {
			    #if (x.returnType != "void" && x.codeGenerator)
                #(x.returnTypeForVar) returnObject = null;
                #end
                #for(generator : x.codeGenerator)
                #(generator.getInsertCode())
            	#end

                #if(x.hasReplace)
                    #for(generator : x.codeGenerator)
                        #(generator.getReplaceCode())
                    #end
                #else

				#if (x.returnType != "void")#if (x.codeGenerator)returnObject = #else return #end#end#(name).super.#(x.name)(
						p0
					);
				#end

				#for(generator : x.codeGenerator)
                #(generator.getAppendCode())
                #end
                #if (x.returnType != "void")
                #if (x.codeGenerator)
                return returnObject;
                #end
                #else
                return null;
                #end
			}
			, p0);
#end
