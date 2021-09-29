package work.ready.core.database.sql;

import work.ready.core.template.source.TemplateSource;

class SqlTemplateSource {

	String file;
	TemplateSource source;

	SqlTemplateSource(String file) {
		this.file = file;
		this.source = null;
	}

	SqlTemplateSource(TemplateSource source) {
		this.file = null;
		this.source = source;
	}

	boolean isFile() {
		return file != null;
	}
}

