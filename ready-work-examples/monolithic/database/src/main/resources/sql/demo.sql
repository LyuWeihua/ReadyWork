#namespace("demo")
    #sql("getByName")
      select * from demo where name = ? limit 1
    #end
    #sql("getByName_1")
      select * from demo where name = #param(0) and age > #param(1) limit 1
    #end
    #sql("getByName_2")
      select * from demo where name = #param(name) and age > #param(age) limit 1
    #end
    #sql("getByNameLike")
      select * from demo where name like concat('%', #param(0), '%')
    #end
    #sql("getAllByPage")
      select * from demo
    #end
    #sql("getAllByDynamicParameter")
      select * from demo
        #for(x : condition)
            #(for.first ? "where": "and") #(x.key) #param(x.value)
        #end
    #end
#end

#namespace("forAuto")
    ### AUTO注解暂时不支持#param参数，只能使用"?"占位符
    #sql("getByName")
      select * from demo where name = ? limit 1
    #end
    #sql("updateRecord")
        update _TABLE_ set age = ?, height = ?, weight = ? where name in ( ? )
    #end
    #sql("deleteRecord")
        delete from _TABLE_ where name in ( ? )
    #end
#end
