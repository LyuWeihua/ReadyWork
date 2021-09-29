#namespace("test")
    #sql("getAllByDynamicParameter")
      select * from demo
        #for(x : condition)
            #(for.first ? "where": "and") #(x.key) #param(x.value)
        #end
    #end
#end

#namespace("forAuto")
    ### AUTO注解暂时不支持#param参数，只能使用"?"占位符
    #sql("getByAgeAndNameLike")
      select * from demo where age > ? and name like concat('%', ?, '%')
    #end
    #sql("insertRecord")
        insert into _TABLE_(name, gender, age) value(?,?,?)
    #end
#end
