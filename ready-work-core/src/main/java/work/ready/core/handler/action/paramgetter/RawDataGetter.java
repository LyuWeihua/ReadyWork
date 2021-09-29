package work.ready.core.handler.action.paramgetter;

import work.ready.core.handler.Controller;
import work.ready.core.handler.action.Action;

public class RawDataGetter extends ParamGetter<RawData> {

	public RawDataGetter(String parameterName, String defaultValue) {
		super(parameterName, defaultValue);
	}

	@Override
	public RawData get(Action action, Controller c) {
		return new RawData(c.getRawData());
	}

	@Override
	protected RawData to(String v) {
		return new RawData(v);
	}
}
