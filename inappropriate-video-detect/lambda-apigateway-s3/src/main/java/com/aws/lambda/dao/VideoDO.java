package com.aws.lambda.dao;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBAttribute;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapperConfig;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBQueryExpression;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBScanExpression;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable;
import com.amazonaws.services.dynamodbv2.datamodeling.PaginatedQueryList;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;

@DynamoDBTable(tableName = "PLACEHOLDER_VIDEOS_TABLE_NAME")
public class VideoDO {

	// get the table name from env. var. set in serverless.yml
	private static final String VIDEOS_TABLE_NAME = System.getenv("VIDEOS_TABLE_NAME");

	private static DynamoDBAdapter db_adapter;
	private final AmazonDynamoDB client;
	private final DynamoDBMapper mapper;

	private String id;
	private String jobID;
	private String result;
	private String detail;

	public VideoDO() {
		// build the mapper config
		DynamoDBMapperConfig mapperConfig = DynamoDBMapperConfig.builder()
				.withTableNameOverride(new DynamoDBMapperConfig.TableNameOverride(VIDEOS_TABLE_NAME)).build();
		// get the db adapter
		this.db_adapter = DynamoDBAdapter.getInstance();
		this.client = this.db_adapter.getDbClient();
		// create the mapper with config
		this.mapper = this.db_adapter.createDbMapper(mapperConfig);
	}

	@DynamoDBHashKey(attributeName = "id")
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	@DynamoDBAttribute(attributeName = "jobid")
	public String getJobID() {
		return jobID;
	}

	public void setJobID(String jobID) {
		this.jobID = jobID;
	}

	@DynamoDBAttribute(attributeName = "result")
	public String getResult() {
		return result;
	}

	public void setResult(String result) {
		this.result = result;
	}

	@DynamoDBAttribute(attributeName = "detail")
	public String getDetail() {
		return detail;
	}

	public void setDetail(String detail) {
		this.detail = detail;
	}

	public String toString() {
		return String.format("Video [id=%s, jobid=%s, result=%s, detail=%s]", this.id, this.jobID, this.result, this.detail);
	}

	// methods
	public Boolean ifTableExists() {
		return this.client.describeTable(VIDEOS_TABLE_NAME).getTable().getTableStatus().equals("ACTIVE");
	}

	public List<VideoDO> list() throws IOException {
		DynamoDBScanExpression scanExp = new DynamoDBScanExpression();
		List<VideoDO> results = this.mapper.scan(VideoDO.class, scanExp);
		return results;
	}

	public VideoDO get(String id) throws IOException {
		VideoDO item = null;

		HashMap<String, AttributeValue> av = new HashMap<String, AttributeValue>();
		av.put(":v1", new AttributeValue().withS(id));

		DynamoDBQueryExpression<VideoDO> queryExp = new DynamoDBQueryExpression<VideoDO>()
				.withKeyConditionExpression("id = :v1").withExpressionAttributeValues(av);

		PaginatedQueryList<VideoDO> result = this.mapper.query(VideoDO.class, queryExp);
		if (result.size() > 0) {
			item = result.get(0);
		}
		return item;
	}

	public void save(VideoDO item) throws IOException {
		this.mapper.save(item);
	}

}
