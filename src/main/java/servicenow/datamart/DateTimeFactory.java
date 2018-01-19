package servicenow.datamart;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import servicenow.core.DateTime;
import servicenow.core.Log;

public class DateTimeFactory {

	final DateTime start;
	final Properties lastMetrics;
	
	final Pattern datePattern = Pattern.compile("\\d{4}-\\d{2}-\\d{2}( \\d{2}:\\d{2}:\\d{2})?");
	final Pattern namePattern = Pattern.compile("[a-z][a-z0-9_.]*", Pattern.CASE_INSENSITIVE);
	final Pattern exprPattern = Pattern.compile("([a-z][a-z0-9_.]*)?([+-])(\\d+)([a-z])", Pattern.CASE_INSENSITIVE);

	private Logger logger = LoggerFactory.getLogger(this.getClass());
	
	public DateTimeFactory(LoaderConfig parent) throws ConfigParseException {
		start = parent.getStart();
		File metricsFile = parent.getMetricsFile();
		assert start != null;
		if (metricsFile == null) {
			this.lastMetrics = null;
		}
		else {
			this.lastMetrics = new Properties();
			try {
				FileInputStream input = new FileInputStream(metricsFile);
				lastMetrics.load(input);
			}
			catch (IOException e) {
				throw new ConfigParseException(e);
			}
		}
	}
	
	public DateTimeFactory(DateTime start, Properties lastMetrics) {
		this.start = start;
		this.lastMetrics = lastMetrics;
	}

	DateTime getStart() {
		return this.start;
	}
	
	DateTime getLast(String propName) throws ConfigParseException {
		assert propName != null;
		if (lastMetrics == null) {
			logger.error(Log.INIT, "Metrics file not specified; " + propName + " will default to \"void\"");
			return null;
		}
		String propValue = lastMetrics.getProperty(propName);
		if (propValue == null) 
			throw new ConfigParseException("Property not found: " + propName);
		return new DateTime(propValue);
	}
	
	public DateTime getDate(Object obj) throws ConfigParseException {
		assert obj != null;
		if (obj instanceof java.util.Date) return new DateTime((java.util.Date) obj);
		String expr = obj.toString();
		if (datePattern.matcher(expr).matches())
			return new DateTime(expr);
		if (namePattern.matcher(expr).matches())
			return getName(expr);
		if (exprPattern.matcher(expr).matches())
			return getExpr(expr);
		throw new ConfigParseException("Invalid datetime: " + expr);
	}

	private DateTime getName(String name) throws ConfigParseException {
		assert name != null;
		if (name.equalsIgnoreCase("void")) return null;
		if (name.equalsIgnoreCase("start")) return getStart();
		if (name.equalsIgnoreCase("today")) return getStart().truncate();
		if (name.equalsIgnoreCase("last")) return getLast("start");
		if (name.toLowerCase().startsWith("last.")) {
			name = name.substring(5);
			return getLast(name);
		}
		return getLast(name);
	}
		
	private DateTime getExpr(String text) throws ConfigParseException {
		Matcher m = exprPattern.matcher(text);
		if (m.matches()) {
			String name = m.group(1);
			String op = m.group(2);
			int sign = (op == "+") ? +1 : -1;
			int num = sign * Integer.parseInt(m.group(3));
			String units = m.group(4);
			if (name == null) name = "start";
			DateTime base = getName(name);
			switch (units.toLowerCase()) {
			case "d": return base.addSeconds(num * DateTime.SEC_PER_DAY);
			case "h": return base.addSeconds(num * DateTime.SEC_PER_HOUR);
			case "m": return base.addSeconds(num * DateTime.SEC_PER_MINUTE);
			default:
				throw new ConfigParseException(String.format("Illegal units \"%s\" in \"%s\"", units, text));
			}						
		}
		else {
			throw new ConfigParseException(String.format("Invalid date expresion: %s", text));
		}
	}
	
}
