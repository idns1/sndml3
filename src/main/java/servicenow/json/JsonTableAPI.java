package servicenow.json;

import servicenow.core.*;

import java.io.IOException;
import java.net.URI;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;
import org.slf4j.Logger;

public class JsonTableAPI extends TableAPI {

	final URI uri;

	final private Logger logger = Log.logger(this.getClass());
	
	public JsonTableAPI(Table table) {
		super(table);
		String path = table.getName() + ".do?JSONv2";
		this.uri = session.getURI(path);
		logger.debug(Log.INIT, this.uri.toString());
	}

	public KeySet getKeys() throws IOException {
		return getKeys(null);
	}
		
	public KeySet getKeys(EncodedQuery query) throws IOException {
		setAPIContext(uri);
		Log.setMethodContext("getKeys");
		JSONObject requestObj = new JSONObject();
		requestObj.put("sysparm_action",  "getKeys");
		if (!EncodedQuery.isEmpty(query))
			requestObj.put("sysparm_query", query.toString());
		JSONObject responseObj = getResponseJSON(uri, HttpMethod.POST, requestObj);
		assert responseObj.has("records");
		KeySet keys = new KeySet(responseObj, "records");
		return keys;
	}

	public Record getRecord(Key sys_id) throws IOException {
		setAPIContext(uri);
		Log.setMethodContext("get");
		Parameters params = new Parameters();
		params.add("sysparm_action", "get");
		params.add("sysparm_sys_id",  sys_id.toString());
		RecordList recs = getResponseRecords(params);
		assert recs != null;
		if (recs.size() == 0) return null;
		return recs.get(0);
	}

	public RecordList getRecords(KeySet keys, boolean displayValue) throws IOException {
		EncodedQuery query = keys.encodedQuery();
		return getRecords(query, displayValue);
	}
	
	public RecordList getRecords(EncodedQuery query, boolean displayValue) throws IOException {
		Parameters params = new Parameters();
		params.add("displayvalue", displayValue ? "all" : "false");
		if (!EncodedQuery.isEmpty(query))
			params.add("sysparm_query", query.toString());
		return getRecords(params);
	}

	public RecordList getRecords(Parameters params) throws IOException {
		setAPIContext(uri);
		params.add("sysparm_action", "getRecords");
		Log.setMethodContext("getRecords");
		return getResponseRecords(params);
	}

	private RecordList getResponseRecords(Parameters params) throws IOException {
		JSONObject requestObj = params.toJSON();
		JSONObject responseObj = super.getResponseJSON(uri, HttpMethod.POST, requestObj);
		assert responseObj.has("records");
		return new RecordList(table, responseObj, "records");
	}
	
	/*
	private JSONObject getResponseObject(JSONObject requestObj) throws IOException {
		String requestText = requestObj.toString();
		logger.debug(Log.REQUEST, requestText);
		HttpEntityEnclosingRequestBase request = new HttpPost(uri);
		HttpEntity requestEntity = new StringEntity(requestText, ContentType.APPLICATION_JSON);
		request.setEntity(requestEntity);
		request.setHeader("Content-Type", "application/json");
		return super.getResponseJSON(request, requestText);
		
		request.setHeader("Accept", "application/json");
		CloseableHttpResponse response = session.getClient().execute(request);		
		StatusLine statusLine = response.getStatusLine();		
		int statusCode = statusLine.getStatusCode();
		HttpEntity responseEntity = response.getEntity();
		Header contentTypeHeader = responseEntity.getContentType();
		String contentType = contentTypeHeader == null ? null : contentTypeHeader.getValue();
		String responseText = EntityUtils.toString(responseEntity);
		int responseLen = responseText == null ? 0 : responseText.length();
		logger.debug(Log.RESPONSE,
				String.format("status=\"%s\" contentType=%s len=%d", 
					statusLine, contentType, responseLen));
		if (statusCode == 401 || statusCode == 403) {
			logger.error(Log.RESPONSE, String.format("%s\nREQUEST:\n%s\n", statusLine, requestText));
			throw new InsufficientRightsException(uri, requestText);
		}
		if (contentType == null) {
			logger.error(Log.RESPONSE, String.format("%s\nREQUEST:\n%s\n", statusLine, requestText));
			throw new NoContentException(uri, requestText);
		}		
		if ("text/html".equals(contentType))
			throw new InstanceUnavailableException(this.uri, responseText);		
		logger.trace(Log.RESPONSE, responseText);
		JSONObject responseObj;
		try {
			responseObj = new JSONObject(responseText);
		}
		catch (org.json.JSONException e) {
			throw new JsonResponseError(responseText);
		}
		checkResponseJSON(uri, responseObj);
		response.close();
		return responseObj;
	}
	*/

	@Override
	public TableReader getDefaultReader() throws IOException {
		return new JsonKeyedReader(this.table);
	}

}