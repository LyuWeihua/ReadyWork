package #(modelPackageName);

import work.ready.core.database.annotation.Table;
import #(baseModelPackageName).#(tableMeta.baseModelName);

/**
 * Generated by Ready.Work
 */
@Table(tableName = "#(tableMeta.name)", primaryKey = "#(tableMeta.primaryKey)")
public class #(tableMeta.modelName) extends #(tableMeta.baseModelName)<#(tableMeta.modelName)> {
	#if(generateDaoInModel)
	public static final #(tableMeta.modelName) dao = new #(tableMeta.modelName)().dao();
	#else

	#end
}
